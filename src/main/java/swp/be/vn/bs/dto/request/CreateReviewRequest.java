package swp.be.vn.bs.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {
    private Integer rating;  // 1-5, optional
    private String comment;  // text, optional
    
    // At least one of rating or comment must be provided
}
