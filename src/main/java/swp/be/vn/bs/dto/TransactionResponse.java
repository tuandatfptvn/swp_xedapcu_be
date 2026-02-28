package swp.be.vn.bs.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Integer transactionId;
    private BigDecimal amount;
    private String transactionType;
    private String status;
    private LocalDateTime createdAt;
    private String bankAccount;

}