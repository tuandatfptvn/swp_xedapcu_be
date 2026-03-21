package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.response.OrderResponse;
import swp.be.vn.bs.service.AdminOrderService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @Autowired
    private AdminOrderService adminOrderService;

    /**
     * GET /api/admin/orders/delivery
     * Admin lấy danh sách orders giao hàng cần xử lý
     * Bao gồm: PENDING_SELLER_CONFIRMATION, PENDING_ADMIN_REVIEW, ASSIGNED_TO_INSPECTOR, IN_DELIVERY
     */
    @GetMapping("/delivery")
    public ResponseEntity<?> getDeliveryOrders(Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            List<OrderResponse> orders = adminOrderService.getDeliveryOrders(adminEmail);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching delivery orders: " + e.getMessage());
        }
    }

    /**
     * GET /api/admin/orders/delivery?status=PENDING_ADMIN_REVIEW
     * Admin lấy danh sách orders theo status cụ thể
     */
    @GetMapping("/delivery/by-status")
    public ResponseEntity<?> getDeliveryOrdersByStatus(
            @RequestParam String status,
            Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            List<OrderResponse> orders = adminOrderService.getDeliveryOrdersByStatus(adminEmail, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching delivery orders: " + e.getMessage());
        }
    }
}
