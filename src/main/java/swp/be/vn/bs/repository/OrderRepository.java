package swp.be.vn.bs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Order;
import swp.be.vn.bs.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    List<Order> findByBuyer_UserId(Integer buyerId);
    
    List<Order> findByBuyer_Email(String buyerEmail);
    
    List<Order> findByPost_Seller_UserId(Integer sellerId);
    
    List<Order> findByPost_Seller_Email(String sellerEmail);
    
    Optional<Order> findByPost_PostId(Integer postId);
    
    // Tìm ACTIVE order cho post này (chỉ có 1 active order per post)
    Optional<Order> findByPost_PostIdAndIsActiveTrue(Integer postId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);

    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.buyer.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.post.seller.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CAST(o.orderId AS string) LIKE CONCAT('%', :keyword, '%')")
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
    
    // ==================== OPTIMIZED QUERIES ====================
    
    /**
     * Lấy orders của inspector (delivery tasks)
     * Bao gồm: ASSIGNED_TO_INSPECTOR, IN_DELIVERY, COMPLETED
     */
    @Query("SELECT o FROM Order o WHERE o.assignedInspector.userId = :inspectorId " +
           "AND (o.status = 'ASSIGNED_TO_INSPECTOR' OR o.status = 'IN_DELIVERY' OR o.status = 'COMPLETED') " +
           "ORDER BY o.createdAt DESC")
    List<Order> findDeliveryTasksByInspector(@Param("inspectorId") Integer inspectorId);
    
    /**
     * Lấy orders theo delivery workflow statuses
     * Bao gồm: PENDING_SELLER_CONFIRMATION, PENDING_ADMIN_REVIEW, ASSIGNED_TO_INSPECTOR, IN_DELIVERY
     */
    @Query("SELECT o FROM Order o WHERE " +
           "o.status = 'PENDING_SELLER_CONFIRMATION' OR " +
           "o.status = 'PENDING_ADMIN_REVIEW' OR " +
           "o.status = 'ASSIGNED_TO_INSPECTOR' OR " +
           "o.status = 'IN_DELIVERY' " +
           "ORDER BY o.createdAt DESC")
    List<Order> findDeliveryOrders();
    
    /**
     * Lấy orders theo status cụ thể, ordered by newest first
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStatusOrderByCreatedAtDesc(@Param("status") OrderStatus status);
}
