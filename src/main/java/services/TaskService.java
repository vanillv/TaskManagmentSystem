package services;


import dto.task.TaskRequestDto;
import dto.task.TaskResponseDto;
import entities.Task;
import entities.User;
import entities.enums.TaskPriority;
import entities.enums.TaskStatus;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repositories.CommentRepository;
import repositories.TaskRepository;
import repositories.UserRepository;
import exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    public TaskResponseDto createTask(TaskRequestDto taskRequestDto, Long authorId) {
        try {
            log.info("task request: " + taskRequestDto.toString());
            User author = userRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + authorId));
            Task task = new Task();
            task.setTitle(taskRequestDto.getTitle());
            task.setDescription(taskRequestDto.getDescription());
            task.setPriority(taskRequestDto.getPriority());
            task.setStatus(TaskStatus.WAITING);
            task.setAuthor(author);
            log.info("task: " + task.toString());
            taskRepository.save(task);
            return new TaskResponseDto(task.getTitle(), task.getDescription(), task.getPriority(),
                    task.getStatus(),task.getAssignee().getUsername(), task.getAuthor().getUsername());
        } catch (Exception e) {
            throw e;
        }

    }
    public TaskResponseDto updateTask(Long taskId, TaskRequestDto taskRequestDto) {
        try {
            Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
            if (taskRequestDto.getTitle() != null) task.setTitle(taskRequestDto.getTitle());
            if (taskRequestDto.getDescription() != null) task.setDescription(taskRequestDto.getDescription());
            User assignee = userRepository.findById(taskRequestDto.getAssigneeId())
                    .orElseThrow(() -> new EntityNotFoundException("Assignee not found with ID: " + taskRequestDto.getAssigneeId()));
            if (taskRequestDto.getPriority() != null) task.setPriority(taskRequestDto.getPriority());
            if (taskRequestDto.getStatus() != null) task.setStatus(taskRequestDto.getStatus());
            taskRepository.save(task);
            return new TaskResponseDto(task.getTitle(), task.getDescription(), task.getPriority(),
                    task.getStatus(), assignee.getUsername(), task.getAuthor().getUsername());
        } catch (Exception e) {
            throw e;
        }
    }
    public void deleteTask(Long taskId) {
        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
            taskRepository.delete(task);
        } catch (Exception e) {
            throw e;
        }

    }
    public List<TaskResponseDto> findTasksByUser(Long userId, boolean asAuthor, TaskStatus status, TaskPriority priority) {
        try {
            List<Task> tasks;
            if (asAuthor) {
                tasks = taskRepository.findByAuthorId(userId);
            } else {
                tasks = taskRepository.findByAssigneeId(userId);
            }
            if (status != null) {
                tasks = tasks.stream()
                        .filter(task -> task.getStatus().equals(status))
                        .collect(Collectors.toList());
            }
            if (priority != null) {
                tasks = tasks.stream()
                        .filter(task -> task.getPriority().equals(priority))
                        .collect(Collectors.toList());
            }
            return tasks.stream()
                    .map(task -> new TaskResponseDto(
                            task.getTitle(),
                            task.getDescription(),
                            task.getPriority(),
                            task.getStatus(),
                            task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                            task.getAuthor().getUsername()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw e;
        }

    }
    public TaskResponseDto updateTaskStatus(Long taskId, TaskStatus status, TaskPriority priority) {
        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new EntityNotFoundException("Task not found."));
            validateStatusTransition(task.getStatus(), status);
            if (status != null) {
                task.setStatus(status);
            }
            if (priority != null) {
                task.setPriority(priority);
            }
            taskRepository.save(task);
            return new TaskResponseDto(task.getTitle(), task.getDescription(), task.getPriority(),
                    task.getStatus(), task.getAssignee().getUsername(), task.getAuthor().getUsername());
        } catch (Exception e) {
            throw e;
        }
    }
    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot update a completed task.");
        }
        if (currentStatus == TaskStatus.IN_PROCESS && newStatus == TaskStatus.WAITING) {
            throw new IllegalArgumentException("Cannot move a task from 'IN_PROGRESS' to 'PENDING'.");
        }
    }
    public boolean existsById(Long id) {
        return taskRepository.existsById(id);
    }
    public boolean hasAccessToTask(Long taskId, Long userId) {
        return taskRepository.hasAccessToTask(taskId,userId);
    }
}




