package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "location", length = 500)
    private String location;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private DeliveryStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = DeliveryStatus.SCHEDULED;
        }
    }
}
