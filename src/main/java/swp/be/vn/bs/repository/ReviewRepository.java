package swp.be.vn.bs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Review;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    // Check if buyer already reviewed this order
    @Query("SELECT r FROM Review r WHERE r.order.orderId = :orderId AND r.fromUser.userId = :fromUserId")
    Optional<Review> findByOrderOrderIdAndFromUserId(@Param("orderId") Integer orderId, @Param("fromUserId") Integer fromUserId);
    
    // Get all reviews for a seller (to_user)
    @Query("SELECT r FROM Review r WHERE r.toUser.userId = :sellerId ORDER BY r.createdAt DESC")
    Page<Review> findByToUserIdOrderByCreatedAtDesc(@Param("sellerId") Integer sellerId, Pageable pageable);
    
    // Get all reviews from a buyer (from_user)
    @Query("SELECT r FROM Review r WHERE r.fromUser.userId = :buyerId ORDER BY r.createdAt DESC")
    Page<Review> findByFromUserIdOrderByCreatedAtDesc(@Param("buyerId") Integer buyerId, Pageable pageable);
    
    // Get reviews with rating >= threshold
    @Query("SELECT r FROM Review r WHERE r.toUser.userId = :sellerId AND r.rating >= :minRating ORDER BY r.createdAt DESC")
    Page<Review> findSellerReviewsByMinRating(@Param("sellerId") Integer sellerId, 
                                              @Param("minRating") Integer minRating, 
                                              Pageable pageable);
    
    // Calculate average rating for seller
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.toUser.userId = :sellerId AND r.rating IS NOT NULL")
    Double getAverageRatingBySellerId(@Param("sellerId") Integer sellerId);
    
    // Count reviews for seller (only reviews with rating)
    @Query("SELECT COUNT(r) FROM Review r WHERE r.toUser.userId = :sellerId AND r.rating IS NOT NULL")
    Long countRatingsBySellerId(@Param("sellerId") Integer sellerId);
    
    // Get all reviews for a seller (paginated)
    @Query("SELECT r FROM Review r WHERE r.toUser.userId = :sellerId ORDER BY r.createdAt DESC")
    Page<Review> findAllSellerReviews(@Param("sellerId") Integer sellerId, Pageable pageable);
    
    // Check if order can be reviewed (order must exist and belong to buyer)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.order.orderId = :orderId AND r.fromUser.userId = :buyerId")
    boolean existsReviewForOrder(@Param("orderId") Integer orderId, @Param("buyerId") Integer buyerId);
    
    // ==================== OPTIMIZED QUERIES WITH EAGER LOAD ====================
    
    /**
     * Get all reviews for a seller with eager loading (avoid N+1)
     */
    @Query("SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.fromUser " +
           "LEFT JOIN FETCH r.toUser " +
           "LEFT JOIN FETCH r.order " +
           "WHERE r.toUser.userId = :sellerId " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findAllSellerReviewsWithEagerLoad(@Param("sellerId") Integer sellerId, Pageable pageable);
}
