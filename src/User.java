abstract class User implements Authenticatable {
    private final String id,name,password;
    private boolean active=true;
    User(String id,String name,String password) {
        this.id=id;
        this.name=name;
        this.password=password;
    }
    String getPassword() {
        return password;
    }
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public boolean login(String p) {
        return active && password.equals(p);
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean a) {
        active=a;
    }
    public abstract String role();
}
