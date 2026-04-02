package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.response.OrderResponse;
import swp.be.vn.bs.dto.request.DeliveryScheduleRequest;
import swp.be.vn.bs.entity.*;
import swp.be.vn.bs.repository.DeliverySessionRepository;
import swp.be.vn.bs.repository.OrderRepository;
import swp.be.vn.bs.repository.PostRepository;
import swp.be.vn.bs.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DeliverySessionRepository deliverySessionRepository;
    
    @Autowired
    private WalletService walletService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private PostService postService;

    @Autowired
    private ViolationService violationService;
    
    /**
     * Tạo đơn hàng và đặt cọc 20%
     */
    @Transactional
    public OrderResponse createDeposit(Integer postId, String buyerEmail, String deliveryAddress) {
        // 1. Validate buyer
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new RuntimeException("Buyer not found: " + buyerEmail));
        
        // 2. Validate post availability
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        
        if (!postService.isPostAvailable(postId)) {
            throw new RuntimeException("Post is not available (already reserved or sold)");
        }
        
        // 3. Check không được mua xe của chính mình
        if (post.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new RuntimeException("You cannot buy your own bicycle");
        }
        
        // 4. Check post đã có ACTIVE order chưa (cancelled orders không count)
        if (orderRepository.findByPost_PostIdAndIsActiveTrue(postId).isPresent()) {
            throw new RuntimeException("This post already has an active order");
        }
        
        // 5. Tính deposit (20% giá xe)
        BigDecimal depositAmount = post.getPrice()
                .multiply(new BigDecimal("0.20"))
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal remainingAmount = post.getPrice().subtract(depositAmount);
        
        // 6. Check balance
        if (!walletService.checkBalance(buyer.getUserId(), depositAmount)) {
            throw new RuntimeException(
                String.format("Insufficient balance. Required: %s VND for 20%% deposit", depositAmount)
            );
        }
        
        // 7. Lock tiền trong wallet (chuyển sang lockedBalance)
        walletService.lockBalance(buyer.getUserId(), depositAmount);
        
        // 8. Lock post trong 10 phút
        postService.lockPost(postId, buyer.getUserId(), 10);
        
        // 9. Tạo Order
        Order order = new Order();
        order.setPost(post);
        order.setBuyer(buyer);
        order.setDepositAmount(depositAmount);
        order.setTotalAmount(post.getPrice());
        order.setRemainingAmount(remainingAmount);
        order.setStatus(OrderStatus.DEPOSIT_PAID);
        order.setDeliveryAddress(deliveryAddress);
        
        Order savedOrder = orderRepository.save(order);
        
        // 10. Tạo Transaction record (type=DEPOSIT, số âm vì trừ tiền)
        transactionService.createTransaction(
            walletService.getOrCreateWallet(buyer),
            buyer,
            savedOrder,
            depositAmount.negate(),
            TransactionType.DEPOSIT,
            "Deposit 20% for order #" + savedOrder.getOrderId() + " - " + post.getTitle()
        );
        
        return mapToResponse(savedOrder);
    }
    
    /**
     * Hủy đặt cọc - hoàn tiền lại cho buyer
     */
    @Transactional
    public void cancelDeposit(Integer orderId, String buyerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // 1. Validate buyer ownership
        if (!order.getBuyer().getEmail().equals(buyerEmail)) {
            throw new RuntimeException("You can only cancel your own orders");
        }
        
        // 2. Check status
        if (order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }
        
        // Theo NEW LOGIC: Buyer cancel -> split 99% hoàn buyer, 1% platform fee
        User buyer = order.getBuyer();
        BigDecimal depositAmount = order.getDepositAmount();

        // 3. Mở khóa và trừ tiền cọc của buyer (lockedBalance -> 0)
        walletService.unlockAndDeduct(buyer.getUserId(), depositAmount);

        // 4. Split: 99% cho Buyer, 1% cho Admin (platform fee)
        // Gọi refundPenaltyToBuyer() với toàn bộ deposit amount
        // Hàm này sẽ tự động split 99-1 và deposit cho cả Buyer và Admin
        walletService.refundPenaltyToBuyer(buyer.getUserId(), depositAmount, 
            "Deposit refund (99%) + Platform fee (1%) for cancelled order #" + orderId);

        // 6. Unlock post
        postService.unlockPost(order.getPost().getPostId());

        // 7. Update order status
        order.setStatus(OrderStatus.CANCELLED);
        order.setIsActive(false);  // Mark order as inactive
        order.setCancelledAt(LocalDateTime.now());  // Record when cancelled
        orderRepository.save(order);
        System.out.println("✅ Order #" + orderId + " cancelled by buyer");
        
        // 8. Update post status back to ACTIVE (so seller can sell to others)
        Post post = order.getPost();
        post.setStatus(PostStatus.ACTIVE);
        post.setReservedUntil(null);  // Clear reservation timestamp
        post.setReservedBy(null);     // Clear who reserved it
        postRepository.save(post);
        System.out.println("✅ Post #" + post.getPostId() + " status updated back to ACTIVE");
    }

    /**
     * Seller hủy đơn: refund cọc cho buyer + create violation cho seller
     */
    @Transactional
    public void cancelOrderBySeller(Integer orderId, String sellerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (!order.getPost().getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("You can only cancel orders for your own posts");
        }

        if (order.getStatus() != OrderStatus.DEPOSIT_PAID && order.getStatus() != OrderStatus.IN_DELIVERY) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        User buyer = order.getBuyer();
        User seller = order.getPost().getSeller();
        BigDecimal depositAmount = order.getDepositAmount();

        // 1. Refund cọc cho buyer (lockedBalance -> balance)
        walletService.unlockBalance(buyer.getUserId(), depositAmount);
        
        // 2. Ghi nhận REFUND transaction cho buyer
        transactionService.createTransaction(
            walletService.getOrCreateWallet(buyer),
            buyer,
            order,
            depositAmount,
            TransactionType.REFUND,
            "Refund deposit for seller-cancelled order #" + orderId
        );
        
        // 3. Ghi nhận violation cho seller (chỉ violationCount++, không trừ tiền)
        violationService.recordViolation(seller, order, "SELLER_CANCEL", BigDecimal.ZERO);
        
        // 4. Update status + unlock post
        order.setStatus(OrderStatus.CANCELLED);
        order.setIsActive(false);  // Mark order as inactive
        order.setCancelledAt(LocalDateTime.now());  // Record when cancelled
        orderRepository.save(order);
        System.out.println("✅ Order #" + orderId + " cancelled by seller");
        
        // 5. Update post status back to ACTIVE (so seller can sell to others)
        Post post = order.getPost();
        post.setStatus(PostStatus.ACTIVE);
        post.setReservedUntil(null);  // Clear reservation timestamp
        post.setReservedBy(null);     // Clear who reserved it
        postRepository.save(post);
        System.out.println("✅ Post #" + post.getPostId() + " status updated back to ACTIVE");
    }

    /**
     * Auto-cancel orders quá 10 phút chưa confirm (scheduler)
     * Rule: system auto-cancel -> refund cọc cho buyer, unlock post, cancel order
     */
    @Transactional
    public int autoCancelExpiredDeposits(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minus(minutes, ChronoUnit.MINUTES);
        List<Order> expired = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.DEPOSIT_PAID, cutoff);

        int cancelled = 0;
        for (Order order : expired) {
            // Nếu post còn đang reserved và đã hết hạn thì mới auto-cancel
            Post post = order.getPost();
            if (post.getReservedUntil() == null || post.getReservedUntil().isAfter(LocalDateTime.now())) {
                continue;
            }

            // Refund buyer deposit
            User buyer = order.getBuyer();
            BigDecimal depositAmount = order.getDepositAmount();
            walletService.unlockBalance(buyer.getUserId(), depositAmount);

            // Unlock post & cancel order
            postService.unlockPost(post.getPostId());
            order.setStatus(OrderStatus.CANCELLED);
            order.setIsActive(false);  // Mark order as inactive
            order.setCancelledAt(LocalDateTime.now());  // Record when cancelled
            orderRepository.save(order);
            System.out.println("✅ Order #" + order.getOrderId() + " auto-cancelled (expired)");
            
            // Update post status back to ACTIVE (so seller can sell to others)
            post.setStatus(PostStatus.ACTIVE);
            post.setReservedUntil(null);  // Clear reservation timestamp
            post.setReservedBy(null);     // Clear who reserved it
            postRepository.save(post);
            System.out.println("✅ Post #" + post.getPostId() + " status updated back to ACTIVE");

            transactionService.createTransaction(
                walletService.getOrCreateWallet(buyer),
                buyer,
                order,
                depositAmount,
                TransactionType.REFUND,
                "Auto-cancel expired deposit for order #" + order.getOrderId()
            );

            cancelled++;
        }

        return cancelled;
    }
    
    /**
     * Hoàn tất đơn hàng - buyer trả 80% còn lại, seller nhận tiền
     */
    @Transactional
    public OrderResponse completeOrder(Integer orderId, Integer verifiedBy) {
        // 1. Validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // 2. Check status - phải đã schedule delivery hoặc deposit paid
        if (order.getStatus() != OrderStatus.IN_DELIVERY && order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new RuntimeException("Order must be in IN_DELIVERY or DEPOSIT_PAID status. Current: " + order.getStatus());
        }
        
        // 3. Get parties
        User buyer = order.getBuyer();
        User seller = order.getPost().getSeller();
        BigDecimal totalPrice = order.getTotalAmount();
        BigDecimal depositPaid = order.getDepositAmount();
        BigDecimal remainingAmount = order.getRemainingAmount();
        
        // 4. Buyer trả 80% còn lại
        if (!walletService.checkBalance(buyer.getUserId(), remainingAmount)) {
            throw new RuntimeException(
                String.format("Buyer insufficient balance for remaining payment. Required: %s VND", remainingAmount)
            );
        }
        
        walletService.chargePayment(buyer.getUserId(), remainingAmount, 
            "Final payment (80%) for order #" + orderId);
        
        // 5. Unlock và trừ deposit của buyer (chuyển từ lockedBalance → 0, không hoàn lại)
        walletService.unlockAndDeduct(buyer.getUserId(), depositPaid);
        
        // 6. Seller nhận = totalPrice (postFee đã được trừ từ seller khi tạo post)
        // Note: PostFee được tạo riêng ở createPost() bằng chargeFee()
        // Không được trừ lại ở đây, vì phí đã là sẵn của seller
        walletService.deposit(seller.getUserId(), totalPrice);
        
        // 8. Update order status
        order.setStatus(OrderStatus.COMPLETED);
        order.setSellerConfirmedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        System.out.println("✅ Order #" + orderId + " status updated to COMPLETED");
        
        // 9. Update post status to SOLD (after buyer pays remaining 80%)
        Post post = order.getPost();
        post.setStatus(PostStatus.SOLD);
        post.setReservedUntil(null);  // Clear reservation timestamp
        post.setReservedBy(null);     // Clear who reserved it
        Post savedPost = postRepository.save(post);
        System.out.println("✅ Post #" + savedPost.getPostId() + " status updated to SOLD");
        
        // 10. Update delivery status nếu có
        deliverySessionRepository.findByOrder_OrderId(orderId).ifPresent(delivery -> {
            delivery.setStatus(DeliveryStatus.COMPLETED);
            deliverySessionRepository.save(delivery);
            System.out.println("✅ Delivery session updated to COMPLETED");
        });
        
        System.out.println("✅ Order completion transaction finished successfully");
        return mapToResponse(savedOrder);
    }
    
    /**
     * Lấy đơn hàng của buyer (đơn mua)
     */
    public List<OrderResponse> getMyOrders(String buyerEmail) {
        return orderRepository.findByBuyer_Email(buyerEmail).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy đơn bán của seller
     */
    public List<OrderResponse> getMySales(String sellerEmail) {
        return orderRepository.findByPost_Seller_Email(sellerEmail).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy chi tiết order
     */
    public OrderResponse getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        return mapToResponse(order);
    }
    
    /**
     * Map Order entity to OrderResponse DTO
     */
    public OrderResponse mapToResponse(Order order) {
        Post post = order.getPost();
        
        OrderResponse.BuyerInfo buyerInfo = OrderResponse.BuyerInfo.builder()
                .userId(order.getBuyer().getUserId())
                .email(order.getBuyer().getEmail())
                .fullName(order.getBuyer().getFullName())
                .phone(order.getBuyer().getPhone())
                .build();
        
        OrderResponse.SellerInfo sellerInfo = OrderResponse.SellerInfo.builder()
                .userId(post.getSeller().getUserId())
                .email(post.getSeller().getEmail())
                .fullName(post.getSeller().getFullName())
                .phone(post.getSeller().getPhone())
                .build();
        
        OrderResponse.InspectorInfo inspectorInfo = null;
        if (order.getAssignedInspector() != null) {
            inspectorInfo = OrderResponse.InspectorInfo.builder()
                    .userId(order.getAssignedInspector().getUserId())
                    .email(order.getAssignedInspector().getEmail())
                    .fullName(order.getAssignedInspector().getFullName())
                    .build();
        }
        
        OrderResponse.PostInfo postInfo = OrderResponse.PostInfo.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .price(post.getPrice())
                .postFee(post.getPostFee())
                .build();
        
        // Build DeliverySessionInfo if exists
        OrderResponse.DeliverySessionInfo deliverySessionInfo = null;
        if (order.getDeliverySession() != null) {
            DeliverySession session = order.getDeliverySession();
            deliverySessionInfo = OrderResponse.DeliverySessionInfo.builder()
                    .sessionId(session.getSessionId())
                    .deliveryDate(session.getDeliveryDate() != null ? 
                            session.getDeliveryDate().atTime(session.getStartTime()) : null)
                    .startTime(session.getStartTime() != null ?
                            session.getDeliveryDate().atTime(session.getStartTime()) : null)
                    .endTime(session.getEndTime() != null ?
                            session.getDeliveryDate().atTime(session.getEndTime()) : null)
                    .location(session.getLocation())
                    .deliveryStatus(session.getStatus() != null ? session.getStatus().toString() : null)
                    .assignedAt(session.getAssignedAt())
                    .deliveredAt(session.getDeliveredAt())
                    .build();
        }
        
        LocalDateTime expiresAt = null;
        if (post.getReservedUntil() != null) {
            expiresAt = post.getReservedUntil();
        }
        
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .postId(post.getPostId())
                .postTitle(post.getTitle())
                .depositAmount(order.getDepositAmount())
                .totalAmount(order.getTotalAmount())
                .remainingAmount(order.getRemainingAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .expiresAt(expiresAt)
                .pickupAddress(order.getPickupAddress())
                .deliveryAddress(order.getDeliveryAddress())
                .sellerConfirmedAt(order.getSellerConfirmedAt())
                .adminReviewedAt(order.getAdminReviewedAt())
                .buyer(buyerInfo)
                .seller(sellerInfo)
                .assignedInspector(inspectorInfo)
                .post(postInfo)
                .deliverySession(deliverySessionInfo)
                .build();

    }


    //1. Seller bao cao : Buyer khong den
    @Transactional
    public void reportBuyerNoShow(Integer orderId, String sellerEmail){
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if(order.getStatus() != OrderStatus.DEPOSIT_PAID && order.getStatus() != OrderStatus.IN_DELIVERY){
            throw new RuntimeException("Cannot report an order in status : " + order.getStatus());
        }

        User buyer = order.getBuyer();
        BigDecimal depositAmount = order.getDepositAmount();

        // Mở khóa và trừ tiền cọc của buyer
        walletService.unlockAndDeduct(buyer.getUserId(), depositAmount);
        
        // PENALTY: Ghi nhận transaction type PENALTY cho buyer
        transactionService.createTransaction(
            walletService.getOrCreateWallet(buyer),
            buyer,
            order,
            depositAmount.negate(),
            TransactionType.PENALTY,
            "Buyer no-show for order #" + orderId + " - deposit forfeited to platform"
        );
        
        // Chuyển deposit của buyer no-show tới admin account
        walletService.transferFeeToAdmin(depositAmount, 
            "Penalty - Buyer no-show for order #" + orderId);

        // Ghi nhận violation cho buyer
        violationService.recordViolation(buyer, order, "BUYER_NO_SHOW", depositAmount);

        // Xử lý trạng thái order và post: hủy đơn, mở lại bài đăng
        order.setStatus(OrderStatus.CANCELLED);
        order.setIsActive(false);  // Mark order as inactive
        order.setCancelledAt(LocalDateTime.now());  // Record when cancelled
        orderRepository.save(order);
        
        // Update post status back to ACTIVE (so seller can sell to others)
        Post post = order.getPost();
        post.setStatus(PostStatus.ACTIVE);
        post.setReservedUntil(null);  // Clear reservation timestamp
        post.setReservedBy(null);     // Clear who reserved it
        postRepository.save(post);
        System.out.println("✅ Post #" + post.getPostId() + " status updated back to ACTIVE");
        
        postService.unlockPost(order.getPost().getPostId());
    }

    // 2. Buyer bao cao : Seller khong den
    @Transactional
    public void reportSellerNoShow(Integer orderId, String buyerEmail){
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

         if(order.getStatus() != OrderStatus.DEPOSIT_PAID && order.getStatus() != OrderStatus.IN_DELIVERY){
             throw new RuntimeException("Cannot report an order in status : " + order.getStatus());
         }

         User buyer = order.getBuyer();
         User seller = order.getPost().getSeller();
         BigDecimal depositAmount = order.getDepositAmount();

         // Hoan tra 100% tien co lai cho Buyer
        walletService.unlockBalance(buyer.getUserId(), depositAmount);

        // Ghi nhan vi pham cho seller
        violationService.recordViolation(seller, order, "SELLER_NO_SHOW", BigDecimal.ZERO);

        // Huy don va mo bai lai
        order.setStatus(OrderStatus.CANCELLED);
        order.setIsActive(false);  // Mark order as inactive
        order.setCancelledAt(LocalDateTime.now());  // Record when cancelled
        orderRepository.save(order);
        
        // Update post status back to ACTIVE (so seller can sell to others)
        Post post = order.getPost();
        post.setStatus(PostStatus.ACTIVE);
        post.setReservedUntil(null);  // Clear reservation timestamp
        post.setReservedBy(null);     // Clear who reserved it
        postRepository.save(post);
        System.out.println("✅ Post #" + post.getPostId() + " status updated back to ACTIVE");
        
        postService.unlockPost(order.getPost().getPostId());
    }

    /**
     * Seller schedule delivery with pickup address
     */
    @Transactional
    public OrderResponse scheduleDelivery(Integer orderId, DeliveryScheduleRequest request, String sellerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Verify seller ownership
        if (!order.getPost().getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("Only seller can schedule delivery");
        }

        // Validate request
        if (request.getPickupAddress() == null || request.getPickupAddress().trim().isEmpty()) {
            throw new RuntimeException("Pickup address is required");
        }
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
            throw new RuntimeException("Delivery address is required");
        }
        if (request.getDeliveryTime() == null) {
            throw new RuntimeException("Delivery time is required");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new RuntimeException("Order must be in DEPOSIT_PAID status to schedule delivery");
        }

        // Update order with addresses
        order.setPickupAddress(request.getPickupAddress());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setStatus(OrderStatus.PENDING_SELLER_CONFIRMATION);
        order.setSellerConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Create or update delivery session
        DeliverySession session = order.getDeliverySession();
        if (session == null) {
            session = new DeliverySession();
            session.setOrder(order);
        }
        session.setLocation(request.getPickupAddress());  // Set pickup location
        session.setDeliveryDate(request.getDeliveryTime().toLocalDate());
        session.setStartTime(request.getDeliveryTime().toLocalTime());
        session.setStatus(DeliveryStatus.PENDING_INSPECTOR_ASSIGNMENT);
        session.setAssignedAt(LocalDateTime.now());
        deliverySessionRepository.save(session);

        return mapToResponse(order);
    }

    /**
     * Seller confirm delivery address
     */
    @Transactional
    public OrderResponse sellerConfirmDelivery(Integer orderId, DeliveryScheduleRequest request, String sellerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Verify seller ownership
        if (!order.getPost().getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException("Only seller can confirm delivery");
        }

        // Validate request
        if (request.getPickupAddress() == null || request.getPickupAddress().trim().isEmpty()) {
            throw new RuntimeException("Pickup address is required");
        }
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
            throw new RuntimeException("Delivery address is required");
        }
        if (request.getDeliveryTime() == null) {
            throw new RuntimeException("Delivery time is required");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new RuntimeException("Order must be in DEPOSIT_PAID status to confirm delivery");
        }

        // Update order with addresses
        order.setPickupAddress(request.getPickupAddress());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setStatus(OrderStatus.PENDING_SELLER_CONFIRMATION);
        order.setSellerConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Create or update delivery session
        DeliverySession session = order.getDeliverySession();
        if (session == null) {
            session = new DeliverySession();
            session.setOrder(order);
        }
        session.setLocation(request.getPickupAddress());  // Set pickup location
        session.setDeliveryDate(request.getDeliveryTime().toLocalDate());
        session.setStartTime(request.getDeliveryTime().toLocalTime());
        session.setStatus(DeliveryStatus.PENDING_INSPECTOR_ASSIGNMENT);
        session.setAssignedAt(LocalDateTime.now());
        deliverySessionRepository.save(session);

        return mapToResponse(order);
    }

    /**
     * Admin assign inspector to delivery
     */
    @Transactional
    public OrderResponse adminAssignInspector(Integer orderId, Integer inspectorId, String adminEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Verify admin exists (could add role check here)
        userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminEmail));

        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> new RuntimeException("Inspector not found with ID: " + inspectorId));

        // Check if inspector has INSPECTOR role
        if (!inspector.getRole().equals(Role.INSPECTOR)) {
            throw new RuntimeException("User is not an inspector");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.PENDING_SELLER_CONFIRMATION &&
            order.getStatus() != OrderStatus.PENDING_ADMIN_REVIEW) {
            throw new RuntimeException("Order cannot be assigned in current status: " + order.getStatus());
        }

        // Update order
        order.setStatus(OrderStatus.ASSIGNED_TO_INSPECTOR);
        order.setAssignedInspector(inspector);
        order.setAdminReviewedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Update delivery session
        deliverySessionRepository.findByOrder_OrderId(orderId).ifPresent(delivery -> {
            delivery.setInspector(inspector);
            delivery.setStatus(DeliveryStatus.ASSIGNED_TO_INSPECTOR);
            delivery.setAssignedAt(LocalDateTime.now());
            deliverySessionRepository.save(delivery);
        });

        return mapToResponse(order);
    }

    /**
     * Inspector start delivery (mark as IN_DELIVERY)
     */
    @Transactional
    public OrderResponse inspectorStartDelivery(Integer orderId, String inspectorEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found: " + inspectorEmail));

        // Verify inspector ownership
        if (!order.getAssignedInspector().getUserId().equals(inspector.getUserId())) {
            throw new RuntimeException("Only assigned inspector can start delivery");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.ASSIGNED_TO_INSPECTOR) {
            throw new RuntimeException("Order must be ASSIGNED_TO_INSPECTOR to start delivery");
        }

        // Update order status to IN_DELIVERY (lúc này inspector bắt đầu giao)
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);

        // Update delivery session status
        deliverySessionRepository.findByOrder_OrderId(orderId).ifPresent(delivery -> {
            delivery.setStatus(DeliveryStatus.ASSIGNED_TO_INSPECTOR); // Vẫn là ASSIGNED, chưa DELIVERED
            deliverySessionRepository.save(delivery);
        });

        return mapToResponse(order);
    }

    /**
     * Inspector mark delivery as completed
     */
    @Transactional
    public OrderResponse inspectorMarkDelivered(Integer orderId, String inspectorEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found: " + inspectorEmail));

        // Verify inspector ownership
        if (!order.getAssignedInspector().getUserId().equals(inspector.getUserId())) {
            throw new RuntimeException("Only assigned inspector can mark as delivered");
        }

        // Check order status - phải đang IN_DELIVERY
        if (order.getStatus() != OrderStatus.IN_DELIVERY) {
            throw new RuntimeException("Order must be IN_DELIVERY to mark as delivered");
        }

        // Update delivery session status to DELIVERED (inspector confirms giao hàng xong)
        // ⚠️ IMPORTANT: Không set order = COMPLETED ở đây!
        // Buyer sẽ gọi /complete để finalize order
        deliverySessionRepository.findByOrder_OrderId(orderId).ifPresent(delivery -> {
            delivery.setStatus(DeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(LocalDateTime.now());
            deliverySessionRepository.save(delivery);
            System.out.println("✅ Delivery session marked as DELIVERED by inspector");
        });

        System.out.println("✅ Inspector #" + orderId + " marked delivery complete (waiting for buyer to confirm)");
        System.out.println("⏳ Order still IN_DELIVERY - buyer must call /complete to finalize");

        return mapToResponse(order);
    }

    /**
     * Inspector lấy danh sách delivery tasks được assign cho chính mình
     */
    public List<OrderResponse> getMyDeliveryTasks(String inspectorEmail) {
        User inspector = userRepository.findByEmail(inspectorEmail)
                .orElseThrow(() -> new RuntimeException("Inspector not found: " + inspectorEmail));

        // Check inspector role
        if (!inspector.getRole().equals(Role.INSPECTOR)) {
            throw new RuntimeException("User is not an inspector");
        }

        // Get delivery tasks using optimized custom query
        List<OrderStatus> statuses = List.of(
                OrderStatus.ASSIGNED_TO_INSPECTOR, 
                OrderStatus.IN_DELIVERY,
                OrderStatus.COMPLETED
        );
        List<Order> orders = orderRepository.findByAssignedInspector_UserIdAndStatusIn(
                inspector.getUserId(), 
                statuses
        );

        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}

