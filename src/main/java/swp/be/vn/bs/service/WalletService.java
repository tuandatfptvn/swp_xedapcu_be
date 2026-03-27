package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.response.TransactionResponse;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.entity.TransactionStatus;
import swp.be.vn.bs.entity.TransactionType;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.Wallet;
import swp.be.vn.bs.repository.TransactionRepository;
import swp.be.vn.bs.repository.UserRepository;
import swp.be.vn.bs.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionService transactionService;

    // =========================================================
    // CONFIG: Penalty Refund Ratio (Admin-Buyer)
    // =========================================================
    // Admin giữ: 1%, Hoàn buyer: 99%
    private static final BigDecimal ADMIN_PENALTY_RATIO = new BigDecimal("0.01");  // 1%
    private static final BigDecimal BUYER_PENALTY_REFUND_RATIO = new BigDecimal("0.99");  // 99%

    // =========================================================
    // 1. CÁC HÀM GET & KIỂM TRA WALLET
    // =========================================================

    /**
     * Lấy hoặc tạo wallet cho user (nếu chưa có)
     */
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setLockedBalance(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    /**
     * Lấy wallet theo userId
     */
    public Wallet getWalletByUserId(Integer userId){
        return walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(user);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setLockedBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
    }

    /**
     * Lấy wallet theo email
     */
    public Wallet getWalletByEmail(String email) {
        return walletRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Wallet not found for email: " + email));
    }

    public BigDecimal getBalance(Integer userId) {
        return getWalletByUserId(userId).getBalance();
    }

    /**
     * Kiểm tra user có đủ tiền không
     */
    public boolean checkBalance(Integer userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);
        return wallet.getBalance().compareTo(amount) >= 0;
    }

    // =========================================================
    // 2. CÁC HÀM GIAO DỊCH TIỀN (NẠP, RÚT, TRỪ PHÍ)
    // =========================================================

    @Transactional
    public Transaction createPendingDeposit(Integer userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUser(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING); // BẮT BUỘC: Đang chờ, không cộng tiền lúc này

        return transactionRepository.save(transaction);
    }

    /**
     * Nạp tiền vào wallet
     */
    @Transactional
    public Transaction deposit(Integer userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than 0");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Wallet wallet = getOrCreateWallet(user);
        
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        
        return transactionService.createTransaction(
            wallet, user, null, amount, TransactionType.DEPOSIT, "Deposit to wallet"
        );
    }

    @Transactional
    public TransactionResponse withdraw(Integer userId, BigDecimal amount, String bankAccount) {
        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUser(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setBankAccount(bankAccount);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    /**
     * Trừ tiền từ wallet (dùng cho phí đăng tin, kiểm định, etc)
     * TỰ ĐỘNG chuyển phí tới admin account
     * ⚠️ CHỈ DÙNG CHO FEES, KHÔNG DÙNG CHO THANH TOÁN!
     * 
     * @param transactionType FEE (mặc định), hoặc POST_FEE, INSPECTION_FEE, PENALTY, etc.
     */
    @Transactional
    public Transaction chargeFee(Integer userId, BigDecimal amount, String description, TransactionType transactionType) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Wallet wallet = getOrCreateWallet(user);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                String.format("Insufficient balance. Required: %s, Available: %s", amount, wallet.getBalance())
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        Transaction feeTransaction = transactionService.createTransaction(
            wallet, user, null, amount.negate(), transactionType, description
        );
        
        // 🔑 KEY: Tự động chuyển phí tới admin account (với same transaction type)
        transferFeeToAdmin(amount, description, transactionType);
        
        return feeTransaction;
    }
    
    /**
     * Overload: Gọi chargeFee với TransactionType.FEE mặc định
     */
    @Transactional
    public Transaction chargeFee(Integer userId, BigDecimal amount, String description) {
        return chargeFee(userId, amount, description, TransactionType.FEE);
    }

    /**
     * Trừ tiền thanh toán từ wallet (dùng cho thanh toán 80%, etc)
     * ⚠️ KHÔNG chuyển tới admin - tiền này thuộc về seller/người nhận
     */
    @Transactional
    public Transaction chargePayment(Integer userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be greater than 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Wallet wallet = getOrCreateWallet(user);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                String.format("Insufficient balance. Required: %s, Available: %s", amount, wallet.getBalance())
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        // Ghi nhận transaction (type=PAYMENT, không tự động chuyển admin)
        return transactionService.createTransaction(
            wallet, user, null, amount.negate(), TransactionType.PAYMENT, description
        );
    }

    /**
     * Chuyển phí/penalty tới admin account duy nhất (quản lý toàn bộ commission)
     * Lấy admin user từ database (giả sử có admin account với role=ADMIN)
     * transactionType: Loại transaction (FEE, INSPECTION_FEE, PENALTY, etc.)
     */
    @Transactional
    public void transferFeeToAdmin(BigDecimal amount, String description, TransactionType transactionType) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Fee amount must be greater than 0");
        }

        // Tìm admin account đầu tiên (thường là ID=1 hoặc email=admin@...)
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ADMIN".equals(u.getRole().toString()))
                .collect(Collectors.toList());
        
        if (admins.isEmpty()) {
            throw new RuntimeException("⚠️ No ADMIN account found. Please create an admin account first!");
        }
        
        User adminUser = admins.get(0);  // Lấy admin đầu tiên
        Wallet adminWallet = getOrCreateWallet(adminUser);
        
        // Cộng tiền vào admin wallet
        adminWallet.setBalance(adminWallet.getBalance().add(amount));
        walletRepository.save(adminWallet);
        
        // Ghi nhận transaction với đúng transaction type
        transactionService.createTransaction(
            adminWallet, adminUser, null, amount,
            transactionType,
            description + " (From: Platform Commission)"
        );
        
        System.out.println("✅ Transferred " + amount + " VND to ADMIN account: " + adminUser.getEmail());
    }
    
    /**
     * Overload: transferFeeToAdmin với TransactionType.DEPOSIT mặc định (để backward compatibility)
     */
    @Transactional
    public void transferFeeToAdmin(BigDecimal amount, String description) {
        transferFeeToAdmin(amount, description, TransactionType.DEPOSIT);
    }

    /**
     * Hoàn penalty cho buyer theo tỷ lệ (Buyer: 99%, Admin: 1%)
     * Dùng khi buyer cancel order - tự động split ngay
     * hoặc khi admin quyết định refund một phần penalty
     */
    @Transactional
    public void refundPenaltyToBuyer(Integer buyerId, BigDecimal penaltyAmount, String description) {
        if (penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Penalty amount must be greater than 0");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found: " + buyerId));
        
        // Tính phần hoàn cho buyer (99%)
        BigDecimal buyerRefund = penaltyAmount.multiply(BUYER_PENALTY_REFUND_RATIO)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Tính phần admin giữ (1%)
        BigDecimal adminKeeps = penaltyAmount.multiply(ADMIN_PENALTY_RATIO)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // 1. Hoàn tiền cho buyer (99%)
        Wallet buyerWallet = getOrCreateWallet(buyer);
        buyerWallet.setBalance(buyerWallet.getBalance().add(buyerRefund));
        walletRepository.save(buyerWallet);
        
        // 2. Ghi nhận transaction cho buyer (PENALTY - mặc dù hoàn 99% nhưng vẫn là PENALTY)
        // Để track rõ ràng đây là tiền phạt được hoàn lại, không phải refund bình thường
        transactionService.createTransaction(
            buyerWallet, buyer, null, buyerRefund,
            TransactionType.PENALTY,
            description + " - Penalty refund to buyer (99%): " + buyerRefund
        );
        
        // 3. Transfer phần admin giữ (1%) từ admin wallet (dùng PENALTY type để track)
        transferFeeToAdmin(adminKeeps, 
            description + " - Platform fee (1%): " + adminKeeps,
            TransactionType.PENALTY);
        
        System.out.println("✅ Split complete - Buyer refunded: " + buyerRefund + " VND (99%). Admin fee: " + adminKeeps + " VND (1%)");

    }

    /**
     * Chuyển tiền giữa 2 user
     */
    @Transactional
    public void transferMoney(Integer fromUserId, Integer toUserId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than 0");
        }
        
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("From user not found: " + fromUserId));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("To user not found: " + toUserId));
        
        Wallet fromWallet = getOrCreateWallet(fromUser);
        Wallet toWallet = getOrCreateWallet(toUser);
        
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for transfer");
        }
        
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        
        transactionService.createTransaction(fromWallet, fromUser, null, amount.negate(), TransactionType.TRANSFER, description + " (sent)");
        transactionService.createTransaction(toWallet, toUser, null, amount, TransactionType.TRANSFER, description + " (received)");
    }

    // =========================================================
    // 3. CÁC HÀM LOCK / UNLOCK TIỀN CỌC
    // =========================================================

    /**
     * Khóa tiền (chuyển từ balance → lockedBalance)
     * Dùng khi buyer đặt cọc
     */
    @Transactional
    public void lockBalance(Integer userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Lock amount must be greater than 0");
        }

        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                String.format("Insufficient balance to lock. Required: %s, Available: %s", amount, wallet.getBalance())
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);
    }

    /**
     * Mở khóa tiền (chuyển từ lockedBalance → balance)
     * Dùng khi hủy đặt cọc
     */
    @Transactional
    public void unlockBalance(Integer userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Unlock amount must be greater than 0");
        }

        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                String.format("Insufficient locked balance. Required: %s, Available: %s", amount, wallet.getLockedBalance())
            );
        }

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    /**
     * Mở khóa và trừ tiền (dùng khi hoàn tất đơn hàng - trừ deposit đã lock)
     */
    @Transactional
    public void unlockAndDeduct(Integer userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                String.format("Insufficient locked balance. Required: %s, Available: %s", amount, wallet.getLockedBalance())
            );
        }

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    // =========================================================
    // 4. XỬ LÝ THANH TOÁN VN-PAY & MAPPING
    // =========================================================

    @Transactional
    public void processVnPayCallback(Integer transactionId, BigDecimal vnpAmount, String vnpResponseCode) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Giao dịch này đã được xử lý trước đó!");
        }

        BigDecimal expectedAmount = transaction.getAmount().multiply(new BigDecimal(100));
        if (expectedAmount.compareTo(vnpAmount) != 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Bảo mật: Số tiền không khớp!");
        }

        if ("00".equals(vnpResponseCode)) {
            Wallet wallet = transaction.getWallet();
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            walletRepository.save(wallet);
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }
        transactionRepository.save(transaction);
    }

    public Page<TransactionResponse> getTransactions(Integer userId, Pageable pageable) {
        Wallet wallet = getWalletByUserId(userId);
        Page<Transaction> transactions = transactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
        return transactions.map(this::mapToResponse);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .amount(t.getAmount())
                .transactionType(t.getType() != null ? t.getType().name() : null)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .createdAt(t.getCreatedAt())
                .bankAccount(t.getBankAccount())
                .build();
    }
}
