package swp.be.vn.bs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bicycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bicycle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bicycle_id")
    private Integer bicycleId;
    
    @Column(name = "brand", length = 255)
    private String brand;
    
    @Column(name = "frame_material", length = 255)
    private String frameMaterial;
    
    @Column(name = "frame_size", length = 50)
    private String frameSize;
    
    @Column(name = "groupset", length = 255)
    private String groupset;
    
    @Column(name = "wheel_size", length = 50)
    private String wheelSize;
    
    @Column(name = "condition_percent")
    private Integer conditionPercent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
