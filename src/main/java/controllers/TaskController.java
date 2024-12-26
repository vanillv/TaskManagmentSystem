package controllers;

import dto.comment.CommentRequestDto;
import dto.comment.CommentResponseDto;
import dto.task.TaskRequestDto;
import dto.task.TaskResponseDto;
import entities.enums.TaskPriority;
import entities.enums.TaskStatus;
import exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import services.CommentService;
import services.TaskService;
import services.UserService;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CommentService commentService;
    private final UserService userService;

    @PostMapping("/createTask")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTask(@RequestBody @Valid TaskRequestDto taskRequestDto, Principal principal) {
        try {
            String email = principal.getName();
            Long authorId = userService.findUserIdByEmail(email);
            TaskResponseDto taskResponseDto = taskService.createTask(taskRequestDto, authorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(taskResponseDto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTask(
            @PathVariable Long taskId,
            @RequestBody @Valid TaskRequestDto taskRequestDto) {
        try {
             return ResponseEntity.ok().body(taskService.updateTask(taskId, taskRequestDto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTask(@PathVariable @Positive Long taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/{userId}/tasks/{taskId}/status")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long taskId,
            @RequestParam @NotNull TaskStatus status, @RequestParam @NotNull TaskPriority priority) {
        try {
            return ResponseEntity.ok(taskService.updateTaskStatus(taskId, status, priority));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PostMapping("/{userId}/tasks/{taskId}/comments")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public ResponseEntity<CommentResponseDto> addCommentToTask(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody @Valid CommentRequestDto commentRequestDto) {
        CommentResponseDto addedComment = commentService.addComment(commentRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(addedComment);
    }
    @GetMapping("/{taskId}/comments")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable @Positive Long taskId) {
        if (!taskService.existsById(taskId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        List<CommentResponseDto> comments = commentService.getCommentsByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }
}

