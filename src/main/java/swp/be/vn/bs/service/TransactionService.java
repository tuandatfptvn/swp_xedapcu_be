package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.entity.Order;
import swp.be.vn.bs.entity.Transaction;
import swp.be.vn.bs.entity.TransactionStatus;
import swp.be.vn.bs.entity.TransactionType;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.Wallet;
import swp.be.vn.bs.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Tạo transaction record
     */
    @Transactional
    public Transaction createTransaction(
            Wallet wallet,
            User user,
            Order order,
            BigDecimal amount,
            TransactionType type,
            String description
    ) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setOrder(order);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Lấy lịch sử giao dịch của user
     */
    public List<Transaction> getTransactionHistory(Integer userId) {
        return transactionRepository.findByUser_UserId(userId);
    }
    
    /**
     * Lấy lịch sử giao dịch theo email
     */
    public List<Transaction> getTransactionHistoryByEmail(String email) {
        return transactionRepository.findByUser_Email(email);
    }
    
    /**
     * Lấy giao dịch của một đơn hàng
     */
    public List<Transaction> getTransactionsByOrder(Integer orderId) {
        return transactionRepository.findByOrder_OrderId(orderId);
    }
    
    /**
     * Lấy giao dịch của một wallet
     */
    public List<Transaction> getTransactionsByWallet(Integer walletId) {
        return transactionRepository.findByWallet_WalletId(walletId);
    }
}
