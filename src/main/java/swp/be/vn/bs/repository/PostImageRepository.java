package swp.be.vn.bs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp.be.vn.bs.entity.PostImage;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Integer> {
    
    List<PostImage> findByPost_PostIdOrderBySortOrderAsc(Integer postId);
    
    Optional<PostImage> findByPost_PostIdAndIsThumbnailTrue(Integer postId);
    
    void deleteByPost_PostId(Integer postId);
    
    long countByPost_PostId(Integer postId);
}
