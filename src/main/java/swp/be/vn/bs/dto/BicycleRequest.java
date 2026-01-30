package swp.be.vn.bs.dto;

import lombok.Data;

@Data
public class BicycleRequest {
    private String brand;
    private String frameMaterial;
    private String frameSize;
    private String groupset;
    private String wheelSize;
    private Integer conditionPercent;
    private Integer categoryId;
}
