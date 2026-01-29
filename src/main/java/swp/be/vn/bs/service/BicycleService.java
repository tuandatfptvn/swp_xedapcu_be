package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.entity.Bicycle;
import swp.be.vn.bs.repository.BicycleRepository;

import java.util.List;

@Service
public class BicycleService {
    @Autowired
    private BicycleRepository bicycleRepository;

    public List<Bicycle> getAllBicycles() { return bicycleRepository.findAll(); }
    public Bicycle createBicycle(Bicycle bicycle) { return bicycleRepository.save(bicycle); }
    public void deleteBicycle(Integer id) { bicycleRepository.deleteById(id); }
}
