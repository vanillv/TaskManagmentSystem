package entities;
import entities.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @NotBlank(message = "Username is required")
   @Column(unique = true, nullable = false)
   private String username;

   @Email(message = "Invalid email format")
   @NotBlank(message = "Email is required")
   @Column(unique = true, nullable = false)
   private String email;

   @NotBlank(message = "Password is required")
   @Column(nullable = false)
   private String password;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private Role role;

   @CreationTimestamp
   @Column(nullable = false, updatable = false)
   private Instant createdAt;
}

