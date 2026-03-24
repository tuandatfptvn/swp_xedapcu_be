package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImageResponse {
    private Integer imageId;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean isThumbnail;
}
