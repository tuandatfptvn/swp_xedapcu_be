package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.WalletRequest;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.service.WalletService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // Xem số dư
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(@RequestParam Integer userId) {
        return ResponseEntity.ok(walletService.getBalance(userId));
    }


    // Rút tiền
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @RequestParam Integer userId,
            @RequestBody WalletRequest request) {
        try {
            Transaction transaction = walletService.withdraw(userId, request.getAmount(), request.getBankAccount());
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            // Nếu lỗi (ví dụ không đủ tiền), trả về mã 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Xem lịch sử
    @GetMapping("/transactions")
    public ResponseEntity<Page<Transaction>> getTransactions(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Transaction> transactions = walletService.getTransactions(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(transactions);
    }
}