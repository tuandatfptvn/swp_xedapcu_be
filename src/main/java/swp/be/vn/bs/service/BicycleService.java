package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.dto.request.BicycleRequest;
import swp.be.vn.bs.dto.response.BicycleRespond;
import swp.be.vn.bs.entity.Bicycle;
import swp.be.vn.bs.entity.Category;
import swp.be.vn.bs.repository.BicycleRepository;
import swp.be.vn.bs.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BicycleService {
    @Autowired
    private BicycleRepository bicycleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<BicycleRespond> getAllBicycles() {
        return bicycleRepository.findAll().stream()
                .map(this::mapToRespond)
                .collect(Collectors.toList());
    }

    public BicycleRespond createBicycle(BicycleRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + request.getCategoryId()));

        Bicycle bicycle = new Bicycle();
        bicycle.setBrand(request.getBrand());
        bicycle.setFrameMaterial(request.getFrameMaterial());
        bicycle.setFrameSize(request.getFrameSize());
        bicycle.setGroupset(request.getGroupset());
        bicycle.setWheelSize(request.getWheelSize());
        bicycle.setConditionPercent(request.getConditionPercent());
        bicycle.setCategory(category); // Gán thực thể Category vào Bicycle

        Bicycle savedBicycle = bicycleRepository.save(bicycle);
        return mapToRespond(savedBicycle);
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