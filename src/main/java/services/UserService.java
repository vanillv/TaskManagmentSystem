package services;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dto.user.UserRequestDto;
import dto.user.UserResponseDto;
import entities.User;
import entities.enums.Role;
import exceptions.ResourceNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserResponseDto registerUser(UserRequestDto userRequestDto) {
        log.info("user request:" + userRequestDto.toString());
        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (userRepository.existsByUsername(userRequestDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists.");
        }
        User newUser = new User();
        newUser.setUsername(userRequestDto.getUsername());
        newUser.setEmail(userRequestDto.getEmail());
        if (validatePassword(userRequestDto.getPassword())) {
        newUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));}
        newUser.setRole(Role.USER);
        User savedUser = userRepository.save(newUser);
        log.info("saved user: " + savedUser.toString());
        return new UserResponseDto(
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
    @Transactional
    public UserResponseDto assignRole(Long userId, Role role) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            if (role == Role.ADMIN && userId == 0) {
                throw new IllegalArgumentException("Role change is not allowed for the root admin (ID = 0).");
            }
            user.setRole(role);
            User updatedUser = userRepository.save(user);
            if (role == Role.ADMIN) {
                appendAdminToYaml(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getEmail());
            }
            return new UserResponseDto(
                    updatedUser.getUsername(),
                    updatedUser.getEmail(),
                    updatedUser.getRole()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException("Failed to assign role due to an unexpected error", e);
        }
    }

    public Long findUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user.getId();
    }
    public User findUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user;
    }
    private void appendAdminToYaml(Long id, String username, String email) {
        Path filePath = Paths.get("src/main/resources/admins.yaml");
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yamlData = mapper.readValue(Files.newInputStream(filePath), Map.class);
            Map<String, Map<String, Object>> admins = (Map<String, Map<String, Object>>) yamlData.getOrDefault("admins", new LinkedHashMap<>());
            String newAdminKey = "admin-" + id;
            Map<String, Object> newAdminData = new LinkedHashMap<>();
            newAdminData.put("id", id);
            newAdminData.put("username", username);
            newAdminData.put("email", email);
            admins.put(newAdminKey, newAdminData);
            yamlData.put("admins", admins);
            mapper.writeValue(Files.newOutputStream(filePath), yamlData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update admins.yaml", e);
        }
    }

    private boolean validatePassword(String password) {
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException("Password must contain at least one digit.");
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new ValidationException("Password must contain at least one letter.");
        }
        return true;
    }
}
