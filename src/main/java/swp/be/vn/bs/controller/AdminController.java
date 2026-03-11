package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.UpdateRoleRequest;
import swp.be.vn.bs.dto.response.UserResponse;
import swp.be.vn.bs.entity.Role;
import swp.be.vn.bs.service.AdminService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    /**
     * GET /api/admin/users
     * Lấy danh sách user (có hỗ trợ phân trang)
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<UserResponse> users = adminService.getAllUsersWithPagination(page, size);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/users/search
     * Tìm kiếm user theo email hoặc tên
     */
    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        try {
            List<UserResponse> results = adminService.searchUsers(keyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("total", results.size());
            response.put("results", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/users/role/{role}
     * Lọc user theo role (ADMIN, SELLER, USER)
     */
    @GetMapping("/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            List<UserResponse> users = adminService.getUsersByRole(userRole);
            
            Map<String, Object> response = new HashMap<>();
            response.put("role", role);
            response.put("total", users.size());
            response.put("users", users);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/users/stats
     * Lấy thống kê user
     */
    @GetMapping("/users/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats() {
        try {
            Map<String, Object> stats = adminService.getUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/users/{email}
     * Lấy chi tiết user
     */
    @GetMapping("/users/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            UserResponse user = adminService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/users/{email}/role
     * Cập nhật role user
     */
    @PutMapping("/users/{email}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable String email,
            @RequestBody UpdateRoleRequest request) {
        try {
            UserResponse user = adminService.updateUserRole(email, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Role updated successfully!");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/users/{email}/disable
     * Khóa tài khoản user
     */
    @PutMapping("/users/{email}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableUser(@PathVariable String email) {
        try {
            UserResponse user = adminService.disableUser(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ User disabled successfully!");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/users/{email}/enable
     * Mở khóa tài khoản user
     */
    @PutMapping("/users/{email}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableUser(@PathVariable String email) {
        try {
            UserResponse user = adminService.enableUser(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ User enabled successfully!");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/users/{email}
     * Cập nhật thông tin user (fullName, picture)
     */
    @PutMapping("/users/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserInfo(
            @PathVariable String email,
            @RequestBody Map<String, String> updates) {
        try {
            UserResponse user = adminService.updateUserInfo(email, updates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ User info updated successfully!");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * DELETE /api/admin/users/{email}
     * Xóa user
     */
    @DeleteMapping("/users/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        try {
            adminService.deleteUser(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ User deleted successfully!");
            response.put("email", email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
