package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.dto.request.UpdateRoleRequest;
import swp.be.vn.bs.dto.response.UserResponse;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .email(user.getEmail())
                        .role(user.getRole())
                        .provider(user.getProvider())
                        .name(user.getName())
                        .picture(user.getPicture())
                        .build())
                .collect(Collectors.toList());
    }
    
    public UserResponse updateUserRole(String email, UpdateRoleRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        user.setRole(request.getRole());
        userRepository.save(user);
        
        return UserResponse.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .name(user.getName())
                .picture(user.getPicture())
                .build();
    }
    
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        userRepository.delete(user);
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        return UserResponse.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .name(user.getName())
                .picture(user.getPicture())
                .build();
    }
}
