package com.example.collaboration.auth;

import com.example.collaboration.user.UserSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final CurrentUserService currentUser;

    public AuthController(AuthService auth, CurrentUserService currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(auth.login(request));
    }

    @GetMapping("/me")
    UserSummaryResponse me() {
        return UserSummaryResponse.from(currentUser.requireCurrentUser());
    }
}

