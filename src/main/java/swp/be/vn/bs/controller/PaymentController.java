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
            // Lưu hóa đơn nháp PENDING
            Transaction transaction = walletService.createPendingDeposit(userId, request.getAmount());

            // Generate link
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

    // 2. API nhận Callback (IPN) từ VNPay
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            // CASE 4: Kiểm tra chữ ký bảo mật (Checksum) chống giả mạo API
            if (!paymentService.verifyIpnChecksum(params)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return ResponseEntity.ok(response);
            }

            Integer transactionId = Integer.parseInt(params.get("vnp_TxnRef"));
            BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount"));
            String vnpResponseCode = params.get("vnp_ResponseCode");

            // Xử lý nghiệp vụ cộng tiền
            walletService.processVnPayCallback(transactionId, vnpAmount, vnpResponseCode);

            // Báo cho VNPay biết đã ghi nhận thành công
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Báo lỗi cho VNPay (VD: Giao dịch đã xử lý, sai tiền...)
            response.put("RspCode", "02");
            response.put("Message", e.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return ResponseEntity.ok(response);
        }
    }
}