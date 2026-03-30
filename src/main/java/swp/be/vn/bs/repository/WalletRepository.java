package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    Optional<Wallet> findByUser_UserId(Integer userId);

    Optional<Wallet> findByUser_Email(String email);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w")
    BigDecimal sumAllBalances();

    @Query("SELECT COALESCE(SUM(w.lockedBalance), 0) FROM Wallet w")
    BigDecimal sumAllLockedBalances();
}