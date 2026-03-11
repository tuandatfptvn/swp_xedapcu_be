package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.InspectionStatus;
import swp.be.vn.bs.entity.PaidBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRequestResponse {
    
    private Integer inspectionId;
    private Integer bookingId;
    private Integer postId;
    private String postTitle;
    private BigDecimal inspectionFee;
    private PaidBy paidBy;
    private InspectionStatus status;
    private LocalDateTime createdAt;
    
    // Inspector info
    private InspectorInfo inspector;
    
    // Report info (nếu đã submit)
    private ReportInfo report;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectorInfo {
        private Integer userId;
        private String email;
        private String fullName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportInfo {
        private Integer reportId;
        private String frameStatus;
        private String brakeStatus;
        private String drivetrainStatus;
        private Integer overallRating;
        private String reportFileUrl;
        private LocalDateTime verifiedAt;
    }
}
