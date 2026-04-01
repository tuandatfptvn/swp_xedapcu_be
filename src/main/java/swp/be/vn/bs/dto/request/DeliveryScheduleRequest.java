package swp.be.vn.bs.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeliveryScheduleRequest {
    private LocalDateTime deliveryTime;
    private String pickupAddress;      // ✅ Seller's pickup address
    private String deliveryAddress;    // Buyer's delivery address
}
