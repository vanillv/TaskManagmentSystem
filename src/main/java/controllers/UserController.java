package controllers;

import dto.comment.CommentResponseDto;
import dto.task.TaskResponseDto;
import dto.user.UserRequestDto;
import dto.user.UserResponseDto;
import entities.enums.Role;
import entities.enums.TaskPriority;
import entities.enums.TaskStatus;
import exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.spi.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import security.UserDetailsServiceImpl;
import services.CommentService;
import services.TaskService;
import services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final TaskService taskService;
    private final CommentService commentService;
    private final UserDetailsServiceImpl userDetailsService;

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN') or #username == principal.username")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UserResponseDto userResponseDto = new UserResponseDto(
                userDetails.getUsername(),
                userDetails.getUsername(),
                Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
        );
        return ResponseEntity.ok(userResponseDto);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@RequestBody @Valid UserRequestDto userRequestDto) {
        UserResponseDto registeredUser = userService.registerUser(userRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @RequestParam Role role) {
        try {
            UserResponseDto updatedUser = userService.assignRole(userId, role);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getCommentsByUser(@PathVariable Long userId) {
        try {
            List<CommentResponseDto> comments = commentService.getCommentsByUser(userId);
            return ResponseEntity.ok(comments);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @GetMapping("/{userId}/tasks")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public ResponseEntity<?> getUserTasks(
        @PathVariable Long userId,
        @RequestParam boolean asAuthor,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority) {
        List<TaskResponseDto> tasks = taskService.findTasksByUser(userId, asAuthor, status, priority);
        return ResponseEntity.ok(tasks);
    }
}


