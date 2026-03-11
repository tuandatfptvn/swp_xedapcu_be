package swp.be.vn.bs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.entity.Order;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.UserViolation;
import swp.be.vn.bs.repository.UserRepository;
import swp.be.vn.bs.repository.UserViolationRepository;

import java.math.BigDecimal;

@Service
public class ViolationService {
    @Autowired private UserViolationRepository userViolationRepository;
    @Autowired private UserRepository userRepository;

    @Transactional
    public void recordViolation(User user, Order order, String violationType, BigDecimal penaltyAmount){
        // Tao record cu UserViolation
        UserViolation violation = new UserViolation();
        violation.setUser(user);
        violation.setOrder(order);
        violation.setViolationType(violationType);
        violation.setPenaltyAmount(penaltyAmount);
        userViolationRepository.save(violation);

        // tang dem violationCount cua User
        user.setViolationCount(user.getViolationCount() + 1);
        userRepository.save(user);
    }


}
