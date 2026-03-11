package swp.be.vn.bs.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletRequest {
    private BigDecimal amount;
    private String bankAccount;
}
