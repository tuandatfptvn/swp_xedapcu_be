package swp.be.vn.bs.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import swp.be.vn.bs.entity.Role;
import swp.be.vn.bs.entity.User;
import swp.be.vn.bs.repository.UserRepository;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Value("${oauth2.frontend.url}")
    private String frontendUrl;
    
    @Value("${oauth2.frontend.redirect-path}")
    private String redirectPath;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");
        
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .provider("google")
                    .providerId(googleId)
                    .role(Role.BUYER)
                    .build();
            userRepository.save(user);
        } else {
            user.setName(name);
            user.setPicture(picture);
            user.setProvider("google");
            user.setProviderId(googleId);
            userRepository.save(user);
        }
        
        String token = jwtTokenProvider.generateTokenFromEmailAndRole(
                user.getEmail(),
                "ROLE_" + user.getRole().name()
        );
        
        // Build redirect URL from configuration
        String redirectUrl = String.format("%s%s?token=%s&email=%s&role=%s",
                frontendUrl,           // From application.properties: http://localhost:5173
                redirectPath,          // From application.properties: /oauth2/redirect
                token, 
                user.getEmail(), 
                user.getRole().name());
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
