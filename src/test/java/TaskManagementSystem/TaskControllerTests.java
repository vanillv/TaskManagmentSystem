package TaskManagementSystem;

import controllers.TaskController;
import dto.task.TaskRequestDto;
import dto.task.TaskResponseDto;
import entities.enums.TaskPriority;
import entities.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import security.JwtTokenProvider;
import services.TaskService;
import services.UserService;
import services.CommentService;
import exceptions.ResourceNotFoundException;
import dto.comment.CommentRequestDto;
import dto.comment.CommentResponseDto;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@WebMvcTest(TaskController.class)
@WithMockUser(username = "admin", roles = {"ADMIN"})
public class TaskControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @InjectMocks
    private TaskController taskController;
    @Mock
    private TaskService taskService;
    @Mock
    private CommentService commentService;
    @Mock
    private UserService userService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    private static final Logger log = LoggerFactory.getLogger(TaskControllerTests.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @BeforeEach
    public void setup() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(anyString())).thenReturn(new UsernamePasswordAuthenticationToken("adminUser", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void createTask_ShouldReturnCreatedTask() throws Exception {
        TaskRequestDto taskRequestDto = new TaskRequestDto("Test Task", "Test Description", TaskPriority.HIGH, TaskStatus.WAITING, 2L);
        TaskResponseDto taskResponseDto = new TaskResponseDto("Test Task", "Test Description", TaskPriority.HIGH, TaskStatus.WAITING, "assigneeUsername", "adminUser");
        log.info("Test data prepared. TaskRequestDto: {}", taskRequestDto);
        when(userService.findUserIdByEmail(anyString())).thenReturn(1L);
        when(taskService.createTask(any(), eq(1L))).thenReturn(taskResponseDto);
        log.info("Mocked userService.findUserIdByEmail() to return: 1L");
        log.info("Mocked taskService.createTask() to return: {}", taskResponseDto);
        mockMvc.perform(post("/api/tasks/createTask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(taskRequestDto)).with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("sub", "test@example.com")
                                        .claim("roles", Collections.singletonList("ROLE_ADMIN")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.assigneeUsername").value("assigneeUsername"))
                .andExpect(jsonPath("$.authorUsername").value("adminUser"));


        log.info("Test executed successfully. Task created with title: 'Test Task'");
    }

    @Test
    void createTask_UserNotFound_ShouldReturnNotFound() throws Exception {
        TaskRequestDto taskRequestDto = new TaskRequestDto("Test Task", "Test Description", TaskPriority.HIGH, TaskStatus.WAITING, 2L);

        when(userService.findUserIdByEmail(anyString())).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(taskRequestDto))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com")))  // Mock JWT with user info
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    void updateTask_ShouldReturnUpdatedTask() throws Exception {
        TaskRequestDto taskRequestDto = new TaskRequestDto("Updated Task", "Updated Description", TaskPriority.MEDIUM, TaskStatus.IN_PROCESS, 3L);
        TaskResponseDto taskResponseDto = new TaskResponseDto("Updated Task", "Updated Description", TaskPriority.MEDIUM, TaskStatus.IN_PROCESS, "assigneeUsername", "authorUsername");

        when(taskService.updateTask(eq(1L), any())).thenReturn(taskResponseDto);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(taskRequestDto))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com")))  // Mock JWT with user info
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("IN_PROCESS"))
                .andExpect(jsonPath("$.assigneeUsername").value("assigneeUsername"))
                .andExpect(jsonPath("$.authorUsername").value("authorUsername"));
    }

    @Test
    void updateTask_TaskNotFound_ShouldReturnNotFound() throws Exception {
        TaskRequestDto taskRequestDto = new TaskRequestDto("Updated Task", "Updated Description", TaskPriority.MEDIUM, TaskStatus.IN_PROCESS, 3L);

        when(taskService.updateTask(eq(1L), any())).thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(taskRequestDto))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com")))  // Mock JWT with user info
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("Task not found"));
    }

    @Test
    void deleteTask_TaskNotFound_ShouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Task not found")).when(taskService).deleteTask(eq(1L));

        mockMvc.perform(delete("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com")))  // Mock JWT with user info
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("Task not found"));
    }


    @Test
    void updateTaskStatus_ShouldReturnUpdatedTask() throws Exception {
        TaskResponseDto taskResponseDto = new TaskResponseDto("Test Task", "Test Description", TaskPriority.HIGH, TaskStatus.IN_PROCESS, "assigneeUsername", "authorUsername");

        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.IN_PROCESS), eq(TaskPriority.HIGH))).thenReturn(taskResponseDto);

        mockMvc.perform(patch("/api/tasks/1/status?status=IN_PROCESS&priority=HIGH")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com")))  // Mock JWT with user info
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROCESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void updateTaskStatus_InvalidStatus_ShouldReturnBadRequest() throws Exception {
        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.IN_PROCESS), eq(TaskPriority.HIGH)))
                .thenThrow(new IllegalArgumentException("Invalid status"));

        mockMvc.perform(patch("/api/tasks/1/status?status=IN_PROCESS&priority=HIGH")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com")))  // Mock JWT with user info
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid status"));
    }

    @Test
    void addCommentToTask_ShouldReturnCreatedComment() throws Exception {
        CommentRequestDto commentRequestDto = new CommentRequestDto("Test comment", 1L, 2L);
        CommentResponseDto commentResponseDto = new CommentResponseDto("Test comment", "testUser", "Test Task");

        when(commentService.addComment(any())).thenReturn(commentResponseDto);

        mockMvc.perform(post("/api/tasks/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(commentRequestDto))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com"))) // Mock JWT with user info
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Test comment"))
                .andExpect(jsonPath("$.authorName").value("testUser"))
                .andExpect(jsonPath("$.taskTitle").value("Test Task"));
    }

    @Test
    void getComments_ShouldReturnListOfComments() throws Exception {
        List<CommentResponseDto> comments = Arrays.asList(
                new CommentResponseDto("Test comment", "testUser", "Test Task"),
                new CommentResponseDto("Another comment", "testUser2", "Test Task")
        );

        when(commentService.getCommentsByTaskId(eq(1L))).thenReturn(comments);

        mockMvc.perform(get("/api/tasks/1/comments")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com"))) // Mock JWT with user info
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Test comment"))
                .andExpect(jsonPath("$[1].text").value("Another comment"));
    }

    @Test
    void getComments_TaskNotFound_ShouldReturnNotFound() throws Exception {
        when(commentService.getCommentsByTaskId(eq(1L))).thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(get("/api/tasks/1/comments")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "test@example.com"))) // Mock JWT with user info
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("Task not found"));
    }

}




