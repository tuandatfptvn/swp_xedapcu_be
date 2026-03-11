package swp.be.vn.bs.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swp.be.vn.bs.entity.Post;
import swp.be.vn.bs.entity.PostImage;
import swp.be.vn.bs.repository.PostImageRepository;
import swp.be.vn.bs.repository.PostRepository;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostImageService {

    private final PostImageRepository postImageRepository;
    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Upload image for a post
     */
    @Transactional
    public PostImage uploadImage(Integer postId, MultipartFile file, Boolean isThumbnail) throws IOException {
        // Validate post exists
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + postId));

        // Get current images
        List<PostImage> existingImages = postImageRepository.findByPost_PostIdOrderBySortOrderAsc(postId);
        
        // Validate max images limit (10 images per post)
        if (existingImages.size() >= 10) {
            throw new IllegalArgumentException("Mỗi bài đăng chỉ được upload tối đa 10 ảnh");
        }

        // Upload to Cloudinary
        String imageUrl = cloudinaryService.uploadImage(file, "bicycles/post_" + postId);

        // Get next sort order
        int nextSortOrder = existingImages.size() + 1;

        // If this is set as thumbnail, unset other thumbnails
        if (Boolean.TRUE.equals(isThumbnail)) {
            postImageRepository.findByPost_PostIdAndIsThumbnailTrue(postId)
                    .ifPresent(oldThumbnail -> {
                        oldThumbnail.setIsThumbnail(false);
                        postImageRepository.save(oldThumbnail);
                    });
        }

        // Create new image record
        PostImage postImage = new PostImage();
        postImage.setPost(post);
        postImage.setImageUrl(imageUrl);
        postImage.setSortOrder(nextSortOrder);
        postImage.setIsThumbnail(isThumbnail != null ? isThumbnail : false);

        return postImageRepository.save(postImage);
    }

    /**
     * Get all images for a post
     */
    public List<PostImage> getPostImages(Integer postId) {
        return postImageRepository.findByPost_PostIdOrderBySortOrderAsc(postId);
    }

    /**
     * Get thumbnail image for a post
     */
    public PostImage getThumbnail(Integer postId) {
        return postImageRepository.findByPost_PostIdAndIsThumbnailTrue(postId)
                .orElse(null);
    }

    /**
     * Set image as thumbnail
     */
    @Transactional
    public PostImage setThumbnail(Integer postId, Integer imageId) {
        PostImage image = postImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh với ID: " + imageId));

        if (!image.getPost().getPostId().equals(postId)) {
            throw new IllegalArgumentException("Ảnh không thuộc bài đăng này");
        }

        // Unset old thumbnail
        postImageRepository.findByPost_PostIdAndIsThumbnailTrue(postId)
                .ifPresent(oldThumbnail -> {
                    if (!oldThumbnail.getImageId().equals(imageId)) {
                        oldThumbnail.setIsThumbnail(false);
                        postImageRepository.save(oldThumbnail);
                    }
                });

        // Set new thumbnail
        image.setIsThumbnail(true);
        return postImageRepository.save(image);
    }

    /**
     * Delete image
     */
    @Transactional
    public void deleteImage(Integer postId, Integer imageId) {
        PostImage image = postImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh với ID: " + imageId));

        if (!image.getPost().getPostId().equals(postId)) {
            throw new IllegalArgumentException("Ảnh không thuộc bài đăng này");
        }

        // Delete from Cloudinary
        cloudinaryService.deleteImage(image.getImageUrl());

        // Delete from database
        postImageRepository.delete(image);

        // Reorder remaining images
        List<PostImage> remainingImages = postImageRepository.findByPost_PostIdOrderBySortOrderAsc(postId);
        for (int i = 0; i < remainingImages.size(); i++) {
            remainingImages.get(i).setSortOrder(i + 1);
        }
        postImageRepository.saveAll(remainingImages);
    }

    /**
     * Delete all images for a post
     */
    @Transactional
    public void deleteAllImages(Integer postId) {
        List<PostImage> images = postImageRepository.findByPost_PostIdOrderBySortOrderAsc(postId);
        
        // Delete from Cloudinary
        images.forEach(image -> cloudinaryService.deleteImage(image.getImageUrl()));
        
        // Delete from database
        postImageRepository.deleteByPost_PostId(postId);
    }
}
