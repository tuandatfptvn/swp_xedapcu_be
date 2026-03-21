package swp.be.vn.bs.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerConfirmDeliveryRequest {
    private String deliveryAddress; // Optional: Seller can update address
}
