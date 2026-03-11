package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Transaction;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    
    List<Transaction> findByUser_UserId(Integer userId);
    
    List<Transaction> findByUser_Email(String email);
    
    List<Transaction> findByWallet_WalletId(Integer walletId);
    
    List<Transaction> findByOrder_OrderId(Integer orderId);
}
