package dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import entities.enums.TaskPriority;
import entities.enums.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRequestDto {

    @NotBlank(message = "Title is mandatory")
    private String title;

    @NotBlank(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Priority is mandatory")
    private TaskPriority priority;

    @NotNull(message = "Status is mandatory")
    private TaskStatus status;

    @NotNull(message = "Assignee ID is mandatory")
    private Long assigneeId;
}

