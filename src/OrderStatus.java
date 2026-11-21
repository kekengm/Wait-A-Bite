enum OrderStatus {
    PENDING, COMPLETED, CANCELLED;

    public static OrderStatus fromCsv(String value) {
        if ("READY".equalsIgnoreCase(value.trim())) return PENDING;
        return OrderStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
