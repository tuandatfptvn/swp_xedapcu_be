package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.response.DeliverySessionResponse;
import swp.be.vn.bs.entity.*;
import swp.be.vn.bs.repository.DeliverySessionRepository;
import swp.be.vn.bs.repository.OrderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class DeliverySessionService {
    
    @Autowired
    private DeliverySessionRepository deliverySessionRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    /**
     * Lên lịch giao hàng cho order
     */
    @Transactional
    public DeliverySessionResponse scheduleDelivery(
            Integer orderId, 
            LocalDateTime deliveryTime,
            String deliveryAddress,
            String sellerEmail) {
        
        // 1. Validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // 2. Validate seller ownership
        if (!order.getPost().getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("Only the seller can schedule delivery for this order");
        }
        
        // 3. Check order status - phải đã deposit
        if (order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new RuntimeException("Cannot schedule delivery. Order status must be DEPOSIT_PAID. Current: " + order.getStatus());
        }
        
        // 4. Check xem đã có delivery session chưa
        if (deliverySessionRepository.findByOrder_OrderId(orderId).isPresent()) {
            throw new RuntimeException("This order already has a scheduled delivery session");
        }
        
        // 5. Validate delivery time (phải trong tương lai)
        if (deliveryTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Delivery time must be in the future");
        }
        
        // 6. Tạo DeliverySession (entity có deliveryDate, startTime, endTime, location)
        DeliverySession delivery = new DeliverySession();
        delivery.setOrder(order);
        delivery.setDeliveryDate(deliveryTime.toLocalDate());
        delivery.setStartTime(deliveryTime.toLocalTime());
        delivery.setEndTime(deliveryTime.plusHours(2).toLocalTime()); // Default 2 hours window
        delivery.setLocation(deliveryAddress);
        delivery.setStatus(DeliveryStatus.SCHEDULED);
        
        DeliverySession saved = deliverySessionRepository.save(delivery);
        
        // 7. Update order status
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);
        
        return mapToResponse(saved);
    }
    
    /**
     * Lấy delivery session theo orderId
     */
    public DeliverySessionResponse getDeliveryByOrderId(Integer orderId) {
        DeliverySession delivery = deliverySessionRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No delivery session found for order ID: " + orderId));
        return mapToResponse(delivery);
    }
    
    /**
     * Update delivery status
     */
    @Transactional
    public DeliverySessionResponse updateDeliveryStatus(Integer sessionId, DeliveryStatus newStatus) {
        DeliverySession delivery = deliverySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Delivery session not found with ID: " + sessionId));
        
        delivery.setStatus(newStatus);
        
        if (newStatus == DeliveryStatus.COMPLETED) {
            delivery.setVerifiedAt(LocalDateTime.now());
        }
        
        DeliverySession updated = deliverySessionRepository.save(delivery);
        return mapToResponse(updated);
    }
    
    /**
     * Map DeliverySession entity to DeliverySessionResponse DTO
     */
    private DeliverySessionResponse mapToResponse(DeliverySession delivery) {
        // Combine deliveryDate + startTime thành deliveryTime
        LocalDateTime deliveryTime = null;
        if (delivery.getDeliveryDate() != null && delivery.getStartTime() != null) {
            deliveryTime = LocalDateTime.of(delivery.getDeliveryDate(), delivery.getStartTime());
        }
        
        return DeliverySessionResponse.builder()
                .sessionId(delivery.getSessionId())
                .orderId(delivery.getOrder().getOrderId())
                .deliveryAddress(delivery.getLocation())
                .deliveryTime(deliveryTime)
                .status(delivery.getStatus())
                .trackingNumber("TRK-" + delivery.getSessionId()) // Generate từ sessionId
                .build();
    }
}
