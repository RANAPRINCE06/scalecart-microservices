# Order Creation Flow Redesign

## 1. Current Flow Risks

The current `OrderServiceImpl#createOrder` implementation directly invokes `ProductPurchaseService.purchaseProducts(...)`, immediately saves an order in `PENDING` status, and publishes an event. This tight coupling and lack of orchestration leads to several real-world risks:

1. **Overselling inventory** – inventory is decremented immediately without reservation. Concurrent requests can oversell because there is no optimistic/pessimistic guard and no reservation ledger.
2. **Non-idempotent requests** – repeated POSTs (client retries/network issues) create duplicate orders. No idempotency key or deduplication exists.
3. **Price drift** – the order accepts client-provided prices; price and promotions are not revalidated against catalog/pricing services, leading to mismatch and potential fraud.
4. **Payment race conditions** – there is no payment authorization step, so stock can be reduced even if the customer cannot pay. Refunds or manual reconciliation is required.
5. **Missing compensation** – if a downstream call fails after the stock update (e.g., notification or payment), there is no rollback or compensation to release inventory.
6. **Lack of order state machine** – orders are persisted as `PENDING` even when inventory is not actually secured or payment is confirmed, creating incorrect reporting.
7. **Inadequate observability** – the flow does not propagate correlation IDs, lacks metrics for reservation failures, and uses generic error handling.
8. **Unbounded transaction** – the method is wrapped in a single transaction; remote service calls inside an ACID transaction introduce long-lived transactions and are prone to timeouts or partial commits.

## 2. Real-World Ecommerce Order Creation Flow

1. **Receive request** with items, quantities, shipping/billing info, and an **Idempotency-Key** header.
2. **Validate user & cart** (user eligibility, address, promotions, existing draft order).
3. **Recalculate pricing** using the Pricing/Catalog service. Reject if prices changed since cart was shown (HTTP 409 Conflict).
4. **Create order draft** with status `PENDING_RESERVATION`; persist with idempotency token.
5. **Reserve inventory** via Inventory service ("try" phase). Reservations decrement available-to-promise (ATP) and create reservation rows with expiry.
6. **Authorize payment** via Payment service (hold funds). Use order total from recalculated pricing. If payment fails return 402 Payment Required.
7. **Confirm inventory** once payment authorization succeeds; Inventory service transitions reservations to `COMMITTED`.
8. **Mark order as `CONFIRMED`**; persist payment auth reference, shipping SLA, etc.
9. **Publish domain events** (`OrderConfirmed`, `InventoryReserved`, `PaymentAuthorized`) for async consumers (fulfillment, notification).
10. **Handle failures**: if inventory reservation fails, release draft order; if payment fails after reservation, release inventory via compensation.

## 3. Service Contracts & Data Model Changes

### Order Service

**Table: `orders`**
- `id` (UUID PK)
- `order_number`
- `user_id`
- `status` (`PENDING_RESERVATION`, `RESERVED`, `PAYMENT_AUTHORIZED`, `CONFIRMED`, `CANCELLED`, `FAILED`)
- `total_amount`
- `currency`
- `idempotency_key` (unique per user)
- `payment_auth_id`
- Timestamps

**Table: `order_items`**
- `id` (UUID PK)
- `order_id` FK
- `product_id`
- `sku`
- `quantity`
- `unit_price`
- `total_price`

**Table: `order_reservations`** (optional audit of reservations per item)
- `id` (UUID)
- `order_id`
- `inventory_reservation_id`
- `status` (`PENDING`, `CONFIRMED`, `RELEASED`)

**Table: `idempotent_requests`**
- `idempotency_key`
- `user_id`
- `response_hash`
- `order_id`
- `created_at`

### Inventory/Product Service

**Table: `inventory`**
- `sku`
- `available_quantity`
- `reserved_quantity`
- `version` (for optimistic locking)

**Table: `inventory_reservations`**
- `id`
- `sku`
- `order_number`
- `quantity`
- `expires_at`
- `status` (`PENDING`, `CONFIRMED`, `RELEASED`, `EXPIRED`)

### API Contracts

**Inventory Service**
- `POST /inventory/reservations` – Reserve stock (Try). Body includes order number, items. Response 201 with reservation IDs.
- `POST /inventory/reservations/{reservationId}/confirm` – Confirm reservation (Confirm). Response 200. If conflict (not enough stock), respond 409 and include reason.
- `POST /inventory/reservations/{reservationId}/release` – Release reservation (Cancel). Idempotent.
- `POST /inventory/availability/check` – Validate price & availability snapshot (optional).

**Order Service**
- `POST /orders` – Accept idempotency key. Returns 201 with order resource or 202 if async processing.
- `POST /orders/{id}/cancel` – Cancel pending order; triggers inventory release & payment void.

### Transaction Boundaries and Saga

Use **outbox pattern** and **saga orchestration** (Order service orchestrates) with transactional boundaries:
- Each step persists state and publishes event via outbox within local transaction.
- Compensation actions: release inventory, void payment.
- If payment fails, order transitions to `FAILED`, inventory release event is sent.
- Use message broker for async retries.

## 4. Example `OrderService.createOrder`

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingClient pricingClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final IdempotencyService idempotencyService;

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, String idempotencyKey, String correlationId) {
        // 1. Idempotency check
        Optional<OrderDto> cached = idempotencyService.findResponse(idempotencyKey, request.getUserId());
        if (cached.isPresent()) {
            return cached.get();
        }

        PricingResponse pricing = pricingClient.repriceCart(request.getItems());
        if (!pricing.isUpToDate()) {
            throw new PriceConflictException("Pricing changed");
        }

        Order order = Order.builder()
                .orderNumber(orderNumberService.next())
                .userId(request.getUserId())
                .status(OrderStatus.PENDING_RESERVATION)
                .totalAmount(pricing.getGrandTotal())
                .currency(pricing.getCurrency())
                .idempotencyKey(idempotencyKey)
                .build();
        orderRepository.save(order);

        List<OrderItem> items = pricing.toOrderItems(order);
        orderItemRepository.saveAll(items);

        try {
            InventoryReservationResponse reservation = inventoryClient.reserve(order.getOrderNumber(), items, correlationId);
            order.setStatus(OrderStatus.RESERVED);
            orderRepository.save(order);

            PaymentAuthorizationResponse paymentAuth = paymentClient.authorize(order.getOrderNumber(), order.getTotalAmount(), correlationId);
            order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
            order.setPaymentAuthId(paymentAuth.authorizationId());
            orderRepository.save(order);

            inventoryClient.confirm(reservation.getReservationId(), correlationId);
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            OrderDto dto = orderMapper.toDto(order);
            idempotencyService.storeResponse(idempotencyKey, request.getUserId(), dto);
            outboxPublisher.publishOrderConfirmed(order, correlationId);
            return dto;
        } catch (InventoryReservationException e) {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            throw e;
        } catch (PaymentException e) {
            // Compensation: release inventory
            inventoryClient.release(order.getOrderNumber(), correlationId);
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            throw e;
        }
    }
}
```

Key points:
- Remote calls are outside single DB transaction but state transitions are persisted atomically per step.
- Compensation ensures inventory release if payment fails.
- Idempotency service caches responses keyed by user + idempotency key.
- Correlation IDs propagate for tracing/logging.

## 5. Inventory Reservation Logic

```java
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    @Transactional
    public InventoryReservation reserve(ReserveRequest request) {
        String correlationId = request.getCorrelationId();
        List<InventoryReservation> reservations = new ArrayList<>();

        for (ReserveItem item : request.getItems()) {
            Inventory inventory = inventoryRepository.findBySkuForUpdate(item.getSku())
                    .orElseThrow(() -> new InventoryNotFoundException(item.getSku()));

            if (inventory.getAvailableQuantity() - inventory.getReservedQuantity() < item.getQuantity()) {
                throw new InsufficientInventoryException(item.getSku());
            }

            inventory.incrementReserved(item.getQuantity());
            inventoryRepository.save(inventory);

            InventoryReservation reservation = InventoryReservation.builder()
                    .sku(item.getSku())
                    .orderNumber(request.getOrderNumber())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.PENDING)
                    .expiresAt(Instant.now().plusSeconds(900))
                    .correlationId(correlationId)
                    .build();
            reservations.add(reservation);
        }

        return reservationRepository.saveAll(reservations);
    }

    @Transactional
    public void confirmReservation(String reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);
        if (reservation.isExpired()) {
            throw new ReservationExpiredException();
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseReservation(String reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return; // idempotent
        }
        Inventory inventory = inventoryRepository.findBySkuForUpdate(reservation.getSku())
                .orElseThrow(() -> new InventoryNotFoundException(reservation.getSku()));
        inventory.decrementReserved(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);
    }
}
```

- `findBySkuForUpdate` uses pessimistic locking. Alternatively, use optimistic locking with `@Version` and retry on conflicts.
- A background job scans reservations and releases expired ones.

## 6. Best Practices

- **Idempotency**: Require `Idempotency-Key` header on POST `/orders`. Store responses for 24h. Return same response for duplicate keys.
- **HTTP Status Codes**:
  - 201 Created – order confirmed synchronously.
  - 202 Accepted – order accepted but still processing asynchronously.
  - 409 Conflict – price changed, inventory conflict.
  - 402 Payment Required – payment authorization failed.
  - 422 Unprocessable Entity – validation errors.
- **Resilience**: Use timeouts (e.g., Spring `WebClient` with `responseTimeout`), retries with backoff for transient failures (Resilience4j), and circuit breakers to prevent cascading failures.
- **Observability**: Propagate `X-Correlation-ID`, emit distributed traces (OpenTelemetry), log key state transitions, publish metrics (reservation failures, payment latency).
- **Security**: Validate user ownership of cart/order; ensure services authenticate via mTLS/JWT.
- **Data consistency**: Use outbox pattern to avoid lost events. Include versioning on entities for optimistic concurrency.

## 6.1 Generic Saga Pattern Template (Command-based, Orchestrated)

This template standardizes how orchestrated sagas are described and implemented across services, ensuring deterministic rollback, idempotency, and reuse of command steps.

### Problem Statement

In a distributed system, a single business operation may span many internal service calls. If any intermediate step fails, the system must roll back previously completed steps to maintain data consistency. Traditional database transactions cannot span multiple services, so we need a distributed transaction mechanism.

### Solution Overview

Use an orchestrated saga pattern with:

- A **Saga Manager** (orchestrator)
- A **Command interface** implemented by each step
- **Compensating (rollback) logic** for every successful step

The Saga Manager:

- Executes steps sequentially
- Tracks successfully completed steps
- Automatically triggers rollback when any step fails

### Core Design Principles

1. Each step must be reversible.
2. Rollback order is always reverse execution order.
3. Only completed steps are rolled back.
4. Saga Manager owns orchestration, not business logic.
5. Commands are reusable across sagas.

### Command Interface (Contract)

Every service operation participating in a saga must implement this interface:

```java
public interface SagaCommand {

    /**
     * Executes the business operation.
     * Must be idempotent.
     */
    void execute() throws Exception;

    /**
     * Compensates (rolls back) the executed operation.
     * Must also be idempotent.
     */
    void compensate() throws Exception;
}
```

### Saga Manager (Orchestrator)

```java
public class SagaManager {

    private final List<SagaCommand> commands = new ArrayList<>();
    private final Deque<SagaCommand> executedCommands = new ArrayDeque<>();

    public void addStep(SagaCommand command) {
        commands.add(command);
    }

    public void execute() throws Exception {
        try {
            for (SagaCommand command : commands) {
                command.execute();
                executedCommands.push(command);
            }
        } catch (Exception ex) {
            rollback();
            throw ex;
        }
    }

    private void rollback() {
        while (!executedCommands.isEmpty()) {
            try {
                executedCommands.pop().compensate();
            } catch (Exception rollbackEx) {
                // Log and continue rollback
            }
        }
    }
}
```

### How Services Participate in a Saga

Each service exposes two operations:

| Operation | Purpose |
| --- | --- |
| `execute()` | Perform the actual business action |
| `compensate()` | Undo the business action |

Example: Wallet debit command

```java
public class DebitWalletCommand implements SagaCommand {

    private final WalletService walletService;
    private final String walletId;
    private final BigDecimal amount;

    @Override
    public void execute() {
        walletService.debit(walletId, amount);
    }

    @Override
    public void compensate() {
        walletService.credit(walletId, amount);
    }
}
```

### Execution Flow Example (10–20 Services)

```
SagaManager
   |
   |-- Step 1: Validate User ✔
   |-- Step 2: Lock Wallet ✔
   |-- Step 3: Debit Wallet ✔
   |-- Step 4: Create Transaction ✔
   |-- Step 5: Call Aggregator ✔
   |-- Step 6: Update Ledger ✔
   |-- Step 7: Notify Service ❌ (FAILED)
   |
   └── ROLLBACK
         ├── Undo Step 6
         ├── Undo Step 5
         ├── Undo Step 4
         ├── Undo Step 3
         ├── Undo Step 2
         └── Undo Step 1
```

### Reuse Across the System

```java
SagaManager saga = new SagaManager();

saga.addStep(new ValidateUserCommand(...));
saga.addStep(new LockWalletCommand(...));
saga.addStep(new DebitWalletCommand(...));
saga.addStep(new CallAggregatorCommand(...));
saga.addStep(new LedgerUpdateCommand(...));

saga.execute();
```

### Applying the Template to Order Placement (This Repository)

The Order service acts as the orchestrator and maps its saga steps directly to Inventory and Payment operations. Each step is idempotent, and every successful step has an explicit compensation action.

```java
SagaManager saga = new SagaManager();

saga.addStep(new ReserveInventoryCommand(inventoryClient, order, items, correlationId));
saga.addStep(new AuthorizePaymentCommand(paymentClient, order, correlationId));
saga.addStep(new ConfirmInventoryCommand(inventoryClient, order, correlationId));

saga.execute();
```

Command responsibilities:

| Step | execute() | compensate() |
| --- | --- | --- |
| ReserveInventoryCommand | Create inventory reservation (Try) | Release reservation (Cancel) |
| AuthorizePaymentCommand | Authorize payment hold | Void authorization |
| ConfirmInventoryCommand | Confirm reservation (Confirm) | Release reservation if confirmation fails |

Operational rules:

1. **Persist order state per step** (e.g., `PENDING_RESERVATION` → `RESERVED` → `PAYMENT_AUTHORIZED` → `CONFIRMED`) so retries remain safe.
2. **Use outbox for events** (e.g., `OrderConfirmed`, `OrderFailed`) to avoid lost notifications.
3. **Propagate correlation IDs** across steps for traceability.
4. **Apply timeouts and retries** on network calls; avoid holding DB transactions open across remote calls.

## 7. Refactored Design Proposal

1. **Order Service acts as orchestrator** using saga pattern (Try-Confirm/Cancel). It maintains order state machine and idempotency table.
2. **Inventory Service provides reservation APIs** (try/confirm/cancel) with locking, expiry, and event emission.
3. **Payment Service** handles authorization/capture/void steps, triggered by order orchestrator.
4. **Messaging**: All services publish domain events to Kafka (or similar) using outbox pattern. Downstream services (fulfillment, notification) subscribe.
5. **Idempotent, resilient endpoints** with proper status codes, retries, and correlation IDs ensure robust client experience.
6. **Monitoring**: Each step emits traces, logs structured with `orderNumber`, `correlationId`, `reservationId`, enabling troubleshooting.

This architecture mirrors production ecommerce platforms: stateful order orchestration with inventory reservations and payment authorization, resilient APIs, compensating actions, and observability-first design.
