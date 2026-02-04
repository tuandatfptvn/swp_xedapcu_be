package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp.be.vn.bs.entity.Bicycle;
import swp.be.vn.bs.entity.User;

import java.util.List;

public interface BicycleRepository extends JpaRepository<Bicycle, Integer> {
    
    List<Bicycle> findByOwner(User owner);
    
    List<Bicycle> findByOwner_UserId(Integer ownerId);
}