package dto.task;

import entities.enums.TaskPriority;
import entities.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDto {
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private String assigneeUsername;
    private String authorUsername;
}
