package com.nahid.order.service.impl;

import com.nahid.order.dto.OrderEventDto;
import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.request.OrderDto;
import com.nahid.order.entity.Order;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.exception.OrderNotFoundException;
import com.nahid.order.exception.OrderProcessingException;
import com.nahid.order.mapper.OrderMapper;
import com.nahid.order.producer.OrderEventPublisher;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.saga.ConfirmReservationCommand;
import com.nahid.order.saga.OrderSagaContext;
import com.nahid.order.saga.PersistOrderCommand;
import com.nahid.order.saga.ReserveProductsCommand;
import com.nahid.order.saga.SagaManager;
import com.nahid.order.service.OrderNumberService;
import com.nahid.order.service.OrderItemFactory;
import com.nahid.order.service.OrderService;
import com.nahid.order.service.OrderStatusService;
import com.nahid.order.service.ProductPurchaseService;
import com.nahid.order.service.UserValidationService;
import com.nahid.order.util.annotation.Auditable;
import com.nahid.order.util.constant.ExceptionMessageConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.nahid.order.util.constant.AppConstant.ORDER;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserValidationService userValidationService;
    private final ProductPurchaseService productPurchaseService;
    private final OrderStatusService orderStatusService;
    private final OrderNumberService orderNumberService;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderItemFactory orderItemFactory;

    @Override
    @Auditable(eventType = "CREATE", entityName = ORDER, action = "CREATE_ORDER")
    public OrderDto createOrder(CreateOrderRequest request) {
        try {
            userValidationService.validateUserForOrder(request.getUserId());

            String orderNumber = orderNumberService.generateOrderNumber();
            OrderSagaContext context = new OrderSagaContext(request, orderNumber);

            SagaManager sagaManager = new SagaManager();
            sagaManager.addStep(new ReserveProductsCommand(productPurchaseService, context));
            sagaManager.addStep(new PersistOrderCommand(orderRepository, orderMapper, orderItemFactory, context));
            sagaManager.addStep(new ConfirmReservationCommand(productPurchaseService, context));
            sagaManager.execute();

            Order savedOrder = context.getSavedOrder();
            publishOrderEvent(savedOrder, OrderStatus.PENDING);

            return orderMapper.toDto(savedOrder);

        } catch (Exception e) {
            throw new OrderProcessingException(
                    String.format(ExceptionMessageConstant.ORDER_CREATION_FAILED, e.getMessage()), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format(ExceptionMessageConstant.ORDER_NOT_FOUND, orderId)));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format(ExceptionMessageConstant.ORDER_NOT_FOUND_BY_NUMBER, orderNumber)));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(orderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toDto);
    }

    @Override
    @Auditable(eventType = "UPDATE", entityName = ORDER, action = "UPDATE_ORDER_STATUS")
    public OrderDto updateOrderStatus(UUID orderId, OrderStatus status) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            String.format(ExceptionMessageConstant.ORDER_NOT_FOUND, orderId)));

            orderStatusService.validateStatusTransition(order.getStatus(), status);
            order.setStatus(status);

            Order savedOrder = orderRepository.save(order);
            return orderMapper.toDto(savedOrder);
        } catch (OrderNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderProcessingException(
                    String.format(ExceptionMessageConstant.ORDER_UPDATE_FAILED, e.getMessage()), e);
        }
    }

    @Override
    @Auditable(eventType = "UPDATE", entityName = ORDER, action = "CANCEL_ORDER")
    public void cancelOrder(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            String.format(ExceptionMessageConstant.ORDER_NOT_FOUND, orderId)));

            orderStatusService.validateCancellation(order.getStatus());

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            publishOrderEvent(order, OrderStatus.CANCELLED);

        } catch (OrderProcessingException | OrderNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderProcessingException(
                    String.format(ExceptionMessageConstant.ORDER_CANCELLATION_FAILED, e.getMessage()), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, Pageable.unpaged())
                .getContent()
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getOrderCountByUserAndStatus(Long userId, OrderStatus status) {
        return orderRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    @Transactional
    @Auditable(eventType = "UPDATE", entityName = ORDER, action = "PROCESS_PAYMENT_RESULT")
    public void processPaymentResult(com.nahid.order.dto.event.PaymentResultEventDto paymentResultEvent) {
        if (paymentResultEvent == null || paymentResultEvent.getOrderId() == null) {
            log.warn("Received null or invalid PaymentResultEvent");
            return;
        }

        UUID orderId = paymentResultEvent.getOrderId();
        java.util.Optional<Order> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isEmpty()) {
            log.warn("Order not found for orderId: {}, skipping payment result processing safely", orderId);
            return;
        }

        Order order = optionalOrder.get();
        com.nahid.order.enums.PaymentStatus paymentStatus = paymentResultEvent.getStatus();

        // Idempotency check: if paymentId already processed or order already in expected state
        if (paymentResultEvent.getPaymentId() != null && paymentResultEvent.getPaymentId().equals(order.getPaymentId())) {
            log.info("PaymentResult for paymentId {} has already been processed for order {}",
                    paymentResultEvent.getPaymentId(), orderId);
            return;
        }

        if (paymentStatus == com.nahid.order.enums.PaymentStatus.COMPLETED) {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                log.info("Order {} is already in status CONFIRMED. Skipping duplicate payment result processing for paymentId {}",
                        orderId, paymentResultEvent.getPaymentId());
                return;
            }

            orderStatusService.validateStatusTransition(order.getStatus(), OrderStatus.CONFIRMED);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentId(paymentResultEvent.getPaymentId());
            Order savedOrder = orderRepository.save(order);

            log.info("Payment SUCCESS: Transitioned Order {} from PENDING to CONFIRMED (Payment ID: {})",
                    orderId, paymentResultEvent.getPaymentId());
            publishOrderEvent(savedOrder, OrderStatus.CONFIRMED);

        } else if (paymentStatus == com.nahid.order.enums.PaymentStatus.FAILED) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.info("Order {} is already in status CANCELLED. Skipping duplicate payment result processing for paymentId {}",
                        orderId, paymentResultEvent.getPaymentId());
                return;
            }

            orderStatusService.validateStatusTransition(order.getStatus(), OrderStatus.CANCELLED);
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentId(paymentResultEvent.getPaymentId());
            Order savedOrder = orderRepository.save(order);

            // Release inventory reservation for cancelled order
            try {
                productPurchaseService.releaseReservation(order.getOrderNumber());
            } catch (Exception e) {
                log.warn("Failed to release inventory reservation for cancelled order {}: {}",
                        order.getOrderNumber(), e.getMessage());
            }

            log.info("Payment FAILED: Transitioned Order {} to CANCELLED (Payment ID: {}, Reason: {})",
                    orderId, paymentResultEvent.getPaymentId(), paymentResultEvent.getFailureReason());
            publishOrderEvent(savedOrder, OrderStatus.CANCELLED);
        } else {
            log.info("Payment status {} for order {} does not require order transition", paymentStatus, orderId);
        }
    }

    private void publishOrderEvent(Order order, OrderStatus status) {
        try {
            OrderEventDto orderEvent = OrderEventDto.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId())
                    .status(status)
                    .totalAmount(order.getTotalAmount())
                    .createdAt(order.getCreatedAt())
                    .eventType("ORDER_" + status.name())
                    .build();

            orderEventPublisher.publishOrderEvent(orderEvent);
        } catch (Exception e) {
            log.error("Failed to publish order event for orderId {}: {}",
                    order.getOrderId(), e.getMessage());
        }
    }
}
