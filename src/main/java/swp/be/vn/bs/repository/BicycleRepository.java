package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp.be.vn.bs.entity.Bicycle;

public interface BicycleRepository extends JpaRepository<Bicycle, Integer> {
}