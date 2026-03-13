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
import swp.be.vn.bs.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
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
}
