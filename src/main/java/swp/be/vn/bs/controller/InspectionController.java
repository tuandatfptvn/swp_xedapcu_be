package swp.be.vn.bs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp.be.vn.bs.dto.request.InspectionBookingRequest;
import swp.be.vn.bs.dto.request.InspectionReportRequest;
import swp.be.vn.bs.dto.response.InspectionBookingResponse;
import swp.be.vn.bs.dto.response.InspectionReportResponse;
import swp.be.vn.bs.dto.response.InspectionRequestResponse;
import swp.be.vn.bs.service.InspectionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Inspection Workflow
 * Quản lý việc kiểm định xe trước khi giao dịch
 */
@RestController
@RequestMapping("/api/inspections")
@CrossOrigin(origins = "*")
public class InspectionController {
    
    @Autowired
    private InspectionService inspectionService;
    
    // ==================== BOOKING ENDPOINTS ====================
    
    /**
     * POST /api/inspections/bookings
     * Buyer/Seller yêu cầu kiểm định xe
     */
    @PostMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createBooking(
            @RequestBody InspectionBookingRequest request,
            Authentication authentication) {
        try {
            String requesterEmail = authentication.getName();
            InspectionBookingResponse response = inspectionService.createBooking(request, requesterEmail);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "✅ Inspection booking created successfully! Waiting for inspector assignment.");
            result.put("booking", response);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create booking: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/inspections/bookings/{bookingId}/assign
     * Admin assign inspector cho booking
     */
    @PutMapping("/bookings/{bookingId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignInspector(
            @PathVariable Integer bookingId,
            @RequestParam Integer inspectorId,
            Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            InspectionBookingResponse response = inspectionService.assignInspector(
                    bookingId, inspectorId, adminEmail);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "✅ Inspector assigned successfully!");
            result.put("booking", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to assign inspector: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/inspections/bookings/{bookingId}/confirm
     * Inspector confirm booking → tạo InspectionRequest
     */
    @PostMapping("/bookings/{bookingId}/confirm")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> confirmBooking(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        try {
            String inspectorEmail = authentication.getName();
            InspectionRequestResponse response = inspectionService.confirmBooking(
                    bookingId, inspectorEmail);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "✅ Booking confirmed! Inspection request created.");
            result.put("inspectionRequest", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to confirm booking: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/inspections/bookings
     * Lấy danh sách bookings (for admin)
     */
    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings() {
        try {
            List<InspectionBookingResponse> bookings = inspectionService.getAllBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bookings: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/inspections/bookings/my-bookings
     * Inspector lấy danh sách bookings của mình
     */
    @GetMapping("/bookings/my-bookings")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> getMyBookings(Authentication authentication) {
        try {
            String inspectorEmail = authentication.getName();
            List<InspectionBookingResponse> bookings = inspectionService.getInspectorBookings(inspectorEmail);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bookings: " + e.getMessage()));
        }
    }
    
    // ==================== INSPECTION REQUEST ENDPOINTS ====================
    
    /**
     * GET /api/inspections/requests/my-requests
     * Inspector lấy danh sách inspection requests của mình
     */
    @GetMapping("/requests/my-requests")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> getMyRequests(Authentication authentication) {
        try {
            String inspectorEmail = authentication.getName();
            List<InspectionRequestResponse> requests = inspectionService.getInspectorRequests(inspectorEmail);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch requests: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/inspections/posts/{postId}/history
     * Lấy lịch sử kiểm định của post (public)
     */
    @GetMapping("/posts/{postId}/history")
    public ResponseEntity<?> getPostInspectionHistory(@PathVariable Integer postId) {
        try {
            List<InspectionRequestResponse> history = inspectionService.getPostInspectionHistory(postId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch history: " + e.getMessage()));
        }
    }
    
    // ==================== REPORT ENDPOINTS ====================
    
    /**
     * POST /api/inspections/{inspectionId}/report
     * Inspector submit báo cáo kiểm định
     */
    @PostMapping("/{inspectionId}/report")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> submitReport(
            @PathVariable Integer inspectionId,
            @RequestBody InspectionReportRequest request,
            Authentication authentication) {
        try {
            String inspectorEmail = authentication.getName();
            InspectionReportResponse response = inspectionService.submitReport(
                    inspectionId, request, inspectorEmail);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "✅ Inspection report submitted successfully! Post marked as inspected.");
            result.put("report", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to submit report: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/inspections/{inspectionId}/report
     * Lấy báo cáo kiểm định (public - để buyer xem trước khi mua)
     */
    @GetMapping("/{inspectionId}/report")
    public ResponseEntity<?> getReport(@PathVariable Integer inspectionId) {
        try {
            InspectionReportResponse report = inspectionService.getReport(inspectionId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Report not found: " + e.getMessage()));
        }
    }
}
