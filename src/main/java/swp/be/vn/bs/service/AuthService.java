package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp.be.vn.bs.config.JwtTokenProvider;
import swp.be.vn.bs.dto.response.AuthResponse;
import swp.be.vn.bs.dto.request.LoginRequest;
import swp.be.vn.bs.dto.request.RegisterRequest;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.UserRepository;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private WalletService walletService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .provider("local")
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Auto tạo wallet cho user mới
        walletService.getOrCreateWallet(savedUser);
        
        String token = jwtTokenProvider.generateTokenFromEmailRoleAndId(
                user.getEmail(), 
                "ROLE_" + user.getRole().name(),
                savedUser.getUserId()
        );
        
        return AuthResponse.builder()
                .userId(savedUser.getUserId())
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        // Step 1: Check if user exists
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        Boolean isActive = user.getIsActive();
        if (isActive == null || !isActive) {
            throw new RuntimeException("Account has been deactivated. Please contact support.");
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Step 4: Generate token
            String token = jwtTokenProvider.generateTokenFromEmailRoleAndId(
                    user.getEmail(),
                    "ROLE_" + user.getRole().name(),
                    user.getUserId()
            );
            
            return AuthResponse.builder()
                    .userId(user.getUserId())
                    .token(token)
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
                    
        } catch (Exception e) {
            // Only catch authentication errors (wrong password)
            throw new RuntimeException("Invalid email or password");
        }
    }
}
