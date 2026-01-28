package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"") // Thêm dấu ngoặc kép để escape reserved keyword
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // Đổi từ "user_id" thành "id"
    private Integer userId; // Giữ tên field là userId cho code consistency
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password", length = 255)
    private String password;
    
    @Column(name = "full_name", length = 200)
    private String fullName;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;
    
    @Column(name = "provider", length = 50)
    private String provider;
    
    @Column(name = "provider_id", length = 255)
    private String providerId;
    
    @Column(name = "name", length = 255)
    private String name;
    
    @Column(name = "picture", length = 500)
    private String picture;
    
    @Column(name = "rating_score")
    private Float ratingScore;
    
    @Column(name = "violation_count")
    private Integer violationCount;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (violationCount == null) {
            violationCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
