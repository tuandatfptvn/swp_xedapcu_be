package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.response.OrderResponse;
import swp.be.vn.bs.entity.*;
import swp.be.vn.bs.repository.DeliverySessionRepository;
import swp.be.vn.bs.repository.OrderRepository;
import swp.be.vn.bs.repository.PostRepository;
import swp.be.vn.bs.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    
    /**
     * Tạo đơn hàng và đặt cọc 20%
     */
    @Transactional
    public OrderResponse createDeposit(Integer postId, String buyerEmail) {
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
        
        // 4. Check post đã có order chưa
        if (orderRepository.findByPost_PostId(postId).isPresent()) {
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
        
        // 3. Unlock tiền (chuyển từ lockedBalance → balance)
        walletService.unlockBalance(order.getBuyer().getUserId(), order.getDepositAmount());
        
        // 4. Unlock post
        postService.unlockPost(order.getPost().getPostId());
        
        // 5. Update order status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // 6. Tạo Transaction record (type=REFUND, số dương vì hoàn tiền)
        transactionService.createTransaction(
            walletService.getOrCreateWallet(order.getBuyer()),
            order.getBuyer(),
            order,
            order.getDepositAmount(),
            TransactionType.REFUND,
            "Refund deposit for cancelled order #" + orderId
        );
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
        BigDecimal postFee = order.getPost().getPostFee();
        
        // 4. Buyer trả 80% còn lại
        if (!walletService.checkBalance(buyer.getUserId(), remainingAmount)) {
            throw new RuntimeException(
                String.format("Buyer insufficient balance for remaining payment. Required: %s VND", remainingAmount)
            );
        }
        
        walletService.chargeFee(buyer.getUserId(), remainingAmount, 
            "Final payment (80%) for order #" + orderId);
        
        // 5. Unlock và trừ deposit của buyer (chuyển từ lockedBalance → 0, không hoàn lại)
        walletService.unlockAndDeduct(buyer.getUserId(), depositPaid);
        
        // 6. Seller nhận = totalPrice - postFee
        BigDecimal sellerReceives = totalPrice.subtract(postFee);
        walletService.deposit(seller.getUserId(), sellerReceives);
        
        // 7. Tạo Transaction cho seller (nhận tiền)
        transactionService.createTransaction(
            walletService.getOrCreateWallet(seller),
            seller,
            order,
            sellerReceives,
            TransactionType.PAYMENT,
            String.format("Received payment for order #%d (Total: %s - Fee: %s = %s)", 
                orderId, totalPrice, postFee, sellerReceives)
        );
        
        // 8. Update order status
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        
        // 9. Update post status
        Post post = order.getPost();
        post.setStatus(PostStatus.SOLD);
        postRepository.save(post);
        
        // 10. Update delivery status nếu có
        deliverySessionRepository.findByOrder_OrderId(orderId).ifPresent(delivery -> {
            delivery.setStatus(DeliveryStatus.COMPLETED);
            deliverySessionRepository.save(delivery);
        });
        
        return mapToResponse(order);
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
    private OrderResponse mapToResponse(Order order) {
        Post post = order.getPost();
        
        OrderResponse.BuyerInfo buyerInfo = OrderResponse.BuyerInfo.builder()
                .userId(order.getBuyer().getUserId())
                .email(order.getBuyer().getEmail())
                .fullName(order.getBuyer().getFullName())
                .build();
        
        OrderResponse.SellerInfo sellerInfo = OrderResponse.SellerInfo.builder()
                .userId(post.getSeller().getUserId())
                .email(post.getSeller().getEmail())
                .fullName(post.getSeller().getFullName())
                .build();
        
        OrderResponse.PostInfo postInfo = OrderResponse.PostInfo.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .price(post.getPrice())
                .postFee(post.getPostFee())
                .build();
        
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
                .buyer(buyerInfo)
                .seller(sellerInfo)
                .post(postInfo)
                .build();
    }
}
