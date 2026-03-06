package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionReportResponse {
    
    private Integer reportId;
    private Integer inspectionId;
    private Integer postId;
    private String postTitle;
    
    // Assessment details
    private String frameStatus;
    private String brakeStatus;
    private String drivetrainStatus;
    private Integer overallRating;
    
    // Report file
    private String reportFileUrl;
    
    // Timestamps
    private LocalDateTime verifiedAt;
    
    // Inspector info
    private InspectorInfo inspector;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectorInfo {
        private Integer userId;
        private String email;
        private String fullName;
    }
}
