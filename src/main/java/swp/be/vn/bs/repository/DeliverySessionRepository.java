package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.DeliverySession;
import swp.be.vn.bs.entity.DeliveryStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliverySessionRepository extends JpaRepository<DeliverySession, Integer> {
    
    Optional<DeliverySession> findByOrder_OrderId(Integer orderId);
    
    List<DeliverySession> findByStatus(DeliveryStatus status);
}
