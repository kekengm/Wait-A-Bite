import java.time.LocalDateTime;

class RegistrationRequest {
    enum Status { PENDING, APPROVED, REJECTED }

    private final int id;
    private final String userId, name, password, role;
    private final int stallId;
    private final LocalDateTime requestedAt;
    private Status status;

    RegistrationRequest(int id, String userId, String name, String password,
                        String role, int stallId, LocalDateTime requestedAt, Status status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.password = password;
        this.role = role;
        this.stallId = stallId;
        this.requestedAt = requestedAt;
        this.status = status;
    }
    int getId() { return id; }
    String getUserId() { return userId; }
    String getName() { return name; }
    String getPassword() { return password; }
    String getRole() { return role; }
    int getStallId() { return stallId; }
    LocalDateTime getRequestedAt() { return requestedAt; }
    Status getStatus() { return status; }
    void approve() { status = Status.APPROVED; }
    void reject() { status = Status.REJECTED; }
}
