package swp.be.vn.bs.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class BicycleRespond {
    private Integer bicycleId;
    private String brand;
    private String frameMaterial;
    private String frameSize;
    private String groupset;
    private String wheelSize;
    private Integer conditionPercent;
    private String categoryName;
}
