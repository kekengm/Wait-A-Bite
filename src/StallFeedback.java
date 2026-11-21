import java.time.LocalDateTime;

class StallFeedback {
    private final int id;
    private final String buyerId;
    private final int stallId;
    private final String message;
    private final LocalDateTime time;

    StallFeedback(int id, String buyerId, int stallId, String message, LocalDateTime time) {
        this.id = id;
        this.buyerId = buyerId;
        this.stallId = stallId;
        this.message = message;
        this.time = time;
    }

    int getId() { return id; }
    String getBuyerId() { return buyerId; }
    int getStallId() { return stallId; }
    String getMessage() { return message; }
    LocalDateTime getTime() { return time; }
}

