package swp.be.vn.bs.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    @Value("${oauth2.frontend.url}")
    private String frontendUrl;
    
    @Value("${oauth2.frontend.redirect-path}")
    private String redirectPath;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        // Get error message from exception
        String errorMessage = exception.getMessage();
        if (errorMessage == null) {
            errorMessage = "Authentication failed";
        }
        
        // Encode message để dùng trong URL parameter
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        
        // Redirect tới FE với error parameter
        String redirectUrl = String.format("%s%s?error=%s",
                frontendUrl,
                redirectPath,
                encodedError
        );
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
