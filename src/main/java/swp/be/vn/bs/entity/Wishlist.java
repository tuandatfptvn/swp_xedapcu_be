package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "wishlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(Wishlist.WishlistId.class)
public class Wishlist {
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    // Composite Primary Key Class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistId implements Serializable {
        private Integer user;
        private Integer post;
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WishlistId that = (WishlistId) o;
            return Objects.equals(user, that.user) && Objects.equals(post, that.post);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(user, post);
        }
    }
}
