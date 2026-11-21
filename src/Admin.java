class Admin extends User {
    Admin(String id,String name,String password) {
        super(id,name,password);
    }
    public String role() {
        return "Admin";
    }
}
