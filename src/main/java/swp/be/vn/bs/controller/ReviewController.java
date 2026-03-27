package swp.be.vn.bs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.config.JwtTokenProvider;
import swp.be.vn.bs.dto.request.CreateReviewRequest;
import swp.be.vn.bs.dto.response.ReviewResponse;
import swp.be.vn.bs.dto.response.SellerRatingResponse;
import swp.be.vn.bs.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * POST /api/reviews/{orderId}
     * Create a new review for an order
     * Only buyer who completed the order can review
     * Review can be written immediately after order completion
     */
    @PostMapping("/{orderId}")
    public ResponseEntity<?> createReview(
            @PathVariable Integer orderId,
            @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {
        try {
            Integer buyerId = getCurrentUserId(httpRequest);
            ReviewResponse response = reviewService.createReview(orderId, buyerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * GET /api/reviews/seller/{sellerId}
     * Get all reviews for a seller with pagination
     * Query params: page (default: 0), size (default: 10)
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerReviews(
            @PathVariable Integer sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviews = reviewService.getSellerReviews(sellerId, pageable);
            return ResponseEntity.ok(reviews);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * GET /api/reviews/seller/{sellerId}/rating
     * Get seller rating summary (average + count)
     */
    @GetMapping("/seller/{sellerId}/rating")
    public ResponseEntity<?> getSellerRating(@PathVariable Integer sellerId) {
        try {
            SellerRatingResponse response = reviewService.getSellerRatingSummary(sellerId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * GET /api/reviews/{reviewId}
     * Get a single review by ID
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReview(@PathVariable Integer reviewId) {
        try {
            ReviewResponse response = reviewService.getReviewById(reviewId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * DELETE /api/reviews/{reviewId}
     * Delete a review (only by creator)
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Integer reviewId,
            HttpServletRequest httpRequest) {
        try {
            Integer userId = getCurrentUserId(httpRequest);
            reviewService.deleteReview(reviewId, userId);
            return ResponseEntity.ok("Review deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Helper method to extract user ID from JWT token
     */
    private Integer getCurrentUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new RuntimeException("Unauthorized");
        }
        return jwtTokenProvider.getUserIdFromToken(token);
    }
    
    /**
     * Helper method to extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
