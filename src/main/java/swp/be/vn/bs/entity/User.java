package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"user\"")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "password")
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;
    
    @Column(name = "provider")
    private String provider;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "picture")
    private String picture;
}
