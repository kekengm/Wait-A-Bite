class Vendor extends User {
    private final int stall;
    Vendor(String id,String name,String password,int stall) {
        super(id,name,password);
        this.stall=stall;
    }
    public int getStall() {
        return stall;
    }
    public String role() {
        return "Vendor";
    }
}
