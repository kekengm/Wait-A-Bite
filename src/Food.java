class Food extends MenuItem {
    Food(int id,int stall,String name,String category,int price,int stock) {
        super(id,stall,name,category,price,stock);
    }
    @Override
    public String type() {
        return "Food";
    }
    @Override
    public String displayDetails() {
        return super.displayDetails();
    }
}
