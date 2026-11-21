class Buyer extends User {
    Buyer(String id,String name,String password) {
        super(id,name,password);
    }
    public String role() {
        return "Buyer";
    }
}
