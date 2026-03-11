package swp.be.vn.bs.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho việc Inspector submit báo cáo kiểm định
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionReportRequest {
    
    /**
     * Tình trạng khung xe
     */
    private String frameStatus;
    
    /**
     * Tình trạng phanh
     */
    private String brakeStatus;
    
    /**
     * Tình trạng bộ truyền động
     */
    private String drivetrainStatus;
    
    /**
     * Đánh giá tổng thể (1-10)
     */
    private Integer overallRating;
    
    /**
     * URL file báo cáo PDF (optional)
     */
    private String reportFileUrl;
    
    /**
     * Ghi chú thêm
     */
    private String notes;
}
