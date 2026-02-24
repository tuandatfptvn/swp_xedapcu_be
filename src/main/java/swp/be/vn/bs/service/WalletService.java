package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.entity.TransactionStatus;
import swp.be.vn.bs.entity.TransactionType;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.Wallet;
import swp.be.vn.bs.repository.TransactionRepository;
import swp.be.vn.bs.repository.UserRepository;
import swp.be.vn.bs.repository.WalletRepository;

import java.math.BigDecimal;

@Service
public class WalletService {


    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;


    public Wallet getWalletByUserId(Integer userId){
        return walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(user);
                    newWallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
    }


    public BigDecimal getBalance(Integer userId) {
        return getWalletByUserId(userId).getBalance();
    }


    // 1. Tạo giao dịch CHỜ THANH TOÁN
    @Transactional
    public Transaction createPendingDeposit(Integer userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUser(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING); // BẮT BUỘC: Đang chờ, không cộng tiền lúc này

        return transactionRepository.save(transaction);
    }

    // 2. Xử lý IPN khi VNPay gọi về báo kết quả
    @Transactional
    public void processVnPayCallback(Integer transactionId, BigDecimal vnpAmount, String vnpResponseCode) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));

        // CASE 1: Chống nạp đúp (Idempotency) - Chỉ xử lý nếu đang PENDING
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Giao dịch này đã được xử lý trước đó!");
        }

        // CASE 2: Kiểm tra dữ liệu khớp tiền không (VNPay trả về x100)
        BigDecimal expectedAmount = transaction.getAmount().multiply(new BigDecimal(100));
        if (expectedAmount.compareTo(vnpAmount) != 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Bảo mật: Số tiền không khớp!");
        }

        // CASE 3: Kiểm tra mã trạng thái giao dịch
        if ("00".equals(vnpResponseCode)) {
            // Thanh toán thành công -> Cộng tiền
            Wallet wallet = transaction.getWallet();
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            walletRepository.save(wallet);
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else {
            // Thanh toán thất bại hoặc người dùng hủy
            transaction.setStatus(TransactionStatus.FAILED);
        }

        transactionRepository.save(transaction);
    }


    @Transactional
    public Transaction withdraw(Integer userId, BigDecimal amount, String bankAccount) {
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
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }


    public Page<Transaction> getTransactions(Integer userId, Pageable pageable) {
        Wallet wallet = getWalletByUserId(userId);
        return transactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
    }
}