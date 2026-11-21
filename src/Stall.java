class Stall {
    private final int id;
    private final String name;
    private boolean open=true;
    Stall(int id,String name) {
        this.id=id;
        this.name=name;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public boolean isOpen() {
        return open;
    }
    public void toggle() {
        open=!open;
    }
}
