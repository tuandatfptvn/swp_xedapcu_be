package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_gateway_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gateway_tx_id")
    private Integer gatewayTxId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "gateway_provider", length = 50)
    private String gatewayProvider; // VNPAY, MOMO, etc.
    
    @Column(name = "external_tx_id", length = 200)
    private String externalTxId; // ID từ VNPay/Momo
    
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 10)
    private String currency;
    
    @Column(name = "status", length = 50)
    private String status; // PENDING, SUCCESS, FAILED, CANCELLED
    
    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;
    
    @Column(name = "callback_data", columnDefinition = "TEXT")
    private String callbackData; // JSON response từ gateway
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currency == null) {
            currency = "VND";
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
