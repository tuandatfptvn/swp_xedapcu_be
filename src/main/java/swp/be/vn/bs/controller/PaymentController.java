package swp.be.vn.bs.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.WalletRequest;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.service.PaymentService;
import swp.be.vn.bs.service.WalletService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletService walletService;

    // 1. API tạo link thanh toán VNPay

    @PostMapping("/create-deposit-url")
    public ResponseEntity<?> createDepositUrl(
            @RequestParam Integer userId,
            @RequestBody WalletRequest request,
            HttpServletRequest httpRequest) {
        try {

            Transaction transaction = walletService.createPendingDeposit(userId, request.getAmount());


            String paymentUrl = paymentService.createVnPayUrl(
                    transaction.getTransactionId(),
                    request.getAmount().longValue(),
                    httpRequest);

            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tạo URL: " + e.getMessage());
        }
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            paymentService.updateTransactionStatus(params);

            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");

            Map<String, String> result = new HashMap<>();
            if ("00".equals(responseCode)) {
                result.put("status", "success");
                result.put("transactionId", txnRef);
            } else {
                result.put("status", "failed");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý return: " + e.getMessage());
        }
    }


    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {

            if (!paymentService.verifyIpnChecksum(params)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return ResponseEntity.ok(response);
            }

            paymentService.updateTransactionStatus(params);

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);

        }catch (RuntimeException e) {
            if (e.getMessage().contains("Số tiền không khớp")) {
                response.put("RspCode", "04");
            } else if (e.getMessage().contains("đã được xử lý")) {
                response.put("RspCode", "02");
            } else {
                response.put("RspCode", "99");
            }
            response.put("Message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}