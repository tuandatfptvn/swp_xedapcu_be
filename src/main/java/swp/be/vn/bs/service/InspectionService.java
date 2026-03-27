package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.request.InspectionBookingRequest;
import swp.be.vn.bs.dto.request.InspectionReportRequest;
import swp.be.vn.bs.dto.response.InspectionBookingResponse;
import swp.be.vn.bs.dto.response.InspectionReportResponse;
import swp.be.vn.bs.dto.response.InspectionRequestResponse;
import swp.be.vn.bs.entity.*;
import swp.be.vn.bs.repository.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InspectionService {
    
    @Autowired
    private InspectionBookingRepository bookingRepository;
    
    @Autowired
    private InspectionRequestRepository requestRepository;
    
    @Autowired
    private InspectionReportRepository reportRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WalletService walletService;
    
    // Default inspection fee (có thể config)
    private static final BigDecimal DEFAULT_INSPECTION_FEE = new BigDecimal("500000"); // 500k VND
    
    /**
     * 3.1.1 - Buyer/Seller yêu cầu kiểm định xe
     */
    @Transactional
    public InspectionBookingResponse createBooking(InspectionBookingRequest request, String requesterEmail) {
        // 1. Validate requester
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + requesterEmail));
        
        // 2. Validate post
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + request.getPostId()));
        
        // 3. Check xem requester có quyền request không (phải là seller hoặc có order với post này)
        boolean isSeller = post.getSeller().getUserId().equals(requester.getUserId());
        // TODO: Check if requester is buyer with active order
        
        if (!isSeller) {
            throw new RuntimeException("Only the seller or potential buyers can request inspection for this post");
        }
        
        // 4. Check xem post đã có booking active chưa
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        if (bookingRepository.existsByPost_PostIdAndStatusIn(request.getPostId(), activeStatuses)) {
            throw new RuntimeException("This post already has an active inspection booking");
        }
        
        // 5. Validate booking time (phải trong tương lai)
        if (request.getBookingDate().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("Booking date must be in the future");
        }
        
        // 6. Tạo InspectionBooking
        InspectionBooking booking = new InspectionBooking();
        booking.setPost(post);
        // Note: inspector sẽ được assign sau bởi admin/system
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setLocation(request.getLocation());
        booking.setStatus(BookingStatus.PENDING); // Chờ assign inspector
        
        InspectionBooking savedBooking = bookingRepository.save(booking);
        
        return mapBookingToResponse(savedBooking, requester);
    }
    
    /**
     * 3.1.2 - Admin assign inspector cho booking
     */
    @Transactional
    public InspectionBookingResponse assignInspector(Integer bookingId, Integer inspectorId, String adminEmail) {
        // 1. Validate admin
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can assign inspector");
        }
        
        // 2. Validate booking
        InspectionBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Can only assign inspector to PENDING bookings");
        }
        
        // 3. Validate inspector
        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> new RuntimeException("Inspector not found with ID: " + inspectorId));
        
        if (inspector.getRole() != Role.INSPECTOR) {
            throw new RuntimeException("User is not an inspector");
        }
        
        // 4. Check inspector availability (không có booking trùng giờ)
        List<InspectionBooking> inspectorBookings = bookingRepository
                .findByInspector_UserIdAndBookingDate(inspectorId, booking.getBookingDate());
        
        for (InspectionBooking existingBooking : inspectorBookings) {
            if (isTimeOverlap(booking, existingBooking)) {
                throw new RuntimeException("Inspector has another booking at this time");
            }
        }
        
        // 5. Assign inspector
        booking.setInspector(inspector);
        // Status vẫn là PENDING, chờ inspector confirm
        
        InspectionBooking updatedBooking = bookingRepository.save(booking);
        
        return mapBookingToResponse(updatedBooking, booking.getPost().getSeller());
    }
    
    /**
     * 3.1.3 - Inspector confirm booking và tạo InspectionRequest
     */
    @Transactional
    public InspectionRequestResponse confirmBooking(Integer bookingId, String inspectorEmail) {
        // 1. Validate inspector
        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found"));
        
        if (inspector.getRole() != Role.INSPECTOR) {
            throw new RuntimeException("Only inspector can confirm booking");
        }
        
        // 2. Validate booking
        InspectionBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        
        if (!booking.getInspector().getUserId().equals(inspector.getUserId())) {
            throw new RuntimeException("You can only confirm your own bookings");
        }
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Can only confirm PENDING bookings");
        }
        
        // 3. Check xem đã có InspectionRequest chưa
        if (requestRepository.findByBooking_BookingId(bookingId).isPresent()) {
            throw new RuntimeException("This booking already has an inspection request");
        }
        
        // 4. Update booking status
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        
        // 5. Tạo InspectionRequest
        InspectionRequest inspectionRequest = new InspectionRequest();
        inspectionRequest.setBooking(booking);
        inspectionRequest.setPost(booking.getPost());
        inspectionRequest.setInspector(inspector);
        inspectionRequest.setInspectionFee(DEFAULT_INSPECTION_FEE);
        inspectionRequest.setPaidBy(PaidBy.SELLER); // Default: seller trả phí
        inspectionRequest.setStatus(InspectionStatus.PENDING);
        
        InspectionRequest savedRequest = requestRepository.save(inspectionRequest);
        
        return mapRequestToResponse(savedRequest);
    }
    
    /**
     * 3.2.1 - Inspector submit báo cáo kiểm định
     */
    @Transactional
    public InspectionReportResponse submitReport(
            Integer inspectionId, 
            InspectionReportRequest request, 
            String inspectorEmail) {
        
        // 1. Validate inspector
        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found"));
        
        if (inspector.getRole() != Role.INSPECTOR) {
            throw new RuntimeException("Only inspector can submit report");
        }
        
        // 2. Validate inspection request
        InspectionRequest inspectionRequest = requestRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection request not found with ID: " + inspectionId));
        
        if (!inspectionRequest.getInspector().getUserId().equals(inspector.getUserId())) {
            throw new RuntimeException("You can only submit report for your own inspections");
        }
        
        if (inspectionRequest.getStatus() == InspectionStatus.COMPLETED) {
            throw new RuntimeException("This inspection already has a report");
        }
        
        // 3. Check xem đã có report chưa
        if (reportRepository.existsByInspection_InspectionId(inspectionId)) {
            throw new RuntimeException("Report already exists for this inspection");
        }
        
        // 4. Tạo InspectionReport
        InspectionReport report = new InspectionReport();
        report.setInspection(inspectionRequest);
        report.setFrameStatus(request.getFrameStatus());
        report.setBrakeStatus(request.getBrakeStatus());
        report.setDrivetrainStatus(request.getDrivetrainStatus());
        report.setOverallRating(request.getOverallRating());
        report.setReportFileUrl(request.getReportFileUrl());
        
        InspectionReport savedReport = reportRepository.save(report);
        
        // 5. Charge inspection fee từ người request (seller/buyer)
        Post post = inspectionRequest.getPost();
        User payer = (inspectionRequest.getPaidBy() == PaidBy.SELLER) 
                ? post.getSeller() 
                : null; // TODO: Get buyer if BUYER pays
        
        if (payer == null) {
            throw new RuntimeException("Cannot determine who should pay the inspection fee");
        }
        
        // Check balance và charge fee
        BigDecimal fee = inspectionRequest.getInspectionFee();
        if (!walletService.checkBalance(payer.getUserId(), fee)) {
            throw new RuntimeException(
                String.format("Insufficient balance to pay inspection fee. Required: %s VND", fee)
            );
        }
        
        walletService.chargeFee(payer.getUserId(), fee, 
            "Inspection fee for post: " + post.getTitle(),
            TransactionType.INSPECTION_FEE);
        
        // 6. Update inspection request status
        inspectionRequest.setStatus(InspectionStatus.COMPLETED);
        requestRepository.save(inspectionRequest);
        
        // 7. Update booking status
        InspectionBooking booking = inspectionRequest.getBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        
        // 8. Mark post as "Inspected" → tăng trust score
        post.setIsInspected(true);
        postRepository.save(post);
        
        return mapReportToResponse(savedReport);
    }
    
    /**
     * Lấy danh sách bookings (for admin/inspector)
     */
    public List<InspectionBookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(booking -> mapBookingToResponse(booking, booking.getPost().getSeller()))
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy bookings của inspector
     */
    public List<InspectionBookingResponse> getInspectorBookings(String inspectorEmail) {
        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found"));
        
        return bookingRepository.findByInspector_UserId(inspector.getUserId()).stream()
                .map(booking -> mapBookingToResponse(booking, booking.getPost().getSeller()))
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy inspection requests của inspector
     */
    public List<InspectionRequestResponse> getInspectorRequests(String inspectorEmail) {
        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found"));
        
        return requestRepository.findByInspector_UserId(inspector.getUserId()).stream()
                .map(this::mapRequestToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy report theo inspection ID
     */
    public InspectionReportResponse getReport(Integer inspectionId) {
        InspectionReport report = reportRepository.findByInspection_InspectionId(inspectionId)
                .orElseThrow(() -> new RuntimeException("Report not found for inspection ID: " + inspectionId));
        
        return mapReportToResponse(report);
    }
    
    /**
     * Lấy inspection history của post
     */
    public List<InspectionRequestResponse> getPostInspectionHistory(Integer postId) {
        return requestRepository.findByPost_PostId(postId).stream()
                .map(this::mapRequestToResponse)
                .collect(Collectors.toList());
    }
    
    // ==================== HELPER METHODS ====================
    
    private boolean isTimeOverlap(InspectionBooking booking1, InspectionBooking booking2) {
        // Check if time ranges overlap
        return !(booking1.getEndTime().isBefore(booking2.getStartTime()) || 
                 booking1.getStartTime().isAfter(booking2.getEndTime()));
    }
    
    private InspectionBookingResponse mapBookingToResponse(InspectionBooking booking, User requester) {
        InspectionBookingResponse.RequesterInfo requesterInfo = InspectionBookingResponse.RequesterInfo.builder()
                .userId(requester.getUserId())
                .email(requester.getEmail())
                .fullName(requester.getFullName())
                .build();
        
        InspectionBookingResponse.InspectorInfo inspectorInfo = null;
        if (booking.getInspector() != null) {
            inspectorInfo = InspectionBookingResponse.InspectorInfo.builder()
                    .userId(booking.getInspector().getUserId())
                    .email(booking.getInspector().getEmail())
                    .fullName(booking.getInspector().getFullName())
                    .build();
        }
        
        return InspectionBookingResponse.builder()
                .bookingId(booking.getBookingId())
                .postId(booking.getPost().getPostId())
                .postTitle(booking.getPost().getTitle())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .location(booking.getLocation())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .requester(requesterInfo)
                .inspector(inspectorInfo)
                .build();
    }
    
    private InspectionRequestResponse mapRequestToResponse(InspectionRequest request) {
        InspectionRequestResponse.InspectorInfo inspectorInfo = InspectionRequestResponse.InspectorInfo.builder()
                .userId(request.getInspector().getUserId())
                .email(request.getInspector().getEmail())
                .fullName(request.getInspector().getFullName())
                .build();
        
        InspectionRequestResponse.ReportInfo reportInfo = null;
        InspectionReport report = reportRepository.findByInspection_InspectionId(request.getInspectionId()).orElse(null);
        if (report != null) {
            reportInfo = InspectionRequestResponse.ReportInfo.builder()
                    .reportId(report.getReportId())
                    .frameStatus(report.getFrameStatus())
                    .brakeStatus(report.getBrakeStatus())
                    .drivetrainStatus(report.getDrivetrainStatus())
                    .overallRating(report.getOverallRating())
                    .reportFileUrl(report.getReportFileUrl())
                    .verifiedAt(report.getVerifiedAt())
                    .build();
        }
        
        return InspectionRequestResponse.builder()
                .inspectionId(request.getInspectionId())
                .bookingId(request.getBooking().getBookingId())
                .postId(request.getPost().getPostId())
                .postTitle(request.getPost().getTitle())
                .inspectionFee(request.getInspectionFee())
                .paidBy(request.getPaidBy())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .inspector(inspectorInfo)
                .report(reportInfo)
                .build();
    }
    
    private InspectionReportResponse mapReportToResponse(InspectionReport report) {
        InspectionReportResponse.InspectorInfo inspectorInfo = InspectionReportResponse.InspectorInfo.builder()
                .userId(report.getInspection().getInspector().getUserId())
                .email(report.getInspection().getInspector().getEmail())
                .fullName(report.getInspection().getInspector().getFullName())
                .build();
        
        return InspectionReportResponse.builder()
                .reportId(report.getReportId())
                .inspectionId(report.getInspection().getInspectionId())
                .postId(report.getInspection().getPost().getPostId())
                .postTitle(report.getInspection().getPost().getTitle())
                .frameStatus(report.getFrameStatus())
                .brakeStatus(report.getBrakeStatus())
                .drivetrainStatus(report.getDrivetrainStatus())
                .overallRating(report.getOverallRating())
                .reportFileUrl(report.getReportFileUrl())
                .verifiedAt(report.getVerifiedAt())
                .inspector(inspectorInfo)
                .build();
    }
}
