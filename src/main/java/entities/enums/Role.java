package entities.enums;

public enum Role {
    ADMIN("Администратор"),
    USER("Пользователь");
    private final String role;
    Role(String role) {
        this.role = role;
    }
    public String getRole() {
        return role;
    }
}
