package dto.auth;

import lombok.Data;

@Data
public class AdminRegistrationDto {
    private Long id;
    private String email;
    private String username;
}
