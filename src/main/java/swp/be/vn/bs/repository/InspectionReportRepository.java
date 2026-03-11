package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
