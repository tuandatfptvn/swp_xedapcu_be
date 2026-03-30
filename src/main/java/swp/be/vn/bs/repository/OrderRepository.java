package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    List<Order> findByBuyer_UserId(Integer buyerId);
    
    List<Order> findByBuyer_Email(String buyerEmail);
    
    List<Order> findByPost_Seller_UserId(Integer sellerId);
    
    List<Order> findByPost_Seller_Email(String sellerEmail);
    
    Optional<Order> findByPost_PostId(Integer postId);

    @Query("SELECT o FROM Order o WHERE CAST(o.orderId AS string) LIKE %:keyword% OR LOWER(o.buyer.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
}
