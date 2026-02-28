package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.DeliveryStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySessionResponse {
    private Integer sessionId;
    private Integer orderId;
    private String deliveryAddress;
    private LocalDateTime deliveryTime;
    private DeliveryStatus status;
    private String trackingNumber;
}
