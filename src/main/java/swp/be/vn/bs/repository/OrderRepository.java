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

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);

    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.buyer.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.post.seller.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CAST(o.orderId AS string) LIKE CONCAT('%', :keyword, '%')")
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
}
