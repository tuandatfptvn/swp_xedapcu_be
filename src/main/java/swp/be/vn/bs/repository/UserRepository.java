package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Role;
import swp.be.vn.bs.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    // ==================== OPTIMIZED QUERIES ====================
    
    /**
     * Lấy danh sách users theo role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") Role role);
    
    /**
     * Search users by email or full name (optimized query)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY u.email ASC")
    List<User> searchUsersByKeyword(@Param("keyword") String keyword);
}
