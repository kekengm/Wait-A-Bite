abstract class MenuItem {
    private final int id,stall;
    private String name,category;
    private int price,stock;
    private boolean active=true;
    MenuItem(int id,int stall,String name,String category,int price,int stock) {
        this.id=id;
        this.stall=stall;
        this.name=name;
        this.category=category;
        updateInfo(price,stock);
    }
    public int getId() {
        return id;
    }
    public int getStall() {
        return stall;
    }
    public String getName() {
        return name;
    }
    public String getCategory() {
        return category;
    }
    public int getPrice() {
        return price;
    }
    public int getStock() {
        return stock;
    }
    public boolean isActive() {
        return active;
    }
    public void remove() {
        active=false;
    }
    public void updateInfo(int price,int stock) {
        if (price< -1||stock<0)throw new IllegalArgumentException("Invalid price or stock");
        this.price=price;
        this.stock=stock;
    }
    public void updateDescription(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public void deduct(int n) {
        stock-=n;
    }
    public abstract String type();
    public String displayDetails() {
        return id+". ["+type()+" / "+category+"] "+name+" | "+(price<0?"Price not provided":"PHP "+price)+" | stock: "+stock;
    }
}
