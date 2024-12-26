package entities.enums;

public enum TaskStatus {
    WAITING("В ожидании"),
    IN_PROCESS("В процессе"),
    COMPLETED("Завершено");
    private final String status;

    TaskStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
