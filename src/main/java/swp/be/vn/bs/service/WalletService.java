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

    @Autowired
    private TransactionService transactionService;

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

    public Wallet getWalletByUserId(Integer userId) {
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

    public BigDecimal getBalance(Integer userId) {
        return getWalletByUserId(userId).getBalance();
    }

    public boolean checkBalance(Integer userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);
        return wallet.getBalance().compareTo(amount) >= 0;
    }

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

    @Transactional
    public Transaction createPendingDeposit(Integer userId, BigDecimal amount) {
        Wallet wallet = getWalletByUserId(userId);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUser(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void processVnPayCallback(Integer transactionId, BigDecimal vnpAmount, String vnpResponseCode) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction already processed");
        }

        BigDecimal expectedAmount = transaction.getAmount().multiply(new BigDecimal(100));
        if (expectedAmount.compareTo(vnpAmount) != 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Amount mismatch");
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
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setBankAccount(bankAccount);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    public Page<TransactionResponse> getTransactions(Integer userId, Pageable pageable) {
        Wallet wallet = getWalletByUserId(userId);
        Page<Transaction> transactions = transactionRepository
                .findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);

        return transactions.map(this::mapToResponse);
    }

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
