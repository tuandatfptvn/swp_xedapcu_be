package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer reportId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false, unique = true)
    private InspectionRequest inspection;
    
    @Column(name = "frame_status", length = 1000)
    private String frameStatus;
    
    @Column(name = "brake_status", length = 1000)
    private String brakeStatus;
    
    @Column(name = "drivetrain_status", length = 1000)
    private String drivetrainStatus;
    
    @Column(name = "overall_rating")
    private Integer overallRating;
    
    @Column(name = "report_file_url", length = 500)
    private String reportFileUrl;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @PrePersist
    protected void onCreate() {
        verifiedAt = LocalDateTime.now();
    }
}
