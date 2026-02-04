package swp.be.vn.bs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.Post;
import swp.be.vn.bs.entity.PostStatus;
import swp.be.vn.bs.entity.User;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    
    Page<Post> findAll(Pageable pageable);
    
    List<Post> findBySeller(User seller);
    
    List<Post> findBySeller_Email(String email);
    
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    
    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
