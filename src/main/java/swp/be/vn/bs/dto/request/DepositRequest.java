package swp.be.vn.bs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    @NotNull(message = "Post ID is required")
    private Integer postId;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;
}
