package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp.be.vn.bs.entity.Wallet;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {
   Optional<Wallet> findByUser_UserId(Integer userId);
}
