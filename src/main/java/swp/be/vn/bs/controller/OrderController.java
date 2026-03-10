package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.DeliveryScheduleRequest;
import swp.be.vn.bs.dto.response.DeliverySessionResponse;
import swp.be.vn.bs.dto.response.OrderResponse;
import swp.be.vn.bs.service.DeliverySessionService;
import swp.be.vn.bs.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private DeliverySessionService deliverySessionService;
    
    /**
     * POST /api/orders/deposit
     * Đặt cọc 20% cho một post
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(
            @RequestParam Integer postId,
            Authentication authentication) {
        try {
            String buyerEmail = authentication.getName();
            OrderResponse response = orderService.createDeposit(postId, buyerEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating deposit: " + e.getMessage());
        }
    }
    
    /**
     * DELETE /api/orders/{orderId}/cancel
     * Hủy đặt cọc - hoàn tiền
     */
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelDeposit(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String buyerEmail = authentication.getName();
            orderService.cancelDeposit(orderId, buyerEmail);
            return ResponseEntity.ok("Deposit cancelled and refunded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error cancelling deposit: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/orders/my-orders
     * Lấy danh sách đơn mua của buyer
     */
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        try {
            String buyerEmail = authentication.getName();
            List<OrderResponse> orders = orderService.getMyOrders(buyerEmail);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching orders: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/orders/my-sales
     * Lấy danh sách đơn bán của seller
     */
    @GetMapping("/my-sales")
    public ResponseEntity<?> getMySales(Authentication authentication) {
        try {
            String sellerEmail = authentication.getName();
            List<OrderResponse> sales = orderService.getMySales(sellerEmail);
            return ResponseEntity.ok(sales);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching sales: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/orders/{orderId}
     * Lấy chi tiết order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer orderId) {
        try {
            OrderResponse order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error fetching order: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/orders/{orderId}/schedule-delivery
     * Lên lịch giao hàng
     */
    @PutMapping("/{orderId}/schedule-delivery")
    public ResponseEntity<?> scheduleDelivery(
            @PathVariable Integer orderId,
            @RequestBody DeliveryScheduleRequest request,
            Authentication authentication) {
        try {
            String sellerEmail = authentication.getName();
            DeliverySessionResponse delivery = deliverySessionService.scheduleDelivery(
                orderId, 
                request.getDeliveryTime(), 
                request.getDeliveryAddress(),
                sellerEmail
            );
            return ResponseEntity.ok(delivery);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error scheduling delivery: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/orders/{orderId}/complete
     * Hoàn tất đơn hàng - buyer trả 80% còn lại
     */
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            // TODO: Add role check hoặc verify buyer/seller
            OrderResponse response = orderService.completeOrder(orderId, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error completing order: " + e.getMessage());
        }
    }
}
