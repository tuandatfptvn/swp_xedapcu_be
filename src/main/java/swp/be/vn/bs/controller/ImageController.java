package swp.be.vn.bs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swp.be.vn.bs.dto.response.PostImageResponse;
import swp.be.vn.bs.entity.PostImage;
import swp.be.vn.bs.service.PostImageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts/{postId}/images")
@RequiredArgsConstructor
@Tag(name = "Image Management", description = "API quản lý ảnh xe đạp")
public class ImageController {

    private final PostImageService postImageService;

    @PostMapping
    @Operation(summary = "Upload ảnh cho bài đăng")
    public ResponseEntity<?> uploadImage(
            @PathVariable Integer postId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isThumbnail", required = false, defaultValue = "false") Boolean isThumbnail
    ) {
        try {
            PostImage image = postImageService.uploadImage(postId, file, isThumbnail);
            PostImageResponse imageResponse = mapToResponse(image);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Upload ảnh thành công");
            response.put("data", imageResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi upload ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách ảnh của bài đăng")
    public ResponseEntity<?> getPostImages(@PathVariable Integer postId) {
        List<PostImage> images = postImageService.getPostImages(postId);
        List<PostImageResponse> imageResponses = images.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", imageResponses);
        response.put("total", imageResponses.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/thumbnail")
    @Operation(summary = "Lấy ảnh thumbnail của bài đăng")
    public ResponseEntity<?> getThumbnail(@PathVariable Integer postId) {
        PostImage thumbnail = postImageService.getThumbnail(postId);
        PostImageResponse imageResponse = mapToResponse(thumbnail);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", imageResponse);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{imageId}/thumbnail")
    @Operation(summary = "Đặt ảnh làm thumbnail")
    public ResponseEntity<?> setThumbnail(
            @PathVariable Integer postId,
            @PathVariable Integer imageId
    ) {
        try {
            PostImage image = postImageService.setThumbnail(postId, imageId);
            PostImageResponse imageResponse = mapToResponse(image);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đặt thumbnail thành công");
            response.put("data", imageResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Xóa ảnh")
    public ResponseEntity<?> deleteImage(
            @PathVariable Integer postId,
            @PathVariable Integer imageId
    ) {
        try {
            postImageService.deleteImage(postId, imageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa ảnh thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping
    @Operation(summary = "Xóa tất cả ảnh của bài đăng")
    public ResponseEntity<?> deleteAllImages(@PathVariable Integer postId) {
        postImageService.deleteAllImages(postId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Xóa tất cả ảnh thành công");
        
        return ResponseEntity.ok(response);
    }

    private PostImageResponse mapToResponse(PostImage image) {
        return PostImageResponse.builder()
                .imageId(image.getImageId())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .isThumbnail(image.getIsThumbnail())
                .build();
    }
}

