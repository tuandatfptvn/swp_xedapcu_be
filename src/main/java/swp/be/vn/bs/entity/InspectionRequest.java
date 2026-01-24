package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_id")
    private Integer inspectionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private InspectionBooking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;
    
    @Column(name = "inspection_fee", precision = 18, scale = 2)
    private BigDecimal inspectionFee;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", length = 50)
    private PaidBy paidBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private InspectionStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = InspectionStatus.PENDING;
        }
    }
}
