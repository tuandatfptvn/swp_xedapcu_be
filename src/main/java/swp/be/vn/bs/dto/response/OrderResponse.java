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
    
    private BuyerInfo buyer;
    private SellerInfo seller;
    private PostInfo post;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuyerInfo {
        private Integer userId;
        private String email;
        private String fullName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
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
}
