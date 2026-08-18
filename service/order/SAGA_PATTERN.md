# Saga Pattern - Distributed Transactions in Microservices

## What is the Saga Pattern?

In microservices, you can't use traditional database transactions across multiple services. The Saga pattern solves this by:
1. Breaking a transaction into **steps**
2. If any step fails, **rollback** the completed steps in reverse order

## The Problem

**Example: Order Creation**
```
Step 1: Reserve inventory  ✅
Step 2: Save order to DB   ✅
Step 3: Charge payment     ❌ FAILS!
```

**Without Saga**: Inventory reserved, order saved, but no payment = **Inconsistent data**

**With Saga**: Automatically rollback Step 2 and Step 1 = **Consistent data**

---

## How It Works

### Basic Flow

```java
SagaManager saga = new SagaManager();
saga.addStep(new ReserveProductsCommand(...));      // Step 1
saga.addStep(new PersistOrderCommand(...));         // Step 2  
saga.addStep(new ConfirmReservationCommand(...));   // Step 3
saga.execute();
```

### Success Path

```
Execute Step 1 → Success ✅
Execute Step 2 → Success ✅
Execute Step 3 → Success ✅
Done!
```

### Failure Path (Automatic Rollback)

```
Execute Step 1 → Success ✅
Execute Step 2 → Success ✅
Execute Step 3 → FAIL ❌
    ↓
Rollback Step 2 ✅ (Delete order from DB)
Rollback Step 1 ✅ (Release inventory)
Done - all cleaned up!
```

---

## Implementation Details

### SagaCommand Interface

Every saga step implements this interface:

```java
public interface SagaCommand {
    void execute() throws Exception;    // Do the action
    void rollback() throws Exception;   // Undo the action
}
```

### Example: Reserve Products

```java
public class ReserveProductsCommand implements SagaCommand {
    
    @Override
    public void execute() {
        // Reserve inventory in Product Service
        productService.reserveProducts(orderNumber);
    }
    
    @Override
    public void rollback() {
        // Release the reservation
        productService.releaseReservation(orderNumber);
    }
}
```

### SagaManager

```java
public class SagaManager {
    private List<SagaCommand> commands;
    private Stack<SagaCommand> executedCommands;
    
    public void execute() {
        try {
            for (SagaCommand command : commands) {
                command.execute();
                executedCommands.push(command);  // Track for rollback
            }
        } catch (Exception ex) {
            rollbackAll();  // Undo everything
            throw ex;
        }
    }
    
    private void rollbackAll() {
        while (!executedCommands.isEmpty()) {
            SagaCommand command = executedCommands.pop();
            command.rollback();  // Undo in reverse order
        }
    }
}
```

---

## Handling Rollback Failures

### The Problem

What if rollback fails? (Service is down, database error, etc.)

```
Step 3 fails → Start rollback
Rollback Step 2 → Database error! ❌
Rollback Step 1 → Service down! ❌
```

**Result**: Inconsistent state

### The Solution: Retry with Backoff

```java
private void rollbackAll() {
    while (!executedCommands.isEmpty()) {
        SagaCommand command = executedCommands.pop();
        
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                command.rollback();
                break;  // Success!
            } catch (Exception ex) {
                if (attempt == 2) {
                    // Log critical error - needs manual fix
                }
                Thread.sleep(1000 * (attempt + 1));  // Wait 1s, 2s, 3s
            }
        }
    }
}
```

**Retry Strategy:**
- Attempt 1: Try immediately
- Attempt 2: Wait 1 second, try again
- Attempt 3: Wait 2 seconds, try again
- After 3 attempts: Log error for manual intervention

---

## Our Order Creation Saga

### The Three Steps

| Step | Action | Rollback |
|------|--------|----------|
| 1. ReserveProducts | Reserve inventory | Release reservation |
| 2. PersistOrder | Save order to DB | Delete order |
| 3. ConfirmReservation | Confirm inventory | Release reservation |

### Full Example

```java
@Service
public class OrderServiceImpl {
    
    public OrderDto createOrder(CreateOrderRequest request) {
        String orderNumber = generateOrderNumber();
        OrderSagaContext context = new OrderSagaContext(request, orderNumber);
        
        SagaManager saga = new SagaManager();
        saga.addStep(new ReserveProductsCommand(productService, context));
        saga.addStep(new PersistOrderCommand(orderRepository, context));
        saga.addStep(new ConfirmReservationCommand(productService, context));
        
        saga.execute();  // Execute all steps with automatic rollback on failure
        
        return orderMapper.toDto(context.getSavedOrder());
    }
}
```

### Scenario 1: All Success

```
1. Reserve inventory     ✅
2. Save order to DB      ✅
3. Confirm reservation   ✅
→ Order created successfully
```

### Scenario 2: Confirm Fails

```
1. Reserve inventory     ✅
2. Save order to DB      ✅
3. Confirm reservation   ❌ FAILS (Product service down)
   ↓ Start rollback
2. Delete order          ✅ (Retried 1 time)
1. Release reservation   ✅ (Retried 2 times, succeeded)
→ Order creation failed, system is consistent
```

---

## Best Practices

### 1. Make Rollbacks Idempotent

Safe to call multiple times:

```java
@Override
public void rollback() {
    if (context.getSavedOrder() != null) {  // Check before delete
        orderRepository.delete(context.getSavedOrder());
    }
}
```

### 2. Don't Rollback Operations That Can't Be Undone

```java
// ❌ BAD - Can't unsend email
saga.addStep(new SendEmailCommand(...));

// ✅ GOOD - Send email AFTER saga completes
saga.execute();
emailService.sendConfirmation();  // Only after all steps succeed
```

### 3. Use Semantic Locking

Use status fields instead of database locks:

```java
product.setStatus("RESERVED");    // During saga
product.setStatus("AVAILABLE");   // After rollback
product.setStatus("SOLD");        // After success
```

### 4. Set Timeouts

Fail fast instead of hanging forever:

```java
@FeignClient(name = "product-service",
    configuration = {connectTimeout = 2000, readTimeout = 5000})
```

---

## Summary

**Saga Pattern** = Multi-step transaction with automatic rollback

✅ **Simple**: Execute steps, rollback on failure  
✅ **Reliable**: Retry failed rollbacks automatically  
✅ **Clear**: Easy to understand and debug  
✅ **Standard**: Uses well-known term "rollback" not jargon  

**Key Points:**
- Each step has `execute()` and `rollback()`
- Failed rollbacks are retried 3 times with backoff
- Rollback happens in reverse order
- Keep it simple - no over-engineering needed

---

## Theory

### What is the Saga Pattern?

The **Saga Pattern** manages **distributed transactions** across multiple microservices. It maintains data consistency without distributed locking or two-phase commits.

### The Problem

In microservices, each service has its own database. Traditional ACID transactions cannot span multiple databases because:
- Each service maintains its own data store
- Network calls can fail
- Services can be temporarily unavailable

### The Solution

The Saga pattern breaks a distributed transaction into **local transactions**. Each local transaction:
1. Updates data within a single service
2. If a step fails, the saga executes **compensating transactions** to undo completed steps

---

## Core Concepts

### 1. Local Transaction
A transaction that operates on a single service's database.

### 2. Compensation (Rollback)
A **compensating transaction** undoes the work of a previous local transaction. It must be:
- **Idempotent**: Can be executed multiple times safely
- **Reversible**: Undoes the business operation

### 3. Saga State Persistence
The saga's execution state is persisted to handle:
- Compensation failures
- Service restarts during saga execution
- Retry mechanisms

### 4. Semantic Lock
Business-level locking (e.g., "reserved" status) instead of database locks.

---

## How It Works

### Forward Execution (Happy Path)

1. **Start Saga**: Create saga execution record
2. **Execute Step 1**: First command executes, state persisted
3. **Execute Step 2**: Second command executes, state persisted
4. **Execute Step 3**: Third command executes, state persisted
5. **Complete**: Mark saga as COMPLETED

### Backward Recovery (Failure Path)

1. **Failure Detected**: Step N fails
2. **Mark Saga Failed**: Update state to FAILED
3. **Start Compensation**: Update state to COMPENSATING
4. **Compensate with Retry**: Each compensation retried up to 3 times
5. **Success**: Mark saga as FAILED with compensation complete
6. **Failure**: Mark as COMPENSATION_FAILED for manual intervention

---

## Compensation Failure Handling

### The Critical Problem

**Scenario**: Product service is down
1. Reserve Products ✅ (Product service online)
2. Persist Order ✅
3. Confirm Reservation ❌ (Product service goes down)
4. **Rollback** PersistOrderCommand.compensate() ❌ (Database fails)
5. **Rollback** ReserveProductsCommand.compensate() ❌ (Product service still down)

**Without proper handling**: Order exists, products reserved = INCONSISTENT STATE

### Our Solution: Saga State Persistence + Retry

#### Saga Execution Table

```sql
CREATE TABLE saga_executions (
    id UUID PRIMARY KEY,
    order_number VARCHAR UNIQUE NOT NULL,
    status VARCHAR NOT NULL, -- IN_PROGRESS, COMPLETED, FAILED, COMPENSATING, COMPENSATION_FAILED
    current_step INTEGER,
    total_steps INTEGER,
    last_error TEXT,
    retry_count INTEGER DEFAULT 0,
    compensation_pending BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### Compensation with Automatic Retry

```java
private void compensateWithRetry(SagaExecution sagaExecution) {
    sagaExecution.setStatus(SagaStatus.COMPENSATING);
    sagaExecutionRepository.save(sagaExecution);
    
    List<Exception> compensationFailures = new ArrayList<>();
    
    while (!executedCommands.isEmpty()) {
        SagaCommand command = executedCommands.pop();
        boolean compensated = false;
        
        for (int attempt = 0; attempt < 3 && !compensated; attempt++) {
            try {
                command.compensate();
                compensated = true;
            } catch (Exception ex) {
                if (attempt == 2) {
                    compensationFailures.add(ex);
                } else {
                    Thread.sleep(1000 * (attempt + 1)); // 1s, 2s backoff
                }
            }
        }
    }
    
    if (!compensationFailures.isEmpty()) {
        sagaExecution.setStatus(SagaStatus.COMPENSATION_FAILED);
        sagaExecution.setCompensationPending(true);
        throw new SagaCompensationException("Manual intervention required");
    }
}
```

#### Saga States

| State | Meaning | Next Action |
|-------|---------|-------------|
| `IN_PROGRESS` | Saga executing forward | Continue execution or fail |
| `COMPLETED` | All steps succeeded | None |
| `FAILED` | Forward execution failed, compensation succeeded | None |
| `COMPENSATING` | Currently rolling back | Continue compensation |
| `COMPENSATION_FAILED` | Rollback failed after retries | **Manual intervention required** |

#### Manual Intervention Process

When `COMPENSATION_FAILED` occurs:

1. **Alert**: System alerts operations team
2. **Query**: `SELECT * FROM saga_executions WHERE status = 'COMPENSATION_FAILED'`
3. **Investigate**: Check `last_error` and `current_step`
4. **Manual Fix**: 
   - Delete orphaned order records
   - Release product reservations via admin API
   - Or wait for service recovery and trigger retry
5. **Mark Complete**: Update status to `FAILED` after manual cleanup

---

## Practical Implementation

### Our Order Creation Saga

```java
SagaManager sagaManager = new SagaManager(sagaExecutionRepository, orderNumber);
sagaManager.addStep(new ReserveProductsCommand(productPurchaseService, context));
sagaManager.addStep(new PersistOrderCommand(orderRepository, orderMapper, orderItemFactory, context));
sagaManager.addStep(new ConfirmReservationCommand(productPurchaseService, context));
sagaManager.execute();
```

### Step-by-Step Flow

#### Step 1: Reserve Products

**Execute:**
- Call Product Service to reserve inventory
- Mark products as "RESERVED"

**Compensate:**
- Release reservation (retried up to 3 times)

#### Step 2: Persist Order

**Execute:**
- Save order to database with status PENDING

**Compensate:**
- Delete order from database (retried up to 3 times)

#### Step 3: Confirm Reservation

**Execute:**
- Confirm reservation in Product Service
- Products move from "RESERVED" to "SOLD"

**Compensate:**
- Release reservation (idempotent, retried up to 3 times)

### Example Scenarios

#### Scenario 1: All Steps Succeed ✅

```
1. ReserveProductsCommand.execute()    → Saga state: IN_PROGRESS (step 1)
2. PersistOrderCommand.execute()        → Saga state: IN_PROGRESS (step 2)
3. ConfirmReservationCommand.execute()  → Saga state: IN_PROGRESS (step 3)
4. Complete                             → Saga state: COMPLETED
```

#### Scenario 2: Compensation Succeeds After Retry ✅

```
1. ReserveProductsCommand.execute()       → Success
2. PersistOrderCommand.execute()          → Success
3. ConfirmReservationCommand.execute()    → Fail (Product service down)
4. Mark saga FAILED, start compensation
5. ConfirmReservationCommand.compensate() → Attempt 1: Fail, Attempt 2: Success
6. PersistOrderCommand.compensate()       → Success
7. ReserveProductsCommand.compensate()    → Attempt 1: Fail, Attempt 2: Success
8. Saga state: FAILED (compensated successfully)
```

#### Scenario 3: Compensation Fails - Manual Intervention ❌

```
1. ReserveProductsCommand.execute()       → Success
2. PersistOrderCommand.execute()          → Success
3. ConfirmReservationCommand.execute()    → Fail
4. PersistOrderCommand.compensate()       → All 3 attempts fail
5. Saga state: COMPENSATION_FAILED
6. Alert operations team
7. Manual cleanup required
```

---

## Best Practices

### 1. Idempotency

Make all operations safe to retry:

```java
@Override
public void compensate() {
    if (context.getSavedOrder() != null) {
        orderRepository.delete(context.getSavedOrder());
    }
}
```

### 2. Retry with Backoff

The saga manager automatically retries compensations with exponential backoff (1s, 2s).

### 3. Monitor Compensation Failures

```sql
-- Alert when compensation fails
SELECT order_number, last_error, retry_count, updated_at
FROM saga_executions
WHERE status = 'COMPENSATION_FAILED'
AND compensation_pending = TRUE;
```

### 4. Background Job for Retry

Create a scheduled job to retry failed compensations:

```java
@Scheduled(fixedDelay = 300000) // Every 5 minutes
public void retryFailedCompensations() {
    List<SagaExecution> failed = sagaExecutionRepository
        .findByStatusAndRetryCountLessThan(SagaStatus.COMPENSATION_FAILED, 10);
    
    for (SagaExecution saga : failed) {
        // Retry compensation logic
    }
}
```

### 5. Semantic Locking

```java
order.setStatus(OrderStatus.PENDING);    // During processing
order.setStatus(OrderStatus.CONFIRMED);  // After success

product.setStatus(ProductStatus.RESERVED);  // During saga
product.setStatus(ProductStatus.AVAILABLE); // After compensation
```

### 6. Timeout Handling

Set reasonable timeouts for external service calls to fail fast rather than hang.

---

## Summary

Key improvements over basic saga:

✅ **Saga state persistence** - Survives service restarts  
✅ **Automatic retry** - 3 attempts per compensation with backoff  
✅ **Failure visibility** - Clear state tracking (COMPENSATION_FAILED)  
✅ **Manual intervention support** - Operations team can identify and fix issues  
✅ **Idempotent operations** - Safe to retry  
✅ **No data loss** - All failures recorded in database

---

## Theory

### What is the Saga Pattern?

The **Saga Pattern** is a design pattern used to manage **distributed transactions** across multiple microservices. Unlike traditional ACID transactions (which work within a single database), the Saga pattern maintains **data consistency** across multiple services without relying on distributed locking or two-phase commits.

### The Problem

In a monolithic application, when you need to perform multiple operations (e.g., debit account, update inventory, create order), you wrap them in a single database transaction:

```
BEGIN TRANSACTION
  - Debit customer account
  - Update inventory
  - Create order
COMMIT TRANSACTION
```

If anything fails, the entire transaction is rolled back automatically.

In **microservices**, each service has its own database. You cannot have a single transaction spanning multiple databases because:
- Each service maintains its own data store
- Network calls can fail
- Services can be temporarily unavailable
- Distributed transactions (2PC - Two-Phase Commit) are complex, slow, and can lead to deadlocks

### The Solution

The Saga pattern breaks a distributed transaction into a series of **local transactions**, where each local transaction:
1. **Updates data** within a single service
2. **Publishes an event** or **invokes the next step**
3. If a step fails, the saga executes **compensating transactions** to undo the completed steps

---

## Why Saga Pattern?

### Without Saga Pattern

❌ **Inconsistent State**: If order creation succeeds but payment fails, you have an orphaned order  
❌ **No Rollback Mechanism**: Manual cleanup required  
❌ **Poor User Experience**: Users don't know what happened  
❌ **Data Corruption**: Partial updates across services  

### With Saga Pattern

✅ **Automatic Rollback**: Failed transactions are compensated automatically  
✅ **Data Consistency**: Eventually consistent across services  
✅ **Better Resilience**: Handles partial failures gracefully  
✅ **Clear Audit Trail**: Track what succeeded and what was rolled back  
✅ **Scalability**: No distributed locks or global transactions  

---

## Types of Saga Patterns

### 1. Orchestration-based Saga (Used in this project)

A central **orchestrator** (Saga Manager) controls the execution flow and decides which steps to execute and when to compensate.

**Pros:**
- Centralized control and monitoring
- Easy to understand and debug
- Clear business logic flow
- Simpler error handling

**Cons:**
- Single point of failure
- Can become complex with many services
- Orchestrator has knowledge of all participants

**When to use:**
- When you have a clear sequence of steps
- When you want centralized monitoring
- When the business process is complex

### 2. Choreography-based Saga

Services communicate through **events** without a central coordinator. Each service listens to events and performs its action and publishes new events.

**Pros:**
- No single point of failure
- Better service independence
- More scalable for simple workflows

**Cons:**
- Harder to track and debug
- Risk of cyclic dependencies
- Complex error handling
- Difficult to understand the overall flow

**When to use:**
- For simple linear workflows
- When services should be completely decoupled
- Event-driven architecture

---

## Core Concepts

### 1. Local Transaction
A transaction that operates on a single service's database.

### 2. Compensation (Rollback)
A **compensating transaction** undoes the work of a previous local transaction. It must be:
- **Idempotent**: Can be executed multiple times safely
- **Reversible**: Undoes the business operation
- **Reliable**: Should not fail under normal circumstances

### 3. Saga Coordinator/Orchestrator
The component responsible for:
- Executing saga steps in order
- Tracking which steps have completed
- Triggering compensations on failure
- Maintaining saga state

### 4. Semantic Lock
Business-level locking (e.g., "reserved" status) instead of database locks.

### 5. Saga Context
Shared state/data passed between saga steps (like OrderSagaContext in our implementation).

---

## How It Works

### Execution Flow

```
┌─────────────────────────────────────────────────────┐
│              SAGA ORCHESTRATOR                      │
└─────────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
    ┌───────┐       ┌───────┐      ┌───────┐
    │Step 1 │       │Step 2 │      │Step 3 │
    │Execute│ ───→  │Execute│ ───→ │Execute│
    └───────┘       └───────┘      └───────┘
        │               │              │
        ▼               ▼              ▼
     SUCCESS         SUCCESS         FAILURE!
        │               │              │
        │               │              ▼
        │               │         ┌──────────┐
        │               │    ┌───│ Rollback │
        │               │    │    └──────────┘
        │               ▼    ▼
        │           ┌──────────┐
        │      ┌────│Compensate│
        │      │    │ Step 2   │
        │      │    └──────────┘
        ▼      ▼
    ┌──────────┐
    │Compensate│
    │ Step 1   │
    └──────────┘
```

### Forward Execution (Happy Path)

1. **Start Saga**: Saga manager begins execution
2. **Execute Step 1**: First command executes successfully
3. **Track Success**: Command pushed to executed stack
4. **Execute Step 2**: Second command executes successfully
5. **Track Success**: Command pushed to executed stack
6. **Execute Step 3**: Third command executes successfully
7. **Complete**: All steps successful, saga completes

### Backward Recovery (Failure Path)

1. **Failure Detected**: Step N fails with exception
2. **Stop Forward Progress**: No more steps executed
3. **Start Compensation**: Pop last successful step from stack
4. **Compensate Step N-1**: Undo the previous step
5. **Continue Rollback**: Pop and compensate each step in reverse order
6. **Complete Rollback**: All completed steps undone
7. **Throw Exception**: Original exception propagated to caller

### Key Principles

1. **Only compensate completed steps**: Failed step is not compensated
2. **Reverse order compensation**: Last executed = first compensated (LIFO)
3. **Best-effort rollback**: Continue compensating even if one fails
4. **Preserve original exception**: Error context maintained

---

## Practical Implementation

### Our Order Creation Saga

The order service implements an **orchestration-based saga** for order creation with three steps:

```java
SagaManager sagaManager = new SagaManager();
sagaManager.addStep(new ReserveProductsCommand(productPurchaseService, context));
sagaManager.addStep(new PersistOrderCommand(orderRepository, orderMapper, orderItemFactory, context));
sagaManager.addStep(new ConfirmReservationCommand(productPurchaseService, context));
sagaManager.execute();
```

### Step-by-Step Flow

#### Step 1: Reserve Products

**Execute:**
```java
- Call Product Service to reserve inventory
- Store reservation response in context
- Mark products as "RESERVED" in Product Service
```

**Compensate:**
```java
- Call Product Service to release reservation
- Products return to "AVAILABLE" state
```

#### Step 2: Persist Order

**Execute:**
```java
- Map request to Order entity
- Calculate total amount
- Set status to PENDING
- Save order to database
- Store saved order in context
```

**Compensate:**
```java
- Delete order from database
- Remove all order items
```

#### Step 3: Confirm Reservation

**Execute:**
```java
- Call Product Service to confirm reservation
- Products move from "RESERVED" to "SOLD"
- Inventory permanently reduced
```

**Compensate:**
```java
- Release reservation (idempotent operation)
- Products return to "AVAILABLE" state
```

### Architecture Components

```
┌─────────────────────────────────────────────────────────┐
│                  OrderServiceImpl                       │
│  - Validates user                                       │
│  - Creates OrderSagaContext                             │
│  - Builds and executes saga                             │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│                   SagaManager                           │
│  - Stores list of commands                              │
│  - Executes commands sequentially                       │
│  - Tracks executed commands (Stack)                     │
│  - Handles rollback on failure                          │
└─────────────────────────────────────────────────────────┘
                        │
        ┌───────────────┼──────────────┐
        ▼               ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Reserve    │ │   Persist    │ │   Confirm    │
│   Products   │ │    Order     │ │ Reservation  │
│   Command    │ │   Command    │ │   Command    │
└──────────────┘ └──────────────┘ └──────────────┘
        │               │              │
        ▼               ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Product    │ │    Order     │ │   Product    │
│   Service    │ │  Repository  │ │   Service    │
└──────────────┘ └──────────────┘ └──────────────┘
```

### SagaCommand Interface

```java
public interface SagaCommand {
    void execute() throws Exception;    // Forward action
    void compensate() throws Exception; // Rollback action
}
```

Every command must implement:
- **execute()**: Performs the business operation
- **compensate()**: Undoes the business operation

### OrderSagaContext

```java
@Getter @Setter
public class OrderSagaContext {
    private final CreateOrderRequest request;
    private final String orderNumber;
    private PurchaseProductResponseDto reservationResponse;
    private Order savedOrder;
    private List<OrderItem> orderItems;
}
```

**Purpose:**
- Share data between saga steps
- Avoid re-fetching data from services
- Maintain saga state

### Example Scenarios

#### Scenario 1: All Steps Succeed ✅

```
1. ReserveProductsCommand.execute()    → Products reserved
2. PersistOrderCommand.execute()        → Order saved to DB
3. ConfirmReservationCommand.execute()  → Reservation confirmed
✅ Saga Complete: Order created successfully
```

#### Scenario 2: Order Persistence Fails ❌

```
1. ReserveProductsCommand.execute()    → Products reserved ✅
2. PersistOrderCommand.execute()        → Database error ❌
3. ROLLBACK INITIATED
   - PersistOrderCommand.compensate()  → (Nothing to delete)
   - ReserveProductsCommand.compensate() → Reservation released
❌ Saga Failed: Order not created, inventory restored
```

#### Scenario 3: Confirmation Fails ❌

```
1. ReserveProductsCommand.execute()       → Products reserved ✅
2. PersistOrderCommand.execute()          → Order saved ✅
3. ConfirmReservationCommand.execute()    → Product service down ❌
4. ROLLBACK INITIATED
   - ConfirmReservationCommand.compensate()  → Release reservation
   - PersistOrderCommand.compensate()        → Delete order from DB
   - ReserveProductsCommand.compensate()     → Release reservation (idempotent)
❌ Saga Failed: Order deleted, inventory restored
```

---

## Best Practices

### 1. Idempotency

**Why:** Network issues may cause retries; operations should be safe to execute multiple times.

```java
// ✅ Good: Check before compensation
@Override
public void compensate() {
    if (context.getSavedOrder() != null) {
        orderRepository.delete(context.getSavedOrder());
    }
}

// ❌ Bad: Assume order exists
@Override
public void compensate() {
    orderRepository.delete(context.getSavedOrder()); // Can throw NPE
}
```

### 2. Compensating Transactions Should Rarely Fail

Design compensations to be:
- Simple and reliable
- Gracefully handle "already compensated" scenarios
- Log errors but continue rollback chain

```java
@Override
public void compensate() {
    try {
        productPurchaseService.releaseReservation(context.getOrderNumber());
    } catch (Exception e) {
        // Idempotent - may already be released
        log.debug("Reservation already released: {}", context.getOrderNumber());
    }
}
```

### 3. Comprehensive Logging

```java
log.debug("Executing saga step {}: {}", stepNumber, command.getClass().getSimpleName());
log.error("Saga failed at step {}: {}", stepNumber, failedCommand, ex);
log.warn("Starting rollback of {} saga steps", totalSteps);
```

**Benefits:**
- Debug failures quickly
- Understand rollback flow
- Audit trail for compliance

### 4. Use Semantic Locking

Instead of database locks:
```java
// Status-based locking
order.setStatus(OrderStatus.PENDING);    // During processing
order.setStatus(OrderStatus.CONFIRMED);  // After success
order.setStatus(OrderStatus.FAILED);     // After rollback

product.setStatus(ProductStatus.RESERVED);  // During saga
product.setStatus(ProductStatus.AVAILABLE); // After compensation
```

### 5. Handle Partial Failures Gracefully

```java
private void rollback() {
    while (!executedCommands.isEmpty()) {
        try {
            executedCommands.pop().compensate();
        } catch (Exception rollbackEx) {
            // Log and continue - don't let one failure stop entire rollback
            log.error("Compensation failed, continuing rollback", rollbackEx);
        }
    }
}
```

### 6. Context Isolation

Each saga execution should have its own context instance:
```java
OrderSagaContext context = new OrderSagaContext(request, orderNumber);
```

### 7. Timeout Handling

```java
// Set reasonable timeouts for external service calls
@FeignClient(name = "product-service", 
    configuration = FeignConfig.class,
    fallback = ProductServiceFallback.class)
```

---

## Common Pitfalls

### ❌ 1. Non-Reversible Operations

**Problem:**
```java
public void execute() {
    emailService.sendConfirmationEmail(); // Can't unsend email!
}
```

**Solution:** Only send emails/notifications AFTER saga completes:
```java
sagaManager.execute();
// Only send after all steps succeed
emailService.sendConfirmationEmail();
```

### ❌ 2. Forgetting to Track Executed Commands

**Problem:**
```java
command.execute();
// Forgot: executedCommands.push(command);
```

**Impact:** Command won't be compensated on rollback.

### ❌ 3. Compensating Failed Steps

**Problem:**
```java
for (SagaCommand cmd : commands) {
    cmd.execute();
    executedCommands.push(cmd); // Push even on failure
}
```

**Solution:** Only push successful commands:
```java
cmd.execute();
executedCommands.push(cmd); // Only reached if execute() succeeds
```

### ❌ 4. Ignoring Compensation Failures

**Problem:**
```java
try {
    command.compensate();
} catch (Exception e) {
    // Silently ignore
}
```

**Solution:** Log and continue:
```java
try {
    command.compensate();
} catch (Exception e) {
    log.error("Compensation failed for {}", command.getClass(), e);
    // Continue with other compensations
}
```

### ❌ 5. Sharing Mutable State Between Commands

**Problem:**
```java
// Commands modifying shared list
private List<Product> sharedProducts;
```

**Solution:** Use immutable context or proper encapsulation:
```java
context.setReservationResponse(response); // Immutable after set
```

### ❌ 6. Not Making Compensations Idempotent

**Problem:**
```java
public void compensate() {
    inventory.add(quantity); // Adds every time, even if already added
}
```

**Solution:**
```java
public void compensate() {
    if (reservation.exists()) {
        reservation.release();
    }
}
```

---

## Monitoring and Observability

### Key Metrics to Track

1. **Saga Success Rate**: Percentage of sagas that complete successfully
2. **Rollback Frequency**: How often compensations are triggered
3. **Step Failure Distribution**: Which steps fail most often
4. **Compensation Success Rate**: How often compensations succeed
5. **Saga Execution Time**: Performance monitoring

### Log Example

```
[DEBUG] Executing saga step 1: ReserveProductsCommand
[DEBUG] Successfully reserved 3 products for order: ORD-2026-001
[DEBUG] Successfully completed saga step 1: ReserveProductsCommand

[DEBUG] Executing saga step 2: PersistOrderCommand
[DEBUG] Successfully persisted order: ORD-2026-001 with 3 items, total: $299.99
[DEBUG] Successfully completed saga step 2: PersistOrderCommand

[DEBUG] Executing saga step 3: ConfirmReservationCommand
[ERROR] Saga failed at step 3: ConfirmReservationCommand. Initiating rollback of 2 completed steps

[WARN] Starting rollback of 2 saga steps
[DEBUG] Rolling back step 1/2: PersistOrderCommand
[DEBUG] Deleting persisted order during compensation: ORD-2026-001
[DEBUG] Successfully rolled back step 1/2: PersistOrderCommand

[DEBUG] Rolling back step 2/2: ReserveProductsCommand
[DEBUG] Releasing product reservation for order: ORD-2026-001
[DEBUG] Successfully rolled back step 2/2: ReserveProductsCommand
[WARN] Rollback completed. Rolled back 2 steps
```

---

## Further Reading

### Books
- **"Microservices Patterns" by Chris Richardson** - Comprehensive coverage of Saga pattern
- **"Building Microservices" by Sam Newman** - Distributed transactions chapter

### Articles
- [Saga Pattern | Microsoft](https://docs.microsoft.com/en-us/azure/architecture/reference-architectures/saga/saga)
- [Pattern: Saga | Microservices.io](https://microservices.io/patterns/data/saga.html)

### Related Patterns
- **Outbox Pattern**: Reliably publish events as part of database transaction
- **Event Sourcing**: Store state changes as sequence of events
- **CQRS**: Separate read and write models

---

## Summary

The Saga pattern is essential for maintaining **data consistency** in microservices without distributed transactions. Key takeaways:

✅ **Use orchestration** for complex workflows with clear sequencing  
✅ **Make operations idempotent** to handle retries safely  
✅ **Design compensations carefully** - they should rarely fail  
✅ **Log extensively** for debugging and monitoring  
✅ **Test failure scenarios** to ensure rollback works correctly  
✅ **Use semantic locking** instead of database locks  

Our implementation provides a **production-ready, maintainable** saga pattern that can be extended for more complex workflows involving payment processing, shipping, notifications, and more.
