package controllers;

import dto.auth.AuthRequestDto;
import dto.auth.AuthResponseDto;
import dto.auth.LoginRequestDto;
import dto.user.UserRequestDto;
import entities.User;
import exceptions.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import security.JwtTokenProvider;
import services.AuthService;
import services.UserService;

import javax.print.attribute.standard.RequestingUserName;
import javax.security.auth.login.AccountLockedException;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    @EventListener(ApplicationReadyEvent.class)
    public void loadAdminsOnStartup() {
        authService.registerAdminsFromJson();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        try {
            validateLoginRequest(loginRequestDto);
            User user = userService.findUserByEmail(loginRequestDto.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponseDto());
            }
            if (loginRequestDto.getToken() != null && !loginRequestDto.getToken().isEmpty()) {
                Authentication auth = jwtTokenProvider.getAuthentication(loginRequestDto.getToken());
                if (auth.isAuthenticated() && auth.getName().equals(user.getEmail())) {
                    String jwtToken = jwtTokenProvider.generateToken(auth);
                    jwtTokenProvider.validateToken(jwtToken);
                    return handleTokenLogin(loginRequestDto, user);
                }
            }
            if (loginRequestDto.getPassword() != null && !loginRequestDto.getPassword().isEmpty()) {
                if (passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null);
                    String newJwtToken = jwtTokenProvider.generateToken(auth);
                    return ResponseEntity.ok(new AuthResponseDto(
                            user.getEmail(),
                            user.getUsername(),
                            user.getRole(),
                            newJwtToken
                    ));
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new AuthResponseDto());
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponseDto());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponseDto());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> authenticateAfterRegistration(@RequestBody AuthRequestDto authRequestDto) {
        try {authService.authenticateUser(authRequestDto);
             return ResponseEntity.ok().body("Authentication completed");
        } catch (IllegalArgumentException Iae) {
            return ResponseEntity.ok("Wrong password or email");
        }
    }
    @PostMapping("/register-admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerAdmins() {
        try {
            authService.registerAdminsFromJson();
            return ResponseEntity.ok("Admins registered successfully from JSON.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("JSON file not found: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Failed to register admins: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    private void validateLoginRequest(LoginRequestDto loginRequestDto) {
        if (loginRequestDto.getEmail() == null || loginRequestDto.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if ((loginRequestDto.getToken() == null || loginRequestDto.getToken().isEmpty()) &&
                (loginRequestDto.getPassword() == null || loginRequestDto.getPassword().isEmpty())) {
            throw new IllegalArgumentException("Either a token or a password must be provided.");
        }
    }
    private ResponseEntity<AuthResponseDto> handleTokenLogin(LoginRequestDto loginRequestDto, User user) {
        Authentication auth = jwtTokenProvider.getAuthentication(loginRequestDto.getToken());
        if (auth.isAuthenticated() && auth.getName().equals(user.getEmail())) {
            String newJwtToken = jwtTokenProvider.generateToken(auth);
            return ResponseEntity.ok(new AuthResponseDto(
                    user.getEmail(),
                    user.getUsername(),
                    user.getRole(),
                    newJwtToken
            ));
        } else {
            return unauthorizedResponse("Invalid or expired token.");
        }
    }
    private ResponseEntity<AuthResponseDto> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponseDto());
    }
}

