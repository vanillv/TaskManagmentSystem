package TaskManagementSystem;
import controllers.UserController;
import dto.comment.CommentResponseDto;
import dto.task.TaskResponseDto;
import dto.user.UserRequestDto;
import dto.user.UserResponseDto;
import exceptions.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import security.UserDetailsServiceImpl;
import services.*;
import entities.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@WebMvcTest(UserController.class)
public class UserControllerTest {
    @InjectMocks
    private UserController userController;
    @Mock
    private UserService userService;
    @Mock
    private TaskService taskService;
    @Mock
    private CommentService commentService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void getUserAsAdminAndReturnUserResponse() {
        String username = "testuser";
        UserDetails mockUserDetails = User.builder()
                .username(username)
                .password("password")
                .roles("ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        ResponseEntity<UserResponseDto> response = userController.getUser(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(username, response.getBody().getUsername());
        verify(userDetailsService, times(1)).loadUserByUsername(username);
    }

    @Test
    void registerUser_ValidInput_ReturnsCreatedUser() {
        UserRequestDto requestDto = new UserRequestDto("newuser", "newuser@example.com", "password123");
        UserResponseDto responseDto = new UserResponseDto("newuser", "newuser@example.com", Role.USER);
        when(userService.registerUser(requestDto)).thenReturn(responseDto);
        ResponseEntity<UserResponseDto> response = userController.registerUser(requestDto);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(requestDto.getUsername(), response.getBody().getUsername());
        verify(userService, times(1)).registerUser(requestDto);
    }
    @Test
    @WithMockUser(roles = {"ADMIN"})
    void assignRole_AsAdmin_ReturnsUpdatedUser() {
        Long userId = 1L;
        Role newRole = Role.ADMIN;
        UserResponseDto responseDto = new UserResponseDto("user1", "user1@example.com", newRole);
        when(userService.assignRole(userId, newRole)).thenReturn(responseDto);
        ResponseEntity<?> response = userController.assignRole(userId, newRole);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof UserResponseDto);
        assertEquals(newRole, ((UserResponseDto) response.getBody()).getRole());
        verify(userService, times(1)).assignRole(userId, newRole);
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void getCommentsByUser_ValidUserId_ReturnsComments() {
        Long userId = 1L;
        List<CommentResponseDto> comments = List.of(
                new CommentResponseDto("Great task!", "user1", "Task1"),
                new CommentResponseDto("Needs revision.", "user1", "Task2")
        );
        when(commentService.getCommentsByUser(userId)).thenReturn(comments);
        ResponseEntity<?> response = userController.getCommentsByUser(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        assertEquals(2, ((List<?>) response.getBody()).size());
        verify(commentService, times(1)).getCommentsByUser(userId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void getUserTasksAsAdminAndReturnTasks() {
        Long userId = 1L;
        boolean asAuthor = true;
        List<TaskResponseDto> tasks = List.of(
                new TaskResponseDto("Task1", "Description1", TaskPriority.HIGH, TaskStatus.IN_PROCESS, "assignee1", "author1"),
                new TaskResponseDto("Task2", "Description2", TaskPriority.MEDIUM, TaskStatus.WAITING, "assignee2", "author1")
        );
        when(taskService.findTasksByUser(userId, asAuthor, null, null)).thenReturn(tasks);
        ResponseEntity<?> response = userController.getUserTasks(userId, asAuthor, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        assertEquals(2, ((List<?>) response.getBody()).size());
        verify(taskService, times(1)).findTasksByUser(eq(userId), eq(asAuthor), eq(null), eq(null));
    }
    @Test
    void registerUser_InvalidEmail_ShouldThrowException() {
        UserRequestDto invalidRequest = new UserRequestDto("username", "invalid-email", "password123");
        when(userService.registerUser(invalidRequest)).thenThrow(new ConstraintViolationException("Invalid email format", Collections.emptySet()));
        Exception exception = assertThrows(ConstraintViolationException.class, () -> {
            userController.registerUser(invalidRequest);
        });
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void registerUser_EmptyUsername_ShouldThrowException() {
        UserRequestDto invalidRequest = new UserRequestDto("", "email@example.com", "password123");
        when(userService.registerUser(invalidRequest)).thenThrow(new ConstraintViolationException("Username is required", Collections.emptySet()));
        Exception exception = assertThrows(ConstraintViolationException.class, () -> {
            userController.registerUser(invalidRequest);
        });
        assertEquals("Username is required", exception.getMessage());
    }

    @Test
    void assignRole_InvalidRole_ShouldReturnBadRequest() {
        Long userId = 1L;
        when(userService.assignRole(userId, null)).thenThrow(new IllegalArgumentException("Invalid role"));
        ResponseEntity<?> response = userController.assignRole(userId, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid role", response.getBody());
    }

    @Test
    void getUserTasks_InvalidUserId_ShouldReturnBadRequest() {
        Long invalidUserId = -1L;
        when(taskService.findTasksByUser(invalidUserId, true, null, null)).thenThrow(new IllegalArgumentException("Invalid user ID"));
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userController.getUserTasks(invalidUserId, true, null, null);
        });
        assertEquals("Invalid user ID", exception.getMessage());
    }

    @Test
    void getCommentsByUser_NonExistentUser_ShouldReturnNotFound() {
        Long userId = 999L;
        when(commentService.getCommentsByUser(userId)).thenThrow(new ResourceNotFoundException("User not found"));
        ResponseEntity<?> response = userController.getCommentsByUser(userId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }
}