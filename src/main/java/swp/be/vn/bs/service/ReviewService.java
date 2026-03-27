package swp.be.vn.bs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.request.CreateReviewRequest;
import swp.be.vn.bs.dto.response.ReviewResponse;
import swp.be.vn.bs.dto.response.SellerRatingResponse;
import swp.be.vn.bs.entity.Order;
import swp.be.vn.bs.entity.OrderStatus;
import swp.be.vn.bs.entity.Review;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.OrderRepository;
import swp.be.vn.bs.repository.ReviewRepository;
import swp.be.vn.bs.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    
    // Validation constants
    private static final long REVIEW_WAIT_TIME_MINUTES = 0; // No wait time - can review immediately
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    
    /**
     * Create a new review for an order
     * Buyer can only review after order is COMPLETED
     * At least rating OR comment must be provided
     */
    public ReviewResponse createReview(Integer orderId, Integer buyerId, CreateReviewRequest request) {
        // Validate order exists and is completed
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getStatus().equals(OrderStatus.COMPLETED)) {
            throw new RuntimeException("Order must be COMPLETED to leave review");
        }
        
        // Validate 1-hour wait time
        LocalDateTime completedTime = order.getCreatedAt();
        LocalDateTime reviewEligibleTime = completedTime.plusMinutes(REVIEW_WAIT_TIME_MINUTES);
        if (LocalDateTime.now().isBefore(reviewEligibleTime)) {
            throw new RuntimeException("Review can only be written after order completion");
        }
        
        if (!order.getBuyer().getUserId().equals(buyerId)) {
            throw new RuntimeException("Only buyer can review this order");
        }
        
        // Check if already reviewed
        Optional<Review> existingReview = reviewRepository.findByOrderOrderIdAndFromUserId(orderId, buyerId);
        if (existingReview.isPresent()) {
            throw new RuntimeException("You already reviewed this order");
        }
        
        // Validate at least rating OR comment provided
        if ((request.getRating() == null || request.getRating() == 0) && 
            (request.getComment() == null || request.getComment().trim().isEmpty())) {
            throw new RuntimeException("At least rating or comment must be provided");
        }
        
        // Validate rating if provided
        if (request.getRating() != null && request.getRating() != 0) {
            if (request.getRating() < MIN_RATING || request.getRating() > MAX_RATING) {
                throw new RuntimeException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);
            }
        }
        
        // Get users
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        User seller = order.getPost().getSeller();
        
        // Create review
        Review review = new Review();
        review.setOrder(order);
        review.setFromUser(buyer);
        review.setToUser(seller);
        review.setRating(request.getRating() != null && request.getRating() > 0 ? request.getRating() : null);
        review.setComment(request.getComment() != null && !request.getComment().trim().isEmpty() 
                ? request.getComment().trim() 
                : null);
        review.setCreatedAt(LocalDateTime.now());
        
        Review savedReview = reviewRepository.save(review);
        return convertToResponse(savedReview);
    }
    
    /**
     * Get all reviews for a seller with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getSellerReviews(Integer sellerId, Pageable pageable) {
        // Verify seller exists
        userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        return reviewRepository.findAllSellerReviews(sellerId, pageable)
                .map(this::convertToResponse);
    }
    
    /**
     * Get seller rating summary (average rating + review count)
     */
    @Transactional(readOnly = true)
    public SellerRatingResponse getSellerRatingSummary(Integer sellerId) {
        // Verify seller exists
        userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        Double averageRating = reviewRepository.getAverageRatingBySellerId(sellerId);
        Long reviewCount = reviewRepository.countRatingsBySellerId(sellerId);
        
        SellerRatingResponse response = new SellerRatingResponse();
        response.setSellerId(sellerId);
        response.setAverageRating(averageRating != null ? averageRating : 0.0);
        response.setTotalRatings(reviewCount != null ? reviewCount : 0L);
        
        return response;
    }
    
    /**
     * Get a single review by ID
     */
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        return convertToResponse(review);
    }
    
    /**
     * Delete a review (only by creator or admin)
     */
    public void deleteReview(Integer reviewId, Integer userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        // Only buyer who created the review can delete it
        if (!review.getFromUser().getUserId().equals(userId)) {
            throw new RuntimeException("Only review creator can delete it");
        }
        
        reviewRepository.deleteById(reviewId);
    }
    
    /**
     * Convert Review entity to ReviewResponse DTO
     */
    private ReviewResponse convertToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setReviewId(review.getReviewId());
        response.setOrderId(review.getOrder().getOrderId());
        response.setFromUserId(review.getFromUser().getUserId());
        response.setFromUserName(review.getFromUser().getFullName());
        response.setToUserId(review.getToUser().getUserId());
        response.setToUserName(review.getToUser().getFullName());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
