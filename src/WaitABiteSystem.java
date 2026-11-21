import java.util.*;
class WaitABiteSystem {
    final ArrayList<User> users=new ArrayList<>();
    final ArrayList<Stall> stalls=new ArrayList<>();
    final ArrayList<MenuItem> items=new ArrayList<>();
    final ArrayList<PurchaseRecord> orders=new ArrayList<>();
    final ArrayList<StallFeedback> feedbacks=new ArrayList<>();
    final ArrayList<RegistrationRequest> registrations = new ArrayList<>();
    final ArrayList<HelpRequest> helpRequests = new ArrayList<>();

    void requestRegistration(String id, String name, String password, String role, int stallId)
            throws InvalidOrderException {
        id = id == null ? "" : id.trim();
        name = name == null ? "" : name.trim();
        if (!id.matches("[A-Za-z0-9_.-]{3,32}"))
            throw new InvalidOrderException("User ID must have 3-32 letters, digits, dots, dashes or underscores.");
        if (name.isBlank() || name.length() > 80 || name.contains(",") || name.contains("\n"))
            throw new InvalidOrderException("Enter a valid name (1-80 characters, without commas).");
        if (password == null || password.length() < 6 || password.length() > 80
                || password.contains("\n") || password.contains("\r"))
            throw new InvalidOrderException("Password must contain 6-80 characters without line breaks.");
        if (!"Buyer".equals(role) && !"Vendor".equals(role))
            throw new InvalidOrderException("Self-registration is available only for Buyers and Vendors.");
        if ("Vendor".equals(role) && stalls.stream().noneMatch(s -> s.getId() == stallId))
            throw new InvalidOrderException("Select a valid vendor stall.");
        final String requestedId = id;
        if (user(id) != null || registrations.stream().anyMatch(r ->
                r.getStatus() == RegistrationRequest.Status.PENDING && r.getUserId().equalsIgnoreCase(requestedId)))
            throw new InvalidOrderException("This ID already exists or has a pending application.");
        int next = registrations.stream().mapToInt(RegistrationRequest::getId).max().orElse(0) + 1;
        registrations.add(new RegistrationRequest(next, id, name, password, role,
            "Vendor".equals(role) ? stallId : 0, java.time.LocalDateTime.now(),
            RegistrationRequest.Status.PENDING));
    }

    void approveRegistration(int registrationId) throws InvalidOrderException {
        RegistrationRequest request = registrations.stream()
            .filter(r -> r.getId() == registrationId).findFirst().orElse(null);
        if (request == null || request.getStatus() != RegistrationRequest.Status.PENDING)
            throw new InvalidOrderException("Request is no longer pending.");
        if (user(request.getUserId()) != null)
            throw new InvalidOrderException("An account with this ID already exists.");
        User account = "Vendor".equals(request.getRole())
            ? new Vendor(request.getUserId(), request.getName(), request.getPassword(), request.getStallId())
            : new Buyer(request.getUserId(), request.getName(), request.getPassword());
        users.add(account);
        request.approve();
    }

    void rejectRegistration(int registrationId) throws InvalidOrderException {
        RegistrationRequest request = registrations.stream()
            .filter(r -> r.getId() == registrationId).findFirst().orElse(null);
        if (request == null || request.getStatus() != RegistrationRequest.Status.PENDING)
            throw new InvalidOrderException("Request is no longer pending.");
        request.reject();
    }

    RegistrationRequest.Status registrationStatus(String userId) {
        RegistrationRequest latest = null;
        for (RegistrationRequest request : registrations)
            if (request.getUserId().equalsIgnoreCase(userId)
                && (latest == null || request.getId() > latest.getId())) latest = request;
        return latest == null ? null : latest.getStatus();
    }

    void submitHelp(User sender, String message) throws InvalidOrderException {
        if (!(sender instanceof Buyer) && !(sender instanceof Vendor))
            throw new InvalidOrderException("Only Buyers and Vendors can submit help requests.");
        String cleaned = message == null ? "" : message.replaceAll("[\r\n\t]+", " ").trim();
        if (cleaned.isBlank() || cleaned.length() > 500)
            throw new InvalidOrderException("Enter a help message of 1-500 characters.");
        int next = helpRequests.stream().mapToInt(HelpRequest::getId).max().orElse(0) + 1;
        helpRequests.add(new HelpRequest(next, sender.getId(), sender.role(), cleaned,
            java.time.LocalDateTime.now(), false));
    }

    void submitLandingHelp(String senderId, String accountType, String message)
            throws InvalidOrderException {
        String contact = senderId == null ? "" : senderId.replaceAll("[\r\n\t]+", " ").trim();
        String cleaned = message == null ? "" : message.replaceAll("[\r\n\t]+", " ").trim();
        if (contact.isBlank() || contact.length() > 80)
            throw new InvalidOrderException("Enter your name or User ID (1-80 characters).");
        if (cleaned.isBlank() || cleaned.length() > 500)
            throw new InvalidOrderException("Enter a help message of 1-500 characters.");
        if (!"Buyer".equals(accountType) && !"Vendor".equals(accountType)
                && !"Other".equals(accountType))
            throw new InvalidOrderException("Select a valid account type.");
        int next = helpRequests.stream().mapToInt(HelpRequest::getId).max().orElse(0) + 1;
        helpRequests.add(new HelpRequest(next, contact, accountType + " (unverified)", cleaned,
            java.time.LocalDateTime.now(), false));
    }

    void resolveHelp(int id) throws InvalidOrderException {
        HelpRequest request = helpRequests.stream().filter(h -> h.getId() == id)
            .findFirst().orElse(null);
        if (request == null) throw new InvalidOrderException("Help request not found.");
        request.resolve();
    }

    Stall stall(int id) {
        return stalls.get(id-1);
    }
    MenuItem item(int id) {
        return items.stream().filter(m->m.getId()==id).findFirst().orElse(null);
    }
    User user(String id) {
        return users.stream().filter(u->u.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    void purchase(Buyer b,int id,int n)throws InvalidOrderException {
        MenuItem m=item(id);
        if (m==null||!m.isActive()||!stall(m.getStall()).isOpen()||m.getPrice()<0||n<=0||n>m.getStock())throw new InvalidOrderException("Item unavailable, price unknown or insufficient stock.");
        m.deduct(n);
        orders.add(new PurchaseRecord(orders.size()+1,b.getId(),m,n));
    }
    String checkout(Buyer buyer, java.util.Map<Integer, Integer> cart) throws InvalidOrderException {
        return checkout(buyer, cart, java.util.Collections.emptyMap());
    }

    String checkout(Buyer buyer, java.util.Map<Integer, Integer> cart,
                    java.util.Map<Integer, String> instructions) throws InvalidOrderException {
        if (cart == null || cart.isEmpty()) throw new InvalidOrderException("Cart is empty.");
        for (java.util.Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            MenuItem item = item(entry.getKey());
            int quantity = entry.getValue();
            if (item == null || !item.isActive() || !stall(item.getStall()).isOpen()
                || item.getPrice() < 0 || quantity <= 0 || quantity > item.getStock()) {
                throw new InvalidOrderException("Unavailable item, unknown price, closed stall or insufficient stock: "
                    + (item == null ? entry.getKey() : item.getName()));
            }
        }
        String receipt = "WAB-" + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        int nextId = orders.stream().mapToInt(PurchaseRecord::getId).max().orElse(0) + 1;
        for (java.util.Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            MenuItem item = item(entry.getKey());
            item.deduct(entry.getValue());
            PurchaseRecord record = new PurchaseRecord(nextId++, buyer.getId(), item, entry.getValue());
            record.setReceiptId(receipt);
            if (instructions != null) {
                record.setInstructions(instructions.get(entry.getKey()));
            }
            orders.add(record);
        }
        return receipt;
    }

    int stallOrderNumber(PurchaseRecord target) {
        Map<Integer, Integer> nextByStall = new HashMap<>();
        Map<String, Integer> grouped = new HashMap<>();
        List<PurchaseRecord> sorted = new ArrayList<>(orders);
        sorted.sort(Comparator.comparingInt(PurchaseRecord::getId));
        for (PurchaseRecord record : sorted) {
            String receipt = record.getReceiptId().isBlank()
                ? "legacy-" + record.getId() : record.getReceiptId();
            String key = record.getStall() + "|" + receipt;
            Integer localNumber = grouped.get(key);
            if (localNumber == null) {
                localNumber = nextByStall.getOrDefault(record.getStall(), 0) + 1;
                nextByStall.put(record.getStall(), localNumber);
                grouped.put(key, localNumber);
            }
            if (record == target || record.getId() == target.getId()) return localNumber;
        }
        return 0;
    }

    void submitFeedback(Buyer buyer, int stallId, String message) throws InvalidOrderException {
        if (buyer == null) throw new InvalidOrderException("Buyer login is required.");
        if (stallId < 1 || stalls.stream().noneMatch(s -> s.getId() == stallId))
            throw new InvalidOrderException("Unknown stall.");
        String cleaned = message == null ? "" : message.replaceAll("[\r\n\t]+", " ").trim();
        if (cleaned.isEmpty()) throw new InvalidOrderException("Please enter your feedback.");
        if (cleaned.length() > 500) throw new InvalidOrderException("Feedback must be at most 500 characters.");
        int nextId = feedbacks.stream().mapToInt(StallFeedback::getId).max().orElse(0) + 1;
        feedbacks.add(new StallFeedback(nextId, buyer.getId(), stallId, cleaned,
            java.time.LocalDateTime.now()));
    }

    void completeStallOrder(Vendor vendor, int recordId) throws InvalidOrderException {
        List<PurchaseRecord> group = pendingStallOrder(vendor, recordId);
        for (PurchaseRecord record : group) record.advance();
    }

    private List<PurchaseRecord> pendingStallOrder(Vendor vendor, int recordId)
            throws InvalidOrderException {
        if (vendor == null) throw new InvalidOrderException("Vendor login is required.");
        PurchaseRecord selected = orders.stream().filter(o -> o.getId() == recordId)
            .findFirst().orElse(null);
        if (selected == null || selected.getStall() != vendor.getStall())
            throw new InvalidOrderException("Order not found for your stall.");
        String receipt = selected.getReceiptId();
        List<PurchaseRecord> group = new ArrayList<>();
        for (PurchaseRecord record : orders) {
            if (record.getStall() != vendor.getStall()) continue;
            if (receipt.isBlank() ? record.getId() == recordId : receipt.equals(record.getReceiptId()))
                group.add(record);
        }
        if (group.isEmpty() || group.stream().anyMatch(o -> o.getStatus() != OrderStatus.PENDING))
            throw new InvalidOrderException("Only an entirely PENDING stall order can be changed.");
        return group;
    }

    void cancelOrder(Vendor vendor, int orderId, String reason) throws InvalidOrderException {
        List<PurchaseRecord> group = pendingStallOrder(vendor, orderId);
        if (reason == null || reason.isBlank())
            throw new InvalidOrderException("Please enter a cancellation reason.");
        Map<MenuItem, Integer> quantities = new LinkedHashMap<>();
        for (PurchaseRecord record : group) {
            MenuItem product = item(record.getItem());
            if (product == null || product.getStall() != vendor.getStall())
                throw new InvalidOrderException("Product was not found; stock cannot be restored automatically.");
            quantities.merge(product, record.getQuantity(), Integer::sum);
        }
        Map<MenuItem, Integer> restored = new LinkedHashMap<>();
        try {
            for (Map.Entry<MenuItem, Integer> entry : quantities.entrySet()) {
                restored.put(entry.getKey(), Math.addExact(entry.getKey().getStock(), entry.getValue()));
            }
        } catch (ArithmeticException ex) {
            throw new InvalidOrderException("Could not restore stock for this order.");
        }
        for (Map.Entry<MenuItem, Integer> entry : restored.entrySet())
            entry.getKey().updateInfo(entry.getKey().getPrice(), entry.getValue());
        for (PurchaseRecord record : group) record.cancel(reason);
    }

    void report(int stall) {
        long sales=0;
        int pending=0;
        Map<Integer,Integer> counts=new HashMap<>();
        Map<String,Integer> categories=new TreeMap<>();
        Map<Integer,Integer> demand=new TreeMap<>();
        for (MenuItem m:items)if (stall==0||m.getStall()==stall)counts.put(m.getId(),0);
        for (PurchaseRecord o:orders)if (stall==0||o.getStall()==stall) {
            if (o.getStatus()!=OrderStatus.COMPLETED) {
                if (o.getStatus()==OrderStatus.PENDING) pending++;
                continue;
            }
            sales+=o.total();
            counts.merge(o.getItem(),o.getQuantity(),Integer::sum);
            categories.merge(o.getType(),o.getQuantity(),Integer::sum);
            demand.merge(o.getStall(),o.getQuantity(),Integer::sum);
        }
        System.out.println("Completed sales: PHP "+sales+" | Outstanding orders: "+pending);
        System.out.println("Units by category: "+categories);
        for (var e:demand.entrySet())System.out.println(stall(e.getKey()).getName()+": "+e.getValue()+" units");
        if (counts.isEmpty())return;
        var sorted=new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<Integer,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        System.out.println("Product ranking (highest to lowest, completed units):");
        for (var e:sorted)System.out.println(item(e.getKey()).getName()+" ["+stall(item(e.getKey()).getStall()).getName()+"]: "+e.getValue());
    }
}
