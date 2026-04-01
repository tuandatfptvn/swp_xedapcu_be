package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.UpdateRoleRequest;
import swp.be.vn.bs.dto.response.UserResponse;
import swp.be.vn.bs.dto.response.InspectionReportResponse;
import swp.be.vn.bs.dto.response.DashboardSummaryResponse;
import swp.be.vn.bs.entity.Role;
import swp.be.vn.bs.entity.Post;
import swp.be.vn.bs.entity.PostStatus;
import swp.be.vn.bs.service.AdminService;
import swp.be.vn.bs.service.InspectionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private AdminService adminService;

    @Autowired
    private InspectionService inspectionService;
    
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
            response.put("message", "✅ User account deactivated successfully (Soft delete)!");
            response.put("email", email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // ===== POST MANAGEMENT =====
    
    /**
     * GET /api/admin/posts
     * Lấy danh sách posts (phân trang)
     */
    @GetMapping("/posts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Post> posts = adminService.getAllPosts(page, size);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/posts/search
     * Tìm kiếm posts theo title
     */
    @GetMapping("/posts/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Post> results = adminService.searchPosts(keyword, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("total", results.getTotalElements());
            response.put("posts", results.getContent());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/posts/status/{status}
     * Lọc posts theo status
     */
    @GetMapping("/posts/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPostsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
            Page<Post> posts = adminService.getPostsByStatus(postStatus, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("total", posts.getTotalElements());
            response.put("posts", posts.getContent());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/posts/stats
     * Lấy thống kê posts
     */
    @GetMapping("/posts/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPostStats() {
        try {
            Map<String, Object> stats = adminService.getPostStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/posts/{postId}
     * Lấy chi tiết post
     */
    @GetMapping("/posts/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPostById(@PathVariable Integer postId) {
        try {
            Post post = adminService.getPostById(postId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/posts/{postId}/status
     * Cập nhật status post
     */
    @PutMapping("/posts/{postId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePostStatus(
            @PathVariable Integer postId,
            @RequestParam PostStatus status) {
        try {
            Post updatedPost = adminService.updatePostStatus(postId, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post status updated successfully!");
            response.put("post", updatedPost);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/posts/{postId}/disable
     * Khóa bài post (CANCELLED)
     */
    @PutMapping("/posts/{postId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disablePost(@PathVariable Integer postId) {
        try {
            Post disabledPost = adminService.disablePost(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post disabled successfully!");
            response.put("post", disabledPost);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/admin/posts/{postId}/enable
     * Mở khóa bài post (ACTIVE)
     */
    @PutMapping("/posts/{postId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enablePost(@PathVariable Integer postId) {
        try {
            Post enabledPost = adminService.enablePost(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post enabled successfully!");
            response.put("post", enabledPost);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * DELETE /api/admin/posts/{postId}
     * Xóa post
     */
    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePost(@PathVariable Integer postId) {
        try {
            adminService.deletePost(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post deleted successfully!");
            response.put("postId", postId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // ===== ORDER MANAGEMENT =====
    
    /**
     * GET /api/admin/orders
     * Lấy danh sách orders (phân trang)
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<swp.be.vn.bs.dto.response.OrderResponse> orders = adminService.getAllOrders(page, size);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/orders/search
     * Tìm kiếm orders theo orderId hoặc email
     */
    @GetMapping("/orders/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchOrders(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<swp.be.vn.bs.dto.response.OrderResponse> results = adminService.searchOrders(keyword, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("total", results.getTotalElements());
            response.put("orders", results.getContent());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/admin/orders/stats
     * Lấy thống kê orders
     */
    @GetMapping("/orders/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrderStats() {
        try {
            Map<String, Object> stats = adminService.getOrderStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    /**
     * GET /api/admin/inspections/{inspectionId}/report
     * Admin xem báo cáo kiểm định theo inspection ID
     */
    @GetMapping("/inspections/{inspectionId}/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getInspectionReport(@PathVariable Integer inspectionId) {
        try {
            InspectionReportResponse report = inspectionService.getReport(inspectionId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/bookings/{bookingId}/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getInspectionReportByBookingId(@PathVariable Integer bookingId) {
        try {
            InspectionReportResponse report = inspectionService.getReportByBookingId(bookingId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/admin/dashboard/summary
     * Lấy dashboard tổng quát cho admin
     */
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardSummary() {
        try {
            DashboardSummaryResponse summary = adminService.getDashboardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching dashboard: " + e.getMessage());
        }
    }
}
