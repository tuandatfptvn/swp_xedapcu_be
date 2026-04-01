package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Integer orderId;
    private Integer postId;
    private String postTitle;
    private BigDecimal depositAmount;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String pickupAddress;      // ✅ Seller's pickup address
    private String deliveryAddress;
    private LocalDateTime sellerConfirmedAt;
    private LocalDateTime adminReviewedAt;
    
    private BuyerInfo buyer;
    private SellerInfo seller;
    private InspectorInfo assignedInspector;
    private PostInfo post;
    private DeliverySessionInfo deliverySession;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuyerInfo {
        private Integer userId;
        private String email;
        private String fullName;
        private String phone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
        private Integer userId;
        private String email;
        private String fullName;
        private String phone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectorInfo {
        private Integer userId;
        private String email;
        private String fullName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInfo {
        private Integer postId;
        private String title;
        private BigDecimal price;
        private BigDecimal postFee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliverySessionInfo {
        private Integer sessionId;
        private LocalDateTime deliveryDate;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String location;
        private String deliveryStatus;
        private LocalDateTime assignedAt;
        private LocalDateTime deliveredAt;
    }
}

