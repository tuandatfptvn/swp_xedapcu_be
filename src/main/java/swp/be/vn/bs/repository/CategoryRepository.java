package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp.be.vn.bs.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
