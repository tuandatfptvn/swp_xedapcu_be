package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.request.PostRequest;
import swp.be.vn.bs.dto.response.PostResponse;
import swp.be.vn.bs.entity.Bicycle;
import swp.be.vn.bs.entity.Post;
import swp.be.vn.bs.entity.PostStatus;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.BicycleRepository;
import swp.be.vn.bs.repository.PostRepository;
import swp.be.vn.bs.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BicycleRepository bicycleRepository;
    
    @Autowired
    private WalletService walletService;
    
    public Page<PostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findAll(pageable)
                .map(this::mapToResponse);
    }
    
    public PostResponse getPostById(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
        return mapToResponse(post);
    }
    
    public List<PostResponse> getMyPosts(String sellerEmail) {
        return postRepository.findBySeller_Email(sellerEmail).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public PostResponse createPost(PostRequest request, String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerEmail));
        
        Bicycle bicycle = bicycleRepository.findById(request.getBicycleId())
                .orElseThrow(() -> new RuntimeException("Bicycle not found with ID: " + request.getBicycleId()));
        
        if (!bicycle.getOwner().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("You can only create posts for your own bicycles");
        }
        
        // Tính post fee (5% giá xe)
        BigDecimal postFee = request.getPrice()
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_UP);
        
        // Check balance và charge fee
        if (!walletService.checkBalance(seller.getUserId(), postFee)) {
            throw new RuntimeException(
                String.format("Insufficient balance to pay post fee. Required: %s VND (5%% of %s)", 
                    postFee, request.getPrice())
            );
        }
        
        walletService.chargeFee(seller.getUserId(), postFee, 
            "Post fee (5%) for listing bicycle: " + request.getTitle());
        
        Post post = new Post();
        post.setSeller(seller);
        post.setBicycle(bicycle);
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setPrice(request.getPrice());
        post.setStatus(PostStatus.ACTIVE);
        post.setIsInspected(false);
        post.setPostFee(postFee);
        
        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost);
    }
    
    @Transactional
    public PostResponse updatePost(Integer id, PostRequest request, String sellerEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
        
        if (!post.getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("You can only update your own posts");
        }
        
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getDescription() != null) post.setDescription(request.getDescription());
        if (request.getPrice() != null) post.setPrice(request.getPrice());
        
        Post updatedPost = postRepository.save(post);
        return mapToResponse(updatedPost);
    }
    
    @Transactional
    public void deletePost(Integer id, String sellerEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
        
        if (!post.getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("You can only delete your own posts");
        }
        
        postRepository.delete(post);
    }
    
    @Transactional
    public PostResponse updatePostStatus(Integer id, PostStatus newStatus, String sellerEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
        
        if (!post.getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("You can only update your own posts");
        }
        
        post.setStatus(newStatus);
        Post updatedPost = postRepository.save(post);
        return mapToResponse(updatedPost);
    }
    
    public Page<PostResponse> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByTitleContainingIgnoreCase(keyword, pageable)
                .map(this::mapToResponse);
    }
    
    /**
     * Khóa post trong X phút (khi buyer đặt cọc)
     */
    @Transactional
    public void lockPost(Integer postId, Integer buyerUserId, int minutesToLock) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        
        post.setStatus(PostStatus.RESERVED);
        post.setReservedUntil(LocalDateTime.now().plusMinutes(minutesToLock));
        post.setReservedBy(buyerUserId);
        
        postRepository.save(post);
    }
    
    /**
     * Mở khóa post (khi hủy cọc hoặc hết thời gian)
     */
    @Transactional
    public void unlockPost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        
        post.setStatus(PostStatus.ACTIVE);
        post.setReservedUntil(null);
        post.setReservedBy(null);
        
        postRepository.save(post);
    }
    
    /**
     * Kiểm tra post có sẵn để mua không
     */
    public boolean isPostAvailable(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        
        if (post.getStatus() == PostStatus.SOLD || post.getStatus() == PostStatus.CANCELLED) {
            return false;
        }
        
        if (post.getStatus() == PostStatus.RESERVED) {
            if (post.getReservedUntil() != null && post.getReservedUntil().isAfter(LocalDateTime.now())) {
                return false;
            } else {
                unlockPost(postId);
                return true;
            }
        }
        
        return post.getStatus() == PostStatus.ACTIVE;
    }
    
    private PostResponse mapToResponse(Post post) {
        PostResponse.SellerInfo sellerInfo = PostResponse.SellerInfo.builder()
                .userId(post.getSeller().getUserId())
                .email(post.getSeller().getEmail())
                .fullName(post.getSeller().getFullName())
                .build();
        
        PostResponse.BicycleInfo bicycleInfo = null;
        if (post.getBicycle() != null) {
            bicycleInfo = PostResponse.BicycleInfo.builder()
                    .bicycleId(post.getBicycle().getBicycleId())
                    .brand(post.getBicycle().getBrand())
                    .frameSize(post.getBicycle().getFrameSize())
                    .groupset(post.getBicycle().getGroupset())
                    .conditionPercent(post.getBicycle().getConditionPercent())
                    .build();
        }
        
        return PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .description(post.getDescription())
                .price(post.getPrice())
                .status(post.getStatus())
                .isInspected(post.getIsInspected())
                .postFee(post.getPostFee())
                .createdAt(post.getCreatedAt())
                .seller(sellerInfo)
                .bicycle(bicycleInfo)
                .build();
    }
}
