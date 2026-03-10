package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.dto.request.BicycleRequest;
import swp.be.vn.bs.dto.response.BicycleRespond;
import swp.be.vn.bs.entity.Bicycle;
import swp.be.vn.bs.entity.Category;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.BicycleRepository;
import swp.be.vn.bs.repository.CategoryRepository;
import swp.be.vn.bs.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BicycleService {
    @Autowired
    private BicycleRepository bicycleRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;

    public List<BicycleRespond> getAllBicycles() {
        return bicycleRepository.findAll().stream()
                .map(this::mapToRespond)
                .collect(Collectors.toList());
    }
    
    public List<BicycleRespond> getBicyclesByOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + ownerEmail));
        
        return bicycleRepository.findByOwner(owner).stream()
                .map(this::mapToRespond)
                .collect(Collectors.toList());
    }

    public BicycleRespond createBicycle(BicycleRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + ownerEmail));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + request.getCategoryId()));

        Bicycle bicycle = new Bicycle();
        bicycle.setBrand(request.getBrand());
        bicycle.setFrameMaterial(request.getFrameMaterial());
        bicycle.setFrameSize(request.getFrameSize());
        bicycle.setGroupset(request.getGroupset());
        bicycle.setWheelSize(request.getWheelSize());
        bicycle.setConditionPercent(request.getConditionPercent());
        bicycle.setCategory(category);
        bicycle.setOwner(owner);

        Bicycle savedBicycle = bicycleRepository.save(bicycle);
        return mapToRespond(savedBicycle);
    }

    public BicycleRespond getBicycleById(Integer id) {
        Bicycle bicycle = bicycleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bicycle not found with ID: " + id));
        return mapToRespond(bicycle);
    }
    
    public BicycleRespond updateBicycle(Integer id, BicycleRequest request, String ownerEmail) {
        Bicycle bicycle = bicycleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bicycle not found with ID: " + id));
        
        if (!bicycle.getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("You can only update your own bicycles");
        }
        
        if (request.getBrand() != null) bicycle.setBrand(request.getBrand());
        if (request.getFrameMaterial() != null) bicycle.setFrameMaterial(request.getFrameMaterial());
        if (request.getFrameSize() != null) bicycle.setFrameSize(request.getFrameSize());
        if (request.getGroupset() != null) bicycle.setGroupset(request.getGroupset());
        if (request.getWheelSize() != null) bicycle.setWheelSize(request.getWheelSize());
        if (request.getConditionPercent() != null) bicycle.setConditionPercent(request.getConditionPercent());
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with ID: " + request.getCategoryId()));
            bicycle.setCategory(category);
        }
        
        Bicycle updatedBicycle = bicycleRepository.save(bicycle);
        return mapToRespond(updatedBicycle);
    }

    public void deleteBicycle(Integer id) {
        bicycleRepository.deleteById(id);
    }

    private BicycleRespond mapToRespond(Bicycle bicycle) {
        return BicycleRespond.builder()
                .bicycleId(bicycle.getBicycleId())
                .brand(bicycle.getBrand())
                .frameMaterial(bicycle.getFrameMaterial())
                .frameSize(bicycle.getFrameSize())
                .groupset(bicycle.getGroupset())
                .wheelSize(bicycle.getWheelSize())
                .conditionPercent(bicycle.getConditionPercent())
                .categoryName(bicycle.getCategory() != null ? bicycle.getCategory().getName() : null)
                .build();
    }
}