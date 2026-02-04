package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.BicycleRequest;
import swp.be.vn.bs.dto.response.BicycleRespond;
import swp.be.vn.bs.service.BicycleService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bicycles")
@CrossOrigin(origins = "*")
public class BicycleController {

    @Autowired
    private BicycleService bicycleService;

    @GetMapping
    public ResponseEntity<List<BicycleRespond>> getAllBicycles() {
        return ResponseEntity.ok(bicycleService.getAllBicycles());
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<?> createBicycle(@RequestBody BicycleRequest bicycleRequest) {
        try {
            BicycleRespond savedBicycle = bicycleService.createBicycle(bicycleRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Bicycle info created successfully!");
            response.put("bicycle", savedBicycle);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi tạo xe: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<?> deleteBicycle(@PathVariable Integer id) {
        try {
            bicycleService.deleteBicycle(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Bicycle deleted successfully!");
            response.put("id", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi xóa: " + e.getMessage());
        }
    }
}