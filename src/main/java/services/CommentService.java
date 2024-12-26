package services;
import dto.comment.CommentRequestDto;
import dto.comment.CommentResponseDto;
import java.util.List;
import entities.Comment;
import entities.Task;
import entities.User;
import exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import repositories.CommentRepository;
import repositories.TaskRepository;
import repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    public CommentResponseDto addComment(CommentRequestDto commentRequestDto) {
        Task task = taskRepository.findById(commentRequestDto.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + commentRequestDto.getTaskId()));
        User author = userRepository.findById(commentRequestDto.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + commentRequestDto.getAuthorId()));
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setText(commentRequestDto.getText());
        Comment savedComment = commentRepository.save(comment);
        return convertToResponseDto(savedComment);
    }
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        commentRepository.delete(comment);
    }
    public List<CommentResponseDto> getCommentsByTaskId(Long taskId) {
       if (!taskRepository.existsById(taskId)) {
           throw new ResourceNotFoundException("Task with ID " + taskId + " does not exist.");
       }
        List<Comment> comments = commentRepository.findByTaskId(taskId);
        return comments.stream()
               .map(comment -> new CommentResponseDto(
                       comment.getText(),
                       comment.getAuthor().getUsername(),
                       comment.getTask().getTitle()
               ))
               .collect(Collectors.toList());
        }


    public List<CommentResponseDto> getCommentsByUser(Long userId) {
        try {
            // Fetch all comments authored by the specified user
            List<Comment> comments = commentRepository.findByAuthorId(userId);

            if (comments.isEmpty()) {
                throw new ResourceNotFoundException("No comments found for user with ID: " + userId);
            }

            // Convert each Comment entity to a CommentResponseDto
            return comments.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
            // Handle case where no comments are found
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to fetch comments for user with ID: " + userId, e);
        }
    }

    private CommentResponseDto convertToResponseDto(Comment comment) {
        CommentResponseDto dto = new CommentResponseDto();
        dto.setText(comment.getText());
        dto.setAuthorName(comment.getAuthor().getUsername());
        dto.setTaskTitle(comment.getTask().getTitle());
        return dto;
    }
}


