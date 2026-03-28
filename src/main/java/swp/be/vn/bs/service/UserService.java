package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.dto.request.UpdateProfileRequest;
import swp.be.vn.bs.dto.response.UserResponse;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.UserRepository;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .provider(user.getProvider())
                .picture(user.getPicture())
                .isActive(user.getIsActive())
                .build();
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .provider(user.getProvider())
                .picture(user.getPicture())
                .isActive(user.getIsActive())
                .build();
    }
    
    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhone(request.getPhoneNumber());
        
        User updatedUser = userRepository.save(user);
        
        return UserResponse.builder()
                .userId(updatedUser.getUserId())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .phone(updatedUser.getPhone())
                .role(updatedUser.getRole())
                .provider(updatedUser.getProvider())
                .picture(updatedUser.getPicture())
                .isActive(updatedUser.getIsActive())
                .build();
    }
}
