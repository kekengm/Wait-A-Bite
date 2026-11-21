import java.time.LocalDateTime;
class PurchaseRecord {
    private final int id,item,stall,quantity,price;
    private final String buyer,name,type;
    private final LocalDateTime time;
    private OrderStatus status=OrderStatus.PENDING;
    private String receiptId = "";
    private String instructions = "";
    private String cancellationReason = "";

    PurchaseRecord(int id,String buyer,MenuItem m,int quantity) {
        this.id=id;
        this.buyer=buyer;
        item=m.getId();
        stall=m.getStall();
        name=m.getName();
        type=m.type();
        price=m.getPrice();
        this.quantity=quantity;
        this.time=LocalDateTime.now();
    }
    PurchaseRecord(int id,String buyer,int item,int stall,String name,String type,int price,int quantity,LocalDateTime time,OrderStatus status) {
        this.id=id;
        this.buyer=buyer;
        this.item=item;
        this.stall=stall;
        this.name=name;
        this.type=type;
        this.price=price;
        this.quantity=quantity;
        this.time=time;
        this.status=status;
    }
    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String text) {
      
        instructions = cleanText(text);
    }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String text) { cancellationReason = cleanText(text); }
    private static String cleanText(String text) {
        return text == null ? "" : text.replaceAll("[\r\n\t]+", " ").trim();
    }
    public boolean cancel(String reason) {
        if (status != OrderStatus.PENDING) return false;
        cancellationReason = cleanText(reason);
        status = OrderStatus.CANCELLED;
        return true;
    }

    public int getPrice() {
        return price;
    }
    public LocalDateTime getTime() {
        return time;
    }
    public int getId() {
        return id;
    }
    public int getItem() {
        return item;
    }
    public int getStall() {
        return stall;
    }
    public int getQuantity() {
        return quantity;
    }
    public String getBuyer() {
        return buyer;
    }
    public String getName() {
        return name;
    }
    public String getType() {
        return type;
    }
    public long total() {
        return (long)price*quantity;
    }
    public OrderStatus getStatus() {
        return status;
    }
    public void advance() {
        if (status == OrderStatus.PENDING) {
            status = OrderStatus.COMPLETED;
        }
    }
    public String toString() {
        return "#"+id+" | "+buyer+" | "+name+" x"+quantity+" | PHP "+total()+" | "+status+" | "+time;
    }
}
