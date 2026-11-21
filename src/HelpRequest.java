import java.time.LocalDateTime;

class HelpRequest {
    private final int id;
    private final String senderId, senderRole, message;
    private final LocalDateTime time;
    private boolean resolved;

    HelpRequest(int id, String senderId, String senderRole, String message,
                LocalDateTime time, boolean resolved) {
        this.id = id;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.message = message;
        this.time = time;
        this.resolved = resolved;
    }
    int getId() { return id; }
    String getSenderId() { return senderId; }
    String getSenderRole() { return senderRole; }
    String getMessage() { return message; }
    LocalDateTime getTime() { return time; }
    boolean isResolved() { return resolved; }
    void resolve() { resolved = true; }
}
