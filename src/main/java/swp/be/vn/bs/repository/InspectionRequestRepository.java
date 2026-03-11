package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.InspectionRequest;
import swp.be.vn.bs.entity.InspectionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionRequestRepository extends JpaRepository<InspectionRequest, Integer> {
    
    /**
     * Tìm inspection request theo booking
     */
    Optional<InspectionRequest> findByBooking_BookingId(Integer bookingId);
    
    /**
     * Tìm inspection request theo post
     */
    List<InspectionRequest> findByPost_PostId(Integer postId);
    
    /**
     * Tìm inspection request theo inspector
     */
    List<InspectionRequest> findByInspector_UserId(Integer inspectorId);
    
    /**
     * Tìm inspection request theo trạng thái
     */
    List<InspectionRequest> findByStatus(InspectionStatus status);
    
    /**
     * Check xem post đã có inspection request completed chưa
     */
    boolean existsByPost_PostIdAndStatus(Integer postId, InspectionStatus status);
    
    /**
     * Tìm completed inspection của post
     */
    Optional<InspectionRequest> findByPost_PostIdAndStatus(Integer postId, InspectionStatus status);
}
