package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.PostRequest;
import swp.be.vn.bs.dto.response.PostResponse;
import swp.be.vn.bs.entity.PostStatus;
import swp.be.vn.bs.service.PostService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponse> postsPage = postService.getAllPosts(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("posts", postsPage.getContent());
        response.put("currentPage", postsPage.getNumber());
        response.put("totalItems", postsPage.getTotalElements());
        response.put("totalPages", postsPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Integer id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }
    
    @GetMapping("/my-posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PostResponse>> getMyPosts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<PostResponse> myPosts = postService.getMyPosts(userDetails.getUsername());
        return ResponseEntity.ok(myPosts);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> createPost(
            @RequestBody PostRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            PostResponse savedPost = postService.createPost(request, userDetails.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post created successfully!");
            response.put("post", savedPost);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating post: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePost(
            @PathVariable Integer id,
            @RequestBody PostRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            PostResponse updatedPost = postService.updatePost(id, request, userDetails.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post updated successfully!");
            response.put("post", updatedPost);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating post: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deletePost(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            postService.deletePost(id, userDetails.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post deleted successfully!");
            response.put("id", id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting post: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePostStatus(
            @PathVariable Integer id,
            @RequestParam PostStatus status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            PostResponse updatedPost = postService.updatePostStatus(id, status, userDetails.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Post status updated to: " + status);
            response.put("post", updatedPost);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponse> postsPage = postService.searchPosts(q, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("posts", postsPage.getContent());
        response.put("currentPage", postsPage.getNumber());
        response.put("totalItems", postsPage.getTotalElements());
        response.put("totalPages", postsPage.getTotalPages());
        response.put("keyword", q);
        
        return ResponseEntity.ok(response);
    }
}
