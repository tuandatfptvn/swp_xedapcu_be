package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private InspectionRequest inspection;
    
    @Column(name = "deposit_amount", precision = 18, scale = 2)
    private BigDecimal depositAmount;
    
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "remaining_amount", precision = 18, scale = 2)
    private BigDecimal remainingAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status;
    
    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_inspector_id")
    private User assignedInspector;
    
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "order")
    private DeliverySession deliverySession;
    
    @Column(name = "seller_confirmed_at")
    private LocalDateTime sellerConfirmedAt;
    
    @Column(name = "admin_reviewed_at")
    private LocalDateTime adminReviewedAt;
    
    @Column(name = "is_active", nullable = true, columnDefinition = "boolean default true")
    private Boolean isActive = true;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
}
