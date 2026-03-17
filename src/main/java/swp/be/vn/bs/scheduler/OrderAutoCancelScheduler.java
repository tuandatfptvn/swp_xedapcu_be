package swp.be.vn.bs.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swp.be.vn.bs.service.OrderService;

@Component
public class OrderAutoCancelScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderAutoCancelScheduler.class);

    @Autowired
    private OrderService orderService;

    /**
     * Chạy mỗi 1 phút: auto-cancel các order đặt cọc quá 10 phút chưa confirm.
     */
    @Scheduled(fixedRate = 60_000)
    public void autoCancelExpiredDeposits() {
        try {
            int cancelled = orderService.autoCancelExpiredDeposits(10);
            if (cancelled > 0) {
                logger.info("Auto-cancelled {} expired deposit orders", cancelled);
            }
        } catch (Exception e) {
            logger.error("Failed to auto-cancel expired deposits", e);
        }
    }
}

