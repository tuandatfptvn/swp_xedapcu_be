package swp.be.vn.bs.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.WalletRequest;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.service.PaymentService;
import swp.be.vn.bs.service.WalletService;

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
            // 1. Xác nhận callback từ VNPay
            paymentService.updateTransactionStatus(params);

            // 2. Xây dựng redirect URL với query parameters
            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");
            
            String status = "00".equals(responseCode) ? "success" : "failed";
            String redirectUrl = String.format(
                "%s?status=%s&transactionId=%s&responseCode=%s",
                paymentService.getFrontendReturnUrl(),
                status,
                txnRef,
                responseCode
            );
            
            // 3. Redirect về frontend (302 Found)
            return ResponseEntity.status(302)
                    .header("Location", redirectUrl)
                    .build();
                    
        } catch (Exception e) {
            // 4. Nếu lỗi, redirect về frontend với status failed
            String failedUrl = paymentService.getFrontendReturnUrl() + "?status=failed&error=" + 
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(302)
                    .header("Location", failedUrl)
                    .build();
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