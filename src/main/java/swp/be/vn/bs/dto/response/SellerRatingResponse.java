package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerRatingResponse {
    private Integer sellerId;
    private Double averageRating;    // 0-5.0
    private Long totalRatings;       // count of reviews with rating
}
