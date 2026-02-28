package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.WalletRequest;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.dto.TransactionResponse; 
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
}
