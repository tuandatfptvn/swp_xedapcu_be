package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.WalletRequest;
import swp.be.vn.bs.dto.response.TransactionResponse;
import swp.be.vn.bs.service.WalletService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;


    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(@RequestParam Integer userId) {
        return ResponseEntity.ok(walletService.getBalance(userId));
    }


    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @RequestParam Integer userId,
            @RequestBody WalletRequest request) {
        try {

            TransactionResponse response = walletService.withdraw(userId, request.getAmount(), request.getBankAccount());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> transactions = walletService.getTransactions(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(transactions);
    }

    /**
     * ADMIN API: Hoàn penalty cho buyer theo tỷ lệ 1-99 (Admin-Buyer)
     * Admin giữ 1%, Buyer lấy lại 99%
     * POST /api/wallet/admin/refund-penalty
     */
    @PostMapping("/admin/refund-penalty")
    public ResponseEntity<?> refundPenalty(
            @RequestParam Integer buyerId,
            @RequestParam BigDecimal penaltyAmount,
            @RequestParam String description) {
        try {
            walletService.refundPenaltyToBuyer(buyerId, penaltyAmount, description);
            BigDecimal buyerRefund = penaltyAmount.multiply(new BigDecimal("0.99"));
            BigDecimal adminKeeps = penaltyAmount.multiply(new BigDecimal("0.01"));
            return ResponseEntity.ok("✅ Admin keeps " + adminKeeps + " VND (1%). " +
                    "Refunded " + buyerRefund + " VND to buyer (99%).");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Error: " + e.getMessage());
        }
    }
}
