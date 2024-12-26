package repositories;

import entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.task.id = :taskId")
    List<Comment> findByTaskId(Long taskId);

    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId")
    List<Comment> findByAuthorId(Long authorId);
    boolean existsByIdAndAuthorId(Long commentId, Long authorId);

}
