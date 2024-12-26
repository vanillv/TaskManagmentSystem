package services;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.auth.AdminRegistrationDto;
import entities.User;
import entities.enums.Role;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import repositories.UserRepository;
import security.JwtTokenProvider;
import dto.auth.AuthRequestDto;
import dto.auth.AuthResponseDto;
import exceptions.UnauthorizedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private static final String ADMIN_JSON_PATH = "admins.json";



    @Transactional
    public AuthResponseDto authenticateUser(AuthRequestDto authRequestDto) {
        try {
            Authentication authentication = authenticate(authRequestDto.getEmail(), authRequestDto.getPassword());
            String token = jwtTokenProvider.generateToken(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return new AuthResponseDto(
                    authRequestDto.getEmail(),
                    userDetails.getUsername(),
                    ((User) userDetails).getRole(),
                    token
            );
        } catch (IllegalArgumentException argumentException) {
            throw argumentException;
        } catch (Exception e) {
            throw e;
        }

    }
    private Authentication authenticate(String email, String password) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(email, password);
            return authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid email or password.");
        }
    }
    public void registerAdminsFromJson() {
        try {
            InputStream inputStream = new ClassPathResource(ADMIN_JSON_PATH).getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            List<AdminRegistrationDto> admins = objectMapper.readValue(inputStream,
                    new TypeReference<List<AdminRegistrationDto>>() {});
            for (AdminRegistrationDto admin : admins) {
                if (userRepository.existsByEmail(admin.getEmail())) {
                    Optional<User> user = userRepository.findByUsername(admin.getUsername());
                    if (user.isPresent()) {
                        user.get().setRole(Role.ADMIN);
                        userRepository.save(user.get());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load admins from JSON file", e);
        }
    }
}


