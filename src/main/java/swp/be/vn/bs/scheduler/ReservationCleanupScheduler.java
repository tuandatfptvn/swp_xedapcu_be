package swp.be.vn.bs.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.entity.Post;
import swp.be.vn.bs.entity.PostStatus;
import swp.be.vn.bs.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to automatically unlock expired post reservations
 * Runs every 2 minutes to check for posts with expired reservedUntil timestamp
 */
@Component
public class ReservationCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservationCleanupScheduler.class);
    
    @Autowired
    private PostRepository postRepository;
    
    /**
     * Chạy mỗi 2 phút để kiểm tra và unlock các post hết hạn reservation
     * Cron expression: Every 2 minutes
     */
    @Scheduled(cron = "0 */2 * * * *")
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        
        // Tìm tất cả posts có reservation expired (reserved_until < now và status = RESERVED)
        List<Post> expiredReservations = postRepository.findAll().stream()
                .filter(post -> post.getReservedUntil() != null 
                        && post.getReservedUntil().isBefore(now)
                        && post.getStatus() == PostStatus.RESERVED)
                .toList();
        
        if (!expiredReservations.isEmpty()) {
            logger.info("Found {} expired reservations to unlock", expiredReservations.size());
            
            for (Post post : expiredReservations) {
                logger.info("Unlocking post ID {} - Reserved by user {}, expired at {}", 
                    post.getPostId(), post.getReservedBy(), post.getReservedUntil());
                
                // Unlock post
                post.setStatus(PostStatus.ACTIVE);
                post.setReservedUntil(null);
                post.setReservedBy(null);
                postRepository.save(post);
            }
            
            logger.info("Successfully unlocked {} expired reservations", expiredReservations.size());
        } else {
            logger.debug("No expired reservations found at {}", now);
        }
    }
    
    /**
     * Optional: Chạy mỗi ngày lúc 2:00 AM để log statistics
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void logDailyStatistics() {
        long activeReservations = postRepository.findAll().stream()
                .filter(post -> post.getReservedUntil() != null 
                        && post.getReservedUntil().isAfter(LocalDateTime.now())
                        && post.getStatus() == PostStatus.RESERVED)
                .count();
        
        logger.info("Daily Stats - Active reservations: {}", activeReservations);
    }
}
