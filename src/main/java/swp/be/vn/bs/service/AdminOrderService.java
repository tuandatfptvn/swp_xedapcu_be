package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.dto.response.OrderResponse;
import swp.be.vn.bs.entity.Order;
import swp.be.vn.bs.entity.OrderStatus;
import swp.be.vn.bs.entity.Role;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.OrderRepository;
import swp.be.vn.bs.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    /**
     * Admin lấy danh sách orders giao hàng cần xử lý
     * Bao gồm: PENDING_SELLER_CONFIRMATION, PENDING_ADMIN_REVIEW, ASSIGNED_TO_INSPECTOR, IN_DELIVERY
     */
    public List<OrderResponse> getDeliveryOrders(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminEmail));

        // Check admin role
        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("User is not an admin");
        }

        // OPTIMIZED: Use custom query instead of findAll().stream().filter()
        List<Order> orders = orderRepository.findDeliveryOrders();

        return orders.stream()
                .map(orderService::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Admin lấy danh sách orders theo status cụ thể
     */
    public List<OrderResponse> getDeliveryOrdersByStatus(String adminEmail, String statusStr) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminEmail));

        // Check admin role
        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("User is not an admin");
        }

        // Parse status
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + statusStr);
        }

        // Validate status is in delivery workflow
        if (status != OrderStatus.PENDING_SELLER_CONFIRMATION &&
            status != OrderStatus.PENDING_ADMIN_REVIEW &&
            status != OrderStatus.ASSIGNED_TO_INSPECTOR &&
            status != OrderStatus.IN_DELIVERY) {
            throw new RuntimeException("Status " + statusStr + " is not in delivery workflow");
        }

        // OPTIMIZED: Use custom query instead of findAll().stream().filter()
        List<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);

        return orders.stream()
                .map(orderService::mapToResponse)
                .collect(Collectors.toList());
    }
}
