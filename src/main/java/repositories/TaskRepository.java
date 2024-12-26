package repositories;

import entities.Task;
import entities.User;
import entities.enums.TaskPriority;
import entities.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAuthorId(Long authorId);
    List<Task> findByAssigneeId(Long assigneeId);
    boolean existsById(Long id);
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM Task t " +
            "WHERE t.id = :taskId AND (t.author.id = :userId OR t.assignee.id = :userId)")
    boolean hasAccessToTask(@Param("taskId") Long taskId, @Param("userId") Long userId);

}

