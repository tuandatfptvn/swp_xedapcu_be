package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.InspectionReport;

import java.util.Optional;

@Repository
public interface InspectionReportRepository extends JpaRepository<InspectionReport, Integer> {
    
    /**
     * Tìm report theo inspection request
     */
    Optional<InspectionReport> findByInspection_InspectionId(Integer inspectionId);
    
    /**
     * Check xem inspection đã có report chưa
     */
    boolean existsByInspection_InspectionId(Integer inspectionId);
    
    /**
     * ✅ FIX BUG: Tìm report theo booking ID
     * Admin xem report phải dùng bookingId, không phải inspectionId
     * Query path: InspectionReport.inspection.booking.bookingId
     */
    @Query("SELECT ir FROM InspectionReport ir " +
           "WHERE ir.inspection.booking.bookingId = :bookingId")
    Optional<InspectionReport> findByInspection_Booking_BookingId(@Param("bookingId") Integer bookingId);
}
