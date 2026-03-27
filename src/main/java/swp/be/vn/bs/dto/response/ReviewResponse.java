package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Integer reviewId;
    private Integer orderId;
    private Integer fromUserId;      // Buyer
    private String fromUserName;      // Buyer name
    private Integer toUserId;         // Seller
    private String toUserName;        // Seller name
    private Integer rating;           // 1-5, nullable
    private String comment;           // text, nullable
    private LocalDateTime createdAt;
}
