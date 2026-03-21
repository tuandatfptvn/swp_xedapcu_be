package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.DepositRequest;
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
     * Đặt cọc 20% cho một post với địa chỉ giao hàng
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(
            @RequestBody DepositRequest request,
            Authentication authentication) {
        try {
            String buyerEmail = authentication.getName();
            OrderResponse response = orderService.createDeposit(
                    request.getPostId(), 
                    buyerEmail, 
                    request.getDeliveryAddress());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating deposit: " + e.getMessage());
        }
    }
    
    /**
     * DELETE /api/orders/{orderId}/cancel
     * Buyer hủy đặt cọc - mất cọc (transfer cho seller)
     */
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelDeposit(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String buyerEmail = authentication.getName();
            orderService.cancelDeposit(orderId, buyerEmail);
            return ResponseEntity.ok("Order cancelled. Deposit forfeited and transferred to seller.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error cancelling deposit: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/orders/{orderId}/seller-cancel
     * Seller hủy đơn - hoàn cọc cho buyer + ghi nhận violation cho seller
     */
    @DeleteMapping("/{orderId}/seller-cancel")
    public ResponseEntity<?> cancelBySeller(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String sellerEmail = authentication.getName();
            orderService.cancelOrderBySeller(orderId, sellerEmail);
            return ResponseEntity.ok("Order cancelled by seller. Deposit refunded and violation recorded.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error cancelling order by seller: " + e.getMessage());
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

    /**
     * POST /api/orders/{orderId}/report-buyer-no-show
     * Seller báo cáo Buyer không đến: Buyer mất cọc, Seller được bồi thường
     */
    @PostMapping("/{orderId}/report-buyer-no-show")
    public ResponseEntity<?> reportBuyerNoShow(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String sellerEmail = authentication.getName();
            orderService.reportBuyerNoShow(orderId, sellerEmail);
            return ResponseEntity.ok("Successfully reported buyer no-show. Deposit transferred and violation recorded.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error reporting buyer no-show: " + e.getMessage());
        }
    }

    /**
     * POST /api/orders/{orderId}/report-seller-no-show
     * Buyer báo cáo Seller không đến: Buyer được hoàn cọc, Seller bị phạt Violation
     */
    @PostMapping("/{orderId}/report-seller-no-show")
    public ResponseEntity<?> reportSellerNoShow(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String buyerEmail = authentication.getName();
            orderService.reportSellerNoShow(orderId, buyerEmail);
            return ResponseEntity.ok("Successfully reported seller no-show. Deposit refunded and violation recorded.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error reporting seller no-show: " + e.getMessage());
        }
    }

    /**
     * PUT /api/orders/{orderId}/seller-confirm-delivery
     * Seller confirm delivery address and proceed
     */
    @PutMapping("/{orderId}/seller-confirm-delivery")
    public ResponseEntity<?> sellerConfirmDelivery(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String sellerEmail = authentication.getName();
            OrderResponse response = orderService.sellerConfirmDelivery(orderId, sellerEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error confirming delivery: " + e.getMessage());
        }
    }

    /**
     * POST /api/orders/{orderId}/admin-assign-inspector
     * Admin assign inspector to delivery
     */
    @PostMapping("/{orderId}/admin-assign-inspector/{inspectorId}")
    public ResponseEntity<?> adminAssignInspector(
            @PathVariable Integer orderId,
            @PathVariable Integer inspectorId,
            Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            OrderResponse response = orderService.adminAssignInspector(orderId, inspectorId, adminEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error assigning inspector: " + e.getMessage());
        }
    }

    /**
     * POST /api/orders/{orderId}/inspector-mark-delivered
     * Inspector mark delivery as completed
     */
    @PostMapping("/{orderId}/inspector-mark-delivered")
    public ResponseEntity<?> inspectorMarkDelivered(
            @PathVariable Integer orderId,
            Authentication authentication) {
        try {
            String inspectorEmail = authentication.getName();
            OrderResponse response = orderService.inspectorMarkDelivered(orderId, inspectorEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error marking delivery as completed: " + e.getMessage());
        }
    }
}
