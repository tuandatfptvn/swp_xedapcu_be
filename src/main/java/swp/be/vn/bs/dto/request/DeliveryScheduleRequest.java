package swp.be.vn.bs.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeliveryScheduleRequest {
    private LocalDateTime deliveryTime;
    private String deliveryAddress;
}
