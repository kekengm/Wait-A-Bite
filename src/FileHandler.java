import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

class FileHandler {
    private static final Path DATA = Path.of("data");

    static WaitABiteSystem loadData() throws IOException {
        WaitABiteSystem system = new WaitABiteSystem();
        try {
            for (String[] r : read("stalls")) {
                Stall stall = new Stall(number(r, 0), r[1]);
                if (!Boolean.parseBoolean(r[2])) {
                    stall.toggle();
                }
                system.stalls.add(stall);
            }
            for (String[] r : read("users")) {
                User user = switch (r[3]) {
                    case "Admin" -> new Admin(r[0], r[1], r[2]);
                    case "Vendor" -> new Vendor(r[0], r[1], r[2], number(r, 4));
                    case "Buyer", "Student" -> new Buyer(r[0], r[1], r[2]);
                    default -> throw new IOException("Unknown role: " + r[3]);
                };
                user.setActive(Boolean.parseBoolean(r[5]));
                system.users.add(user);
            }
            for (String[] r : read("products")) {
                MenuItem item;
                if (r[4].equals("Drink")) {
                    item = new Drink(number(r, 0), number(r, 1), r[2], r[3], number(r, 5), number(r, 6));
                } else {
                    item = new Food(number(r, 0), number(r, 1), r[2], r[3], number(r, 5), number(r, 6));
                }
                if (!Boolean.parseBoolean(r[7])) {
                    item.remove();
                }
                system.items.add(item);
            }
            for (String[] r : read("purchases")) {
                PurchaseRecord record = new PurchaseRecord(number(r, 0), r[1], number(r, 2),
                    number(r, 3), r[4], r[5], number(r, 6), number(r, 7),
                    LocalDateTime.parse(r[8]), OrderStatus.fromCsv(r[9]));
                if (r.length > 11) record.setReceiptId(r[11]);
                if (r.length > 12) record.setInstructions(r[12]);
                if (r.length > 13) record.setCancellationReason(r[13]);
                system.orders.add(record);
            }
            if (Files.exists(DATA.resolve("registration_requests.csv"))) {
                for (String[] r : read("registration_requests")) {
                    if (r.length < 8) continue;
                    system.registrations.add(new RegistrationRequest(number(r, 0), r[1], r[2],
                        r[3], r[4], number(r, 5), LocalDateTime.parse(r[6]),
                        RegistrationRequest.Status.valueOf(r[7])));
                }
            }
            if (Files.exists(DATA.resolve("help_requests.csv"))) {
                for (String[] r : read("help_requests")) {
                    if (r.length < 6) continue;
                    system.helpRequests.add(new HelpRequest(number(r, 0), r[1], r[2], r[3],
                        LocalDateTime.parse(r[4]), Boolean.parseBoolean(r[5])));
                }
            }
            if (Files.exists(DATA.resolve("stall_feedback.csv"))) {
                for (String[] r : read("stall_feedback")) {
                    system.feedbacks.add(new StallFeedback(number(r, 0), r[1], number(r, 2),
                        r[3], LocalDateTime.parse(r[4])));
                }
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid CSV data: " + e.getMessage(), e);
        }
        return system;
    }

    static void saveData(WaitABiteSystem system) throws IOException {
        List<String> stalls = new ArrayList<>();
        stalls.add(row("ID", "Name", "Open"));
        for (Stall s : system.stalls) {
            stalls.add(row(s.getId(), s.getName(), s.isOpen()));
        }
        List<String> users = new ArrayList<>();
        users.add(row("ID", "Name", "Demo password", "Role", "Stall ID", "Active"));
        for (User u : system.users) {
            int stall = u instanceof Vendor ? ((Vendor) u).getStall() : 0;
            users.add(row(u.getId(), u.getName(), u.getPassword(), u.role(), stall, u.isActive()));
        }
        List<String> products = new ArrayList<>();
        products.add(row("ID", "Stall ID", "Name", "Category", "Type", "Price PHP", "Stock", "Active"));
        for (MenuItem m : system.items) {
            products.add(row(m.getId(), m.getStall(), m.getName(), m.getCategory(), m.type(), m.getPrice(), m.getStock(), m.isActive()));
        }
        List<String> purchases = new ArrayList<>();
        purchases.add(row("Order ID", "Buyer ID", "Product ID", "Stall ID", "Product", "Type", "Unit price PHP", "Quantity", "Time", "Status", "Total PHP", "Receipt ID", "Instructions", "Cancellation Reason"));
        for (PurchaseRecord o : system.orders) {
            purchases.add(row(o.getId(), o.getBuyer(), o.getItem(), o.getStall(), o.getName(), o.getType(), o.getPrice(), o.getQuantity(), o.getTime(), o.getStatus(), o.total(), o.getReceiptId(), o.getInstructions(), o.getCancellationReason()));
        }
        List<String> feedbacks = new ArrayList<>();
        feedbacks.add(row("Feedback ID", "Buyer ID", "Stall ID", "Feedback", "Time"));
        for (StallFeedback f : system.feedbacks) {
            feedbacks.add(row(f.getId(), f.getBuyerId(), f.getStallId(), f.getMessage(), f.getTime()));
        }
        write("stalls", stalls);
        write("users", users);
        write("products", products);
        write("purchases", purchases);
        List<String> registrations = new ArrayList<>();
        registrations.add(row("Request ID", "User ID", "Name", "Demo password", "Role", "Stall ID", "Requested At", "Status"));
        for (RegistrationRequest r : system.registrations) {
            registrations.add(row(r.getId(), r.getUserId(), r.getName(), r.getPassword(),
                r.getRole(), r.getStallId(), r.getRequestedAt(), r.getStatus()));
        }
        List<String> help = new ArrayList<>();
        help.add(row("Help ID", "Sender ID", "Role", "Message", "Time", "Resolved"));
        for (HelpRequest h : system.helpRequests) {
            help.add(row(h.getId(), h.getSenderId(), h.getSenderRole(), h.getMessage(),
                h.getTime(), h.isResolved()));
        }
        write("stall_feedback", feedbacks);
        write("registration_requests", registrations);
        write("help_requests", help);
    }

    private static String row(Object... values) {
        StringJoiner result = new StringJoiner(",");
        for (Object value : values) {
            result.add("\"" + value.toString().replace("\"", "\"\"") + "\"");
        }
        return result.toString();
    }

    private static List<String[]> read(String name) throws IOException {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(DATA.resolve(name + ".csv"))) {
            reader.readLine(); // Skip column headers.
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    records.add(parse(line));
                }
            }
        }
        return records;
    }

    private static String[] parse(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new IOException("Unclosed CSV quotation mark");
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private static int number(String[] row, int column) {
        return Integer.parseInt(row[column]);
    }

    private static void write(String name, List<String> rows) throws IOException {
        Files.createDirectories(DATA);
        Path target = DATA.resolve(name + ".csv");
        Path temporary = DATA.resolve(name + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
            for (String line : rows) {
                writer.write(line);
                writer.newLine();
            }
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
