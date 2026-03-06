package swp.be.vn.bs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.BookingStatus;
import swp.be.vn.bs.entity.PaidBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionBookingResponse {
    
    private Integer bookingId;
    private Integer postId;
    private String postTitle;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private BookingStatus status;
    private PaidBy paidBy;
    private LocalDateTime createdAt;
    
    // Requester info (người yêu cầu kiểm định)
    private RequesterInfo requester;
    
    // Inspector info (người kiểm định)
    private InspectorInfo inspector;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequesterInfo {
        private Integer userId;
        private String email;
        private String fullName;
    }
    
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
