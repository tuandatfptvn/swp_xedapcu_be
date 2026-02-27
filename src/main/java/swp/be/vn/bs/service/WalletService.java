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