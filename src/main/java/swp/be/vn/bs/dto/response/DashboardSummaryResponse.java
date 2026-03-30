package swp.be.vn.bs.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    // User Stats
    private Map<String, Object> userStats;
    
    // Post Stats
    private Map<String, Object> postStats;
    
    // Order Stats
    private Map<String, Object> orderStats;
    
    // Financial Stats
    private FinancialStats financialStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialStats {
        private BigDecimal platformRevenue; // Commission từ phí kiểm định, phí đăng bài, và phí hoa hồng đơn hàng
        private long totalTransactions;
        private BigDecimal totalOrderValue; // Tổng giá trị các đơn hàng thành công trên hệ thống
    }
}
