package swp.be.vn.bs.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PostRequest {
    private Integer bicycleId;
    private String title;
    private String description;
    private BigDecimal price;
}
