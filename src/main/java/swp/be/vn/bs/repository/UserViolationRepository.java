package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.UserViolation;

import java.util.List;

@Repository
public interface UserViolationRepository extends JpaRepository<UserViolation, Integer> {
    List<UserViolation> findByUser_UserId(Integer userId);

}
