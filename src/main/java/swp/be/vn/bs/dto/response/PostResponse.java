package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.PostStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Integer postId;
    private String title;
    private String description;
    private BigDecimal price;
    private PostStatus status;
    private Boolean isInspected;
    private BigDecimal postFee;
    private LocalDateTime createdAt;
    
    private SellerInfo seller;
    private BicycleInfo bicycle;
    
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
    public static class BicycleInfo {
        private Integer bicycleId;
        private String brand;
        private String frameSize;
        private String groupset;
        private Integer conditionPercent;
    }
}
