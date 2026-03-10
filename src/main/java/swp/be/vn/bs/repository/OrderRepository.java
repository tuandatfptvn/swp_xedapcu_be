package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    List<Order> findByBuyer_UserId(Integer buyerId);
    
    List<Order> findByBuyer_Email(String buyerEmail);
    
    List<Order> findByPost_Seller_UserId(Integer sellerId);
    
    List<Order> findByPost_Seller_Email(String sellerEmail);
    
    Optional<Order> findByPost_PostId(Integer postId);
}
