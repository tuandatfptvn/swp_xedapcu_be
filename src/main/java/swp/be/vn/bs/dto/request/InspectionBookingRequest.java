package swp.be.vn.bs.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.PaidBy;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO cho việc request kiểm định xe
 * Buyer/Seller yêu cầu kiểm tra xe trước khi giao dịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionBookingRequest {
    
    /**
     * ID của post cần kiểm định
     */
    private Integer postId;
    
    /**
     * Ngày muốn kiểm định
     */
    private LocalDate bookingDate;
    
    /**
     * Giờ bắt đầu
     */
    private LocalTime startTime;
    
    /**
     * Giờ kết thúc
     */
    private LocalTime endTime;
    
    /**
     * Địa điểm kiểm định
     */
    private String location;
    
    /**
     * Ai trả phí kiểm định (BUYER hoặc SELLER)
     */
    private PaidBy paidBy;
}
