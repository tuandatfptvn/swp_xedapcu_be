package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.BookingStatus;
import swp.be.vn.bs.entity.InspectionBooking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionBookingRepository extends JpaRepository<InspectionBooking, Integer> {
    
    /**
     * Tìm booking theo post
     */
    List<InspectionBooking> findByPost_PostId(Integer postId);
    
    /**
     * Tìm booking theo inspector
     */
    List<InspectionBooking> findByInspector_UserId(Integer inspectorId);
    
    /**
     * Tìm booking theo trạng thái
     */
    List<InspectionBooking> findByStatus(BookingStatus status);
    
    /**
     * Tìm booking pending (chưa assign inspector)
     */
    List<InspectionBooking> findByStatusAndInspectorIsNull(BookingStatus status);
    
    /**
     * Tìm booking theo ngày
     */
    List<InspectionBooking> findByBookingDate(LocalDate date);
    
    /**
     * Tìm booking của inspector trong ngày cụ thể
     */
    List<InspectionBooking> findByInspector_UserIdAndBookingDate(Integer inspectorId, LocalDate date);
    
    /**
     * Check xem post đã có booking chưa
     */
    boolean existsByPost_PostIdAndStatusIn(Integer postId, List<BookingStatus> statuses);
    
    /**
     * Tìm booking active của post (PENDING hoặc CONFIRMED)
     */
    Optional<InspectionBooking> findByPost_PostIdAndStatusIn(Integer postId, List<BookingStatus> statuses);
}
