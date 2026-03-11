package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.TransactionResponse;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.entity.TransactionStatus;
import swp.be.vn.bs.entity.TransactionType;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.Wallet;
import swp.be.vn.bs.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.entity.TransactionType;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.Wallet;
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
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setBankAccount(bankAccount);

        Transaction savedTransaction = transactionRepository.save(transaction);


        return mapToResponse(savedTransaction);
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
                .transactionType(t.getTransactionType() != null ? t.getTransactionType().name() : null)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .createdAt(t.getCreatedAt())
                .bankAccount(t.getBankAccount())

                .build();
    }
}
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TransactionService transactionService;
    
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
    public Wallet getWalletByUserId(Integer userId) {
        return walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user ID: " + userId));
    }
    
    /**
     * Lấy wallet theo email
     */
    public Wallet getWalletByEmail(String email) {
        return walletRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Wallet not found for email: " + email));
    }
    
    /**
     * Kiểm tra user có đủ tiền không
     */
    public boolean checkBalance(Integer userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);
        return wallet.getBalance().compareTo(amount) >= 0;
    }
    
    /**
     * Trừ tiền từ wallet (dùng cho phí đăng tin, thanh toán)
     * Tạo Transaction record tự động
     */
    @Transactional
    public Transaction chargeFee(Integer userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Wallet wallet = getOrCreateWallet(user);
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                String.format("Insufficient balance. Required: %s, Available: %s", 
                    amount, wallet.getBalance())
            );
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        return transactionService.createTransaction(
            wallet, 
            user, 
            null, 
            amount.negate(),
            TransactionType.FEE, 
            description
        );
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
            wallet, 
            user, 
            null, 
            amount,
            TransactionType.DEPOSIT, 
            "Deposit to wallet"
        );
    }
    
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
                String.format("Insufficient balance to lock. Required: %s, Available: %s", 
                    amount, wallet.getBalance())
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
                String.format("Insufficient locked balance. Required: %s, Available: %s", 
                    amount, wallet.getLockedBalance())
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
                String.format("Insufficient locked balance. Required: %s, Available: %s", 
                    amount, wallet.getLockedBalance())
            );
        }
        
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        walletRepository.save(wallet);
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
        
        transactionService.createTransaction(
            fromWallet, fromUser, null, amount.negate(), 
            TransactionType.TRANSFER, description + " (sent)"
        );
        
        transactionService.createTransaction(
            toWallet, toUser, null, amount, 
            TransactionType.TRANSFER, description + " (received)"
        );
    }
}
