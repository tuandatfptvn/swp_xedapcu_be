package swp.be.vn.bs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@CrossOrigin(origins = "*")
public class OAuth2Controller {
    
    @GetMapping("/redirect")
    public ResponseEntity<?> oauth2Redirect(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam String role) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Google login successful!");
        response.put("token", token);
        response.put("email", email);
        response.put("role", role);
        response.put("loginType", "GOOGLE_OAUTH2");
        
        return ResponseEntity.ok(response);
    }
}
