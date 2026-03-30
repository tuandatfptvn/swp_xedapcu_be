package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.dto.request.UpdateRoleRequest;
import swp.be.vn.bs.dto.response.UserResponse;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.entity.Role;
import swp.be.vn.bs.entity.Post;
import swp.be.vn.bs.entity.PostStatus;
import swp.be.vn.bs.entity.Order;
import swp.be.vn.bs.entity.OrderStatus;
import swp.be.vn.bs.dto.response.OrderResponse;
import swp.be.vn.bs.repository.UserRepository;
import swp.be.vn.bs.repository.PostRepository;
import swp.be.vn.bs.repository.OrderRepository;
import swp.be.vn.bs.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Helper method: convert User → UserResponse
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .fullName(user.getFullName())
                .picture(user.getPicture())
                .isActive(user.getIsActive() != null ? user.getIsActive() : true)
                .build();
    }
    
    /**
     * Lấy danh sách tất cả user
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy danh sách user với phân trang
     */
    public Page<UserResponse> getAllUsersWithPagination(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable)
                .map(this::convertToUserResponse);
    }
    
    /**
     * Tìm kiếm user theo email hoặc tên
     */
    public List<UserResponse> searchUsers(String keyword) {
        return userRepository.findAll().stream()
                .filter(user -> user.getEmail().contains(keyword) || 
                        (user.getFullName() != null && user.getFullName().contains(keyword)))
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lọc user theo role
     */
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == role)
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy thống kê user
     */
    public Map<String, Object> getUserStats() {
        List<User> allUsers = userRepository.findAll();
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", allUsers.size());
        stats.put("admins", allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count());
        stats.put("sellers", allUsers.stream().filter(u -> u.getRole() == Role.SELLER).count());
        stats.put("buyers", allUsers.stream().filter(u -> u.getRole() == Role.USER).count());
        stats.put("activeUsers", allUsers.stream().filter(u -> u.getIsActive()).count());
        stats.put("disabledUsers", allUsers.stream().filter(u -> !u.getIsActive()).count());
        
        return stats;
    }
    
    /**
     * Khóa tài khoản user
     */
    public UserResponse disableUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        return convertToUserResponse(user);
    }
    
    /**
     * Mở khóa tài khoản user
     */
    public UserResponse enableUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        user.setIsActive(true);
        userRepository.save(user);
        
        return convertToUserResponse(user);
    }
    
    /**
     * Cập nhật thông tin user
     */
    public UserResponse updateUserInfo(String email, Map<String, String> updates) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (updates.containsKey("fullName")) {
            user.setFullName(updates.get("fullName"));
        }
        if (updates.containsKey("picture")) {
            user.setPicture(updates.get("picture"));
        }
        
        userRepository.save(user);
        return convertToUserResponse(user);
    }
    
    /**
     * Cập nhật role user
     */
    public UserResponse updateUserRole(String email, UpdateRoleRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        user.setRole(request.getRole());
        userRepository.save(user);
        
        return convertToUserResponse(user);
    }
    
    /**
     * Lấy user theo email
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        return convertToUserResponse(user);
    }
    
    /**
     * Xóa user
     */
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        userRepository.delete(user);
    }
    
    // ===== POST MANAGEMENT =====
    
    /**
     * Lấy danh sách tất cả posts (phân trang)
     */
    public Page<Post> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAll(pageable);
    }
    
    /**
     * Tìm kiếm posts theo title
     */
    public Page<Post> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }
    
    /**
     * Lọc posts theo status
     */
    public Page<Post> getPostsByStatus(PostStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByStatus(status, pageable);
    }
    
    /**
     * Lấy chi tiết post
     */
    public Post getPostById(Integer postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
    }
    
    /**
     * Lấy thống kê posts
     */
    public Map<String, Object> getPostStats() {
        List<Post> allPosts = postRepository.findAll();
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalPosts", allPosts.size());
        stats.put("active", allPosts.stream().filter(p -> p.getStatus() == PostStatus.ACTIVE).count());
        stats.put("pending", allPosts.stream().filter(p -> p.getStatus() == PostStatus.PENDING).count());
        stats.put("reserved", allPosts.stream().filter(p -> p.getStatus() == PostStatus.RESERVED).count());
        stats.put("sold", allPosts.stream().filter(p -> p.getStatus() == PostStatus.SOLD).count());
        stats.put("cancelled", allPosts.stream().filter(p -> p.getStatus() == PostStatus.CANCELLED).count());
        stats.put("expired", allPosts.stream().filter(p -> p.getStatus() == PostStatus.EXPIRED).count());
        
        return stats;
    }
    
    /**
     * Cập nhật status post
     */
    public Post updatePostStatus(Integer postId, PostStatus newStatus) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        
        post.setStatus(newStatus);
        return postRepository.save(post);
    }
    
    /**
     * Khóa bài post (chuyển sang RESERVED)
     */
    public Post disablePost(Integer postId) {
        return updatePostStatus(postId, PostStatus.RESERVED);
    }
    
    /**
     * Mở khóa bài post (chuyển sang ACTIVE)
     */
    public Post enablePost(Integer postId) {
        return updatePostStatus(postId, PostStatus.ACTIVE);
    }
    
    /**
     * Xóa post
     */
    public void deletePost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        
        postRepository.delete(post);
    }
    
    // ===== ORDER MANAGEMENT =====
    
    /**
     * Lấy danh sách tất cả orders (phân trang)
     */
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable)
                .map(orderService::mapToResponse);
    }
    
    /**
     * Tìm kiếm orders theo keyword (email hoặc orderId)
     */
    public Page<OrderResponse> searchOrders(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.searchOrders(keyword, pageable)
                .map(orderService::mapToResponse);
    }
    
    /**
     * Lấy thống kê orders
     */
    public Map<String, Object> getOrderStats() {
        List<Order> allOrders = orderRepository.findAll();
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalOrders", allOrders.size());
        
        // Count by status
        stats.put("depositPaid", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DEPOSIT_PAID).count());
        stats.put("pendingSellerConfirmation", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING_SELLER_CONFIRMATION).count());
        stats.put("assignedToInspector", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.ASSIGNED_TO_INSPECTOR).count());
        stats.put("inDelivery", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.IN_DELIVERY).count());
        stats.put("completed", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
        stats.put("cancelled", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());
        
        // Sum revenue (total amount of completed orders)
        java.math.BigDecimal totalRevenue = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
            .map(Order::getTotalAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);
            
        return stats;
    }
}
