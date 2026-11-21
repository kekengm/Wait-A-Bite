import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import javax.swing.border.MatteBorder;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class WaitABiteFrame extends JFrame {

    private WaitABiteSystem system;
    private User currentUser;
    private JPanel body;
    private JPanel root;
    private final LinkedHashMap<Integer, Integer> cart = new LinkedHashMap<>();
    private final Map<Integer, String> cartInstructions = new HashMap<>();


    public WaitABiteFrame(WaitABiteSystem system) {
        this.system = system;
        setTitle("Wait A Bite");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 768);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        showRoles();
    }

    private void setRoot(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }

    private JPanel loginCard(String title) {
        JPanel card = Theme.card(new GridLayout(0, 1, 0, 14));
        card.setPreferredSize(new Dimension(400, 390));
        JLabel heading = Theme.label(title, 20, Theme.INK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(heading);
        return card;
    }

    private void showRoles() {
        currentUser = null;
        cart.clear();
        cartInstructions.clear();
 
        JPanel page = Theme.background(new BorderLayout());
        JPanel center = Theme.panel(new GridBagLayout());
        JPanel card = Theme.card(new BorderLayout(0, 14));
        JPanel roleButtons = Theme.panel(new GridLayout(3, 1, 0, 12));
        for (String role : new String[] {"Admin", "Vendor", "Buyer"}) {
            roleButtons.add(Theme.button("Log in as " + role, () -> showLogin(role)));
        }
        card.add(roleButtons, BorderLayout.CENTER);
        card.add(Theme.button("CREATE ACCOUNT (ADMIN APPROVAL)", this::showRegister), BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(410, 290));
        center.add(card);
        page.add(center, BorderLayout.CENTER);

        JPanel footer = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setBorder(new EmptyBorder(0, 16, 22, 24));
        footer.add(Theme.button("HELP", this::sendLandingHelp));
        page.add(footer, BorderLayout.SOUTH);
        setRoot(page);
    }

    private void showRegister() {
        JPanel page = Theme.background(new GridBagLayout());
        JPanel card = Theme.card(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(480, 440));
        JPanel fields = Theme.panel(new GridLayout(0, 1, 0, 5));
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[] {"Buyer", "Vendor"});
        JComboBox<String> stalls = new JComboBox<>();
        for (Stall stall : system.stalls) stalls.addItem(stall.getName());
        fields.add(Theme.label("CREATE ACCOUNT | Admin approval required", 18, Theme.INK));
        fields.add(Theme.label("User ID", 12, Theme.INK));
        fields.add(id);
        fields.add(Theme.label("Full name", 12, Theme.INK));
        fields.add(name);
        fields.add(Theme.label("Password (6+ characters)", 12, Theme.INK));
        fields.add(password);
        fields.add(Theme.label("Account type", 12, Theme.INK));
        fields.add(role);
        fields.add(Theme.label("Vendor stall (only for Vendor accounts)", 12, Theme.INK));
        fields.add(stalls);
        stalls.setEnabled(false);
        role.addActionListener(e -> stalls.setEnabled("Vendor".equals(role.getSelectedItem())));
        card.add(fields, BorderLayout.CENTER);
        JPanel actions = Theme.panel(new GridLayout(1, 2, 8, 0));
        actions.add(Theme.button("BACK", this::showRoles));
        actions.add(Theme.button("SUBMIT REQUEST", () -> {
            try {
                system.requestRegistration(id.getText(), name.getText(),
                    new String(password.getPassword()), String.valueOf(role.getSelectedItem()),
                    "Vendor".equals(role.getSelectedItem()) ? stalls.getSelectedIndex() + 1 : 0);
                if (save()) {
                    message("Registration submitted! Your account cannot log in until Admin approves it.");
                    showRoles();
                }
            } catch (InvalidOrderException ex) { message(ex.getMessage()); }
        }));
        card.add(actions, BorderLayout.SOUTH);
        page.add(card);
        setRoot(page);
    }

    private void showLogin(String role) {
        JPanel page = Theme.background(new GridBagLayout());
        JPanel card = loginCard(role + " Login");
        JTextField id = new JTextField();
        JPasswordField password = new JPasswordField();
        card.add(Theme.label(role.equals("Buyer") ? "Buyer ID" : "User ID (UID)", 14, Theme.INK));
        card.add(id);
        card.add(Theme.label("Password", 14, Theme.INK));
        JPanel passwordRow = Theme.panel(new BorderLayout(6, 0));
        passwordRow.add(password, BorderLayout.CENTER);
        JButton eye = new JButton("Show");
        char echo = password.getEchoChar();
        eye.addActionListener(e -> {
            boolean visible = password.getEchoChar() == (char) 0;
            password.setEchoChar(visible ? echo : (char) 0);
            eye.setText(visible ? "Show" : "Hide");
        });
        passwordRow.add(eye, BorderLayout.EAST);
        card.remove(password);
        card.add(passwordRow);
        JButton login = Theme.button("Log in", () -> {
            User user = system.user(id.getText().trim());
            if (user == null || !user.role().equals(role) || !user.login(new String(password.getPassword()))) {
                RegistrationRequest.Status pending = system.registrationStatus(id.getText().trim());
                if (user == null && pending == RegistrationRequest.Status.PENDING)
                    message("Your registration is pending Admin approval. You cannot log in yet.");
                else if (user == null && pending == RegistrationRequest.Status.REJECTED)
                    message("Your registration was declined. You can submit a new request.");
                else message("Invalid user ID, password or role.");
                return;
            }
            currentUser = user;
            showShell();
        });
        card.add(login);
        card.add(Theme.button("Back", this::showRoles));
        card.setPreferredSize(new Dimension(400, 400));
        page.add(card);
        setRoot(page);
        getRootPane().setDefaultButton(login);
    }

    private int scope() {
        return currentUser instanceof Vendor ? ((Vendor) currentUser).getStall() : 0;
    }

    private void showShell() {
        root = Theme.background(new BorderLayout(0, 0));
        root.putClientProperty("dashboard", true);
        body = Theme.panel(new BorderLayout(12, 12));
        body.setBorder(new EmptyBorder(22, 24, 18, 24));
        root.add(body, BorderLayout.CENTER);

        JPanel side = Theme.panel(new BorderLayout(0, 18));
        side.setPreferredSize(new Dimension(255, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 2, new Color(255, 230, 230, 175)),
            new EmptyBorder(20, 12, 16, 14)));
        side.add(Theme.brandLogo(225), BorderLayout.NORTH);

        JPanel links = Theme.panel(new GridLayout(0, 1, 0, 12));
        JPanel navWrapper = Theme.panel(new BorderLayout());
        if (currentUser instanceof Buyer) {
            links.add(Theme.button("HOME", this::showHome));
            links.add(Theme.button("STALLS", this::showStalls));
            links.add(Theme.button("MY ORDERS", this::showOrders));
            links.add(Theme.button("CART / CHECKOUT", this::showCart));
        } else if (currentUser instanceof Admin) {
            links.add(Theme.button("DASHBOARD", this::showHome));
            links.add(Theme.button("STALLS", this::showStalls));
            links.add(Theme.button("SALES ANALYTICS", this::showSales));
            links.add(Theme.button("ACCOUNT MANAGEMENT", this::showSettings));
            links.add(Theme.button("REGISTRATION REQUESTS", this::showRegistrations));
            links.add(Theme.button("HELP REQUESTS", this::showHelpRequests));
        } else {
            links.add(Theme.button("OVERVIEW", this::showHome));
            links.add(Theme.button("MY PRODUCTS", () -> showProducts(scope(), "", "")));
            links.add(Theme.button("SALES ANALYTICS", this::showSales));
            links.add(Theme.button("STALL FEEDBACK", this::showFeedback));
        }
        navWrapper.add(links, BorderLayout.NORTH);
        side.add(navWrapper, BorderLayout.CENTER);
        JPanel bottom = Theme.panel(new GridLayout(0, 1, 0, 10));
        if (currentUser instanceof Vendor) {
            bottom.add(Theme.button("Toggle Stall Availability", this::toggleOwnStall));
        }
        bottom.add(Theme.button("LOGOUT", () -> {
            if (confirm("Log out", "Log out of this account?")) showRoles();
        }));
        JLabel note = new JLabel("For academic purposes only", SwingConstants.CENTER);
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setForeground(Color.WHITE);
        bottom.add(note);
        side.add(bottom, BorderLayout.SOUTH);
        root.add(side, BorderLayout.WEST);
        setRoot(root);
        showHome();
    }

    private void toggleOwnStall() {
        if (!(currentUser instanceof Vendor)) return;
        Stall stall = system.stall(scope());
        String next = stall.isOpen() ? "Non-Visible" : "Visible";
        if (!confirm("Stall Availability", "Change " + stall.getName() + " to " + next + "?")) return;
        stall.toggle();
        if (save()) showHome();
    }

    private boolean confirm(String title, String detail) {
        return JOptionPane.showConfirmDialog(this, detail, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void page(JPanel content) {
        body.removeAll();
        body.add(content, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    private JPanel section(String title) {
        JPanel panel = Theme.card(new BorderLayout(10, 10));
        panel.add(Theme.label(title, 24, Theme.INK), BorderLayout.NORTH);
        return panel;
    }

    private String vendorGreetingName() {
        if (!(currentUser instanceof Vendor)) return "Vendor";
        String name = system.stall(scope()).getName().trim();
        if (name.regionMatches(true, 0, "CAFE ", 0, 5)) name = name.substring(5);
        String[] words = name.split("\\s+");
        StringBuilder display = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (display.length() > 0) display.append(' ');
            if (word.length() <= 2) display.append(word.toUpperCase(java.util.Locale.ROOT));
            else display.append(word.substring(0, 1).toUpperCase(java.util.Locale.ROOT))
                .append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return display.toString();
    }

    private void showHome() {
        if (currentUser instanceof Buyer) {
            showBuyerHome();
            return;
        }
        JPanel content = Theme.panel(new BorderLayout(12, 12));
        JPanel top = Theme.panel(new GridLayout(2, 1, 0, 12));
        JPanel greeting = Theme.card(new BorderLayout(0, 8));
        String greetingTitle = currentUser instanceof Admin
            ? "Welcome back, ADMIN!" : "Aprons tied, " + vendorGreetingName() + "?";
        String greetingSubtitle = currentUser instanceof Admin
            ? "Everything is running butter than expected today."
            : "Let's make today's menu impossible to resist.";
        JPanel greetingLines = Theme.panel(new GridLayout(2, 1, 0, 5));
        greetingLines.add(Theme.label(greetingTitle, 30, Theme.INK));
        greetingLines.add(Theme.label(greetingSubtitle, 20, Theme.INK));
        greeting.add(greetingLines, BorderLayout.NORTH);
        JPanel greetingInfo = Theme.panel(new BorderLayout(10, 0));
        
        if (currentUser instanceof Vendor) {
            Stall vendorStall = system.stall(scope());
            JLabel status = Theme.label("Store Status: " + (vendorStall.isOpen() ? "Visible" : "Non-Visible"), 18,
                vendorStall.isOpen() ? new Color(21, 112, 65) : new Color(185, 45, 45));
            greetingInfo.add(status, BorderLayout.EAST);
        }
        greeting.add(greetingInfo, BorderLayout.CENTER);
        top.add(greeting);
        JPanel metrics = Theme.panel(new GridLayout(1, 3, 14, 0));
        long sales = 0;
        int pending = 0;
        int today = 0;
        int products = 0;
        int active = 0;
        for (PurchaseRecord order : system.orders) {
            if (scope() != 0 && order.getStall() != scope()) continue;
            boolean isToday = order.getTime().toLocalDate().equals(LocalDate.now());
            if (isToday) today++;
            if (order.getStatus() == OrderStatus.PENDING) pending++;
            if (order.getStatus() == OrderStatus.COMPLETED && (scope() == 0 || isToday)) sales += order.total();
        }
        for (MenuItem item : system.items) {
            if (item.isActive() && (scope() == 0 || item.getStall() == scope())) products++;
        }
        for (Stall stall : system.stalls) if (stall.isOpen()) active++;
        metrics.add(metric(scope() == 0 ? "Total Sales" : "Today's Sales", "PHP " + sales));
        metrics.add(metric(scope() == 0 ? "Active Stalls" : "Pending Orders", "" + (scope() == 0 ? active : pending)));
        metrics.add(metric(scope() == 0 ? "Orders Today" : "Products Listed", "" + (scope() == 0 ? today : products)));
        top.add(metrics);
        content.add(top, BorderLayout.NORTH);
        if (scope() == 0) {
            JPanel chart = section("Total Sales Overview | Last 7 Days");
            chart.add(new SalesChart(system), BorderLayout.CENTER);
            JPanel lower = Theme.panel(new GridLayout(1, 2, 12, 0));
            lower.add(chart);
            lower.add(stallSalesPanel());
            content.add(lower, BorderLayout.CENTER);
        } else {
            content.add(ordersPanel(true), BorderLayout.CENTER);
        }
        page(content);
    }

    private JPanel stallSalesPanel() {
        JPanel panel = section("Sales Contribution by Stall (Highest to Lowest)");
        JTable table = table("Stall", "Completed Sales (PHP)");
        ArrayList<Stall> stalls = new ArrayList<>(system.stalls);
        Map<Integer, Long> totals = new HashMap<>();
        for (PurchaseRecord order : system.orders) {
            if (order.getStatus() == OrderStatus.COMPLETED) {
                totals.merge(order.getStall(), order.total(), Long::sum);
            }
        }
        stalls.sort(Comparator.comparingLong((Stall stall) -> totals.getOrDefault(stall.getId(), 0L)).reversed());
        for (Stall stall : stalls) addRow(table, stall.getName(), totals.getOrDefault(stall.getId(), 0L));
        panel.add(new JScrollPane(table));
        return panel;
    }

    private JPanel metric(String title, String value) {
        JPanel panel = section(title);
        panel.add(Theme.label(value, 32, Theme.INK), BorderLayout.CENTER);
        return panel;
    }

    private void showBuyerHome() {
        JPanel content = Theme.panel(new BorderLayout(12, 12));
        JPanel top = Theme.panel(new BorderLayout(0, 8));
        JPanel buyerGreeting = Theme.card(new GridLayout(2, 1, 0, 5));
        buyerGreeting.add(Theme.label("Welcome back, Red Spartan!", 23, Theme.INK));
        buyerGreeting.add(Theme.label("Good food is just a few taps away. What are we craving?", 17, Theme.INK));
        buyerGreeting.setPreferredSize(new Dimension(1, 88));
        top.add(buyerGreeting, BorderLayout.NORTH);
        JPanel search = Theme.panel(new BorderLayout(8, 0));
        JTextField field = new JTextField();
        search.add(field);
        search.add(Theme.button("Search foods, drinks and more", () -> showProducts(0, field.getText(), "")), BorderLayout.EAST);
        search.setPreferredSize(new Dimension(1, 50));
        top.add(search, BorderLayout.CENTER);
        JPanel categories = Theme.panel(new GridLayout(1, 3, 12, 0));
        categories.add(Theme.button("FOODS", () -> showProducts(0, "", "Food")));
        categories.add(Theme.button("DRINKS", () -> showProducts(0, "", "Drink")));
        categories.add(Theme.button("MY ORDERS", this::showOrders));
        categories.setPreferredSize(new Dimension(1, 52));
        top.add(categories, BorderLayout.SOUTH);
        content.add(top, BorderLayout.NORTH);
        JPanel daily = Theme.panel(new GridLayout(1, 2, 16, 0));
        daily.add(dailyBestSellersPanel("Food", "TOP 3 FOODS TODAY"));
        daily.add(dailyBestSellersPanel("Drink", "TOP 3 DRINKS / BEVERAGES TODAY"));
        content.add(daily, BorderLayout.CENTER);
        content.add(campusNote(), BorderLayout.SOUTH);
        page(content);
    }

    private JPanel dailyBestSellersPanel(String type, String title) {
        JPanel panel = section(title);
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<Integer, Integer> sold = new HashMap<>();
        for (PurchaseRecord order : system.orders) {
            if (order.getStatus() != OrderStatus.COMPLETED
                || !order.getTime().toLocalDate().equals(today)
                || !type.equals(order.getType())) {
                continue;
            }
            sold.merge(order.getItem(), order.getQuantity(), Integer::sum);
        }

        ArrayList<MenuItem> ranked = new ArrayList<>();
        for (MenuItem item : system.items) {
            if (type.equals(item.type()) && sold.getOrDefault(item.getId(), 0) > 0) {
                ranked.add(item);
            }
        }
        ranked.sort(Comparator
            .comparingInt((MenuItem item) -> sold.getOrDefault(item.getId(), 0))
            .reversed()
            .thenComparing(MenuItem::getName)
            .thenComparingInt(MenuItem::getId));

        JPanel list = Theme.panel(new GridLayout(0, 1, 0, 12));
        if (ranked.isEmpty()) {
            JPanel empty = Theme.card(new BorderLayout());
            empty.add(Theme.label("No completed sales for this category today yet.",
                14, Theme.INK), BorderLayout.CENTER);
            list.add(empty);
        } else {
            for (int i = 0; i < Math.min(3, ranked.size()); i++) {
                MenuItem item = ranked.get(i);
                JPanel row = Theme.card(new BorderLayout(12, 4));
                JPanel info = Theme.panel(new GridLayout(0, 1, 0, 4));
                info.add(Theme.label((i + 1) + ". " + item.getName(), 15, Theme.INK));
                info.add(Theme.label(system.stall(item.getStall()).getName()
                    + "  |  " + sold.get(item.getId()) + " sold today", 12, Theme.INK));
                String price = item.getPrice() < 0 ? "Price not set" : "PHP " + item.getPrice();
                info.add(Theme.label(price, 13, Theme.INK));
                row.add(info, BorderLayout.CENTER);
                JButton add = Theme.button("+ Add to Cart", () -> addDailyBestSeller(item));
                boolean available = item.isActive() && item.getPrice() >= 0
                    && item.getStock() > cart.getOrDefault(item.getId(), 0)
                    && system.stall(item.getStall()).isOpen();
                add.setEnabled(available);
                row.add(add, BorderLayout.EAST);
                list.add(row);
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void addDailyBestSeller(MenuItem item) {
        String input = JOptionPane.showInputDialog(this,
            "Quantity for " + item.getName() + " (" + system.stall(item.getStall()).getName() + ")", "1");
        if (input == null) return;
        try {
            int quantity = Integer.parseInt(input.trim());
            int updated = cart.getOrDefault(item.getId(), 0) + quantity;
            if (quantity <= 0 || !item.isActive() || item.getPrice() < 0
                || updated > item.getStock() || !system.stall(item.getStall()).isOpen()) {
                message("Invalid quantity, unavailable product, or insufficient stock.");
                return;
            }
            cart.put(item.getId(), updated);
            Object[] options = {"Keep Browsing", "View Cart / Checkout"};
            int choice = JOptionPane.showOptionDialog(this,
                item.getName() + " x" + quantity + " added to your cart.",
                "Added to Cart", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (choice == 1) showCart();
            else showBuyerHome();
        } catch (NumberFormatException ex) {
            message("Enter a valid whole-number quantity.");
        }
    }

    private JPanel buyerStallCards() {
        JPanel panel = section("EXPLORE STALLS  |  Select an image to view the menu");
        JPanel grid = Theme.panel(new GridLayout(0, 4, 16, 16));
        for (Stall stall : system.stalls) {
            JPanel tile = Theme.card(new BorderLayout(0, 6));
            tile.setPreferredSize(new Dimension(205, 270));
            JButton imageButton = new JButton(new StallImageIcon(stall.getId(), 166, 165));
            imageButton.setPreferredSize(new Dimension(175, 169));
            imageButton.setBorderPainted(false);
            imageButton.setContentAreaFilled(false);
            imageButton.setFocusPainted(false);
            imageButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imageButton.setToolTipText("View " + stall.getName() + " menu");
            imageButton.getAccessibleContext().setAccessibleName("Open " + stall.getName() + " menu");
            imageButton.addActionListener(e -> showProducts(stall.getId(), "", ""));
            tile.add(imageButton, BorderLayout.CENTER);
            JPanel footer = Theme.panel(new BorderLayout(0, 2));
            JLabel name = Theme.label(stall.getName(), 12, Theme.INK);
            name.setHorizontalAlignment(SwingConstants.CENTER);
            footer.add(name, BorderLayout.CENTER);
            JLabel status = Theme.label(stall.isOpen() ? "OPEN" : "CLOSED", 11,
                stall.isOpen() ? new Color(30, 115, 55) : Theme.RED);
            status.setHorizontalAlignment(SwingConstants.CENTER);
            footer.add(status, BorderLayout.SOUTH);
            JPanel footerWithFeedback = Theme.panel(new BorderLayout(0, 5));
            footerWithFeedback.add(footer, BorderLayout.CENTER);
            footerWithFeedback.add(Theme.button("Send Feedback", () -> sendFeedback(stall.getId())),
                BorderLayout.SOUTH);
            tile.add(footerWithFeedback, BorderLayout.SOUTH);
            grid.add(tile);
        }
        JPanel alignTop = Theme.panel(new BorderLayout());
        alignTop.add(grid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(alignTop);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static final String[] STALL_IMAGES = {
        "", "CafeAromatiko.png", "TenderJuicyHotdog.png", "RR Sorellas Homemade.png",
        "Fried Noodles Haus.png", "PotatoCorner.png", "RC Beef Shawarma.png",
        "JuliesBakery.png", "Ash-Mins Menu.png", "Theatery.png", "Chowking.png",
        "Waffle Time.png", "JuiceBar.png", "Itsnok.png"
    };

    private static class StallImageIcon implements Icon {
        private final Image image;
        private final int width, height;
        StallImageIcon(int stallId, int width, int height) {
            this.width = width;
            this.height = height;
            Image found = null;
            if (stallId > 0 && stallId < STALL_IMAGES.length) {
                try {
                    found = javax.imageio.ImageIO.read(new java.io.File("assets", STALL_IMAGES[stallId]));
                } catch (IOException ignored) { }
            }
            image = found;
        }
        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        public void paintIcon(Component component, Graphics g, int x, int y) {
            if (image == null) return;
            int sourceW = image.getWidth(null), sourceH = image.getHeight(null);
            if (sourceW <= 0 || sourceH <= 0) return;
            double scale = Math.min((width - 4.0) / sourceW, (height - 4.0) / sourceH);
            int w = Math.max(1, (int) Math.round(sourceW * scale));
            int h = Math.max(1, (int) Math.round(sourceH * scale));
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, x + (width - w) / 2, y + (height - h) / 2, w, h, null);
            g2.dispose();
        }
    }

    private JPanel stallPanel() {
        if (currentUser instanceof Buyer) return buyerStallCards();
        JPanel panel = section("STALLS");
        JPanel rows = Theme.panel(new GridLayout(0, 1, 0, 10));
        for (Stall stall : system.stalls) {
            JPanel row = Theme.panel(new BorderLayout(10, 0));
            row.add(Theme.button(stall.getName(), () -> showProducts(stall.getId(), "", "")));
            JLabel status = Theme.label(stall.isOpen() ? "OPEN" : "CLOSED", 14, stall.isOpen() ? new Color(30, 115, 55) : Theme.RED);
            row.add(status, BorderLayout.EAST);
            if (currentUser instanceof Admin || scope() == stall.getId()) {
                row.add(Theme.button("Toggle", () -> {
                    if (!confirm("Change Stall Status", "Change " + stall.getName() + " to " + (stall.isOpen() ? "Non-Visible" : "Visible") + "?")) return;
                    stall.toggle();
                    if (save()) showStalls();
                }), BorderLayout.WEST);
            }
            rows.add(row);
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        panel.add(scroll);
        return panel;
    }

    private JPanel campusNote() {
        JPanel p = Theme.panel(new FlowLayout(FlowLayout.CENTER));
        p.add(Theme.label("Batangas State University: Alangilan Campus TNEU Canteen", 12, Color.WHITE));
        return p;
    }

    private void showStalls() {
        if (currentUser instanceof Buyer) {
            JPanel wrapper = Theme.panel(new BorderLayout(0, 8));
            wrapper.add(stallPanel(), BorderLayout.CENTER);
            wrapper.add(campusNote(), BorderLayout.SOUTH);
            page(wrapper);
        } else page(stallPanel());
    }

    private JTable table(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setForeground(Theme.INK);
        table.setBackground(new Color(255, 241, 241));
        table.setSelectionBackground(new Color(244, 177, 177));
        table.getTableHeader().setBackground(Theme.PINK);
        table.getTableHeader().setForeground(Theme.INK);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(234, 205, 205));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    private void centerProductColumns(JTable table) {
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centered);
        }
    }

    private void addRow(JTable table, Object... values) {
        ((DefaultTableModel) table.getModel()).addRow(values);
    }

    private void showProducts(int stall, String query, String type) {
        JPanel listing = productPanel(stall, query, type);
        if (currentUser instanceof Vendor) {
            JPanel wrap = Theme.panel(new BorderLayout());
            wrap.add(listing, BorderLayout.NORTH);
            page(wrap);
        } else {
            page(listing);
        }
    }

    private JPanel productPanel(int stall, String query, String type) {
        String title = stall == 0 ? "Menu | " + (type.isEmpty() ? "All products" : type)
            : system.stall(stall).getName() + (currentUser instanceof Buyer ? " | Menu" : " | My Products");
        JPanel panel = section(title);
        JTable table = table("ID", "Product", "Category", "Type", "Price (PHP)", "Stock", "Status");
        centerProductColumns(table);
        for (MenuItem item : system.items) {
            if (!item.isActive() || (stall != 0 && item.getStall() != stall)) continue;
            if (!type.isEmpty() && !item.type().equals(type)) continue;
            String searchable = item.getName() + " " + item.getCategory() + " " + system.stall(item.getStall()).getName();
            if (!searchable.toLowerCase().contains(query.toLowerCase())) continue;
            String status = item.getStock() == 0 ? "Out of stock" : item.getStock() <= 5 ? "Low stock" : "In stock";
            addRow(table, item.getId(), item.getName(), item.getCategory(), item.type(), item.getPrice() < 0 ? "Not set" : item.getPrice(), item.getStock(), status);
        }
        JScrollPane productScroll = new JScrollPane(table);
        if (currentUser instanceof Vendor) {
            int visibleRows = Math.min(12, Math.max(1, table.getRowCount()));
            int height = table.getTableHeader().getPreferredSize().height
                + visibleRows * table.getRowHeight() + 4;
            productScroll.setPreferredSize(new Dimension(700, height));
        }
        panel.add(productScroll, BorderLayout.CENTER);
        JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT));
        if (currentUser instanceof Buyer) {
            actions.add(Theme.button("Add to Cart", () -> addToCart(table, stall, query, type)));
            actions.add(Theme.button("View Cart / Checkout", this::showCart));
            if (stall > 0)
                actions.add(Theme.button("Send Stall Feedback", () -> sendFeedback(stall)));
        } else if (currentUser instanceof Admin || scope() == stall) {
            actions.add(Theme.button("+ Add Product", () -> editProduct(null, stall)));
            actions.add(Theme.button("Edit Product", () -> {
                MenuItem item = selectedProduct(table);
                if (item != null) editProduct(item, item.getStall());
            }));
            actions.add(Theme.button("Remove Product", () -> {
                MenuItem item = selectedProduct(table);
                if (item != null && confirm("Remove Product", "Remove " + item.getName() + " from the menu?")) {
                    item.remove();
                    if (save()) showProducts(stall, query, type);
                }
            }));
        }
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private MenuItem selectedProduct(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a product first.");
            return null;
        }
        return system.item((Integer) table.getValueAt(row, 0));
    }

    private void addToCart(JTable table, int stall, String query, String type) {
        MenuItem item = selectedProduct(table);
        if (item == null) return;
        String input = JOptionPane.showInputDialog(this, "Quantity for " + item.getName(), "1");
        if (input == null) return;
        try {
            int quantity = Integer.parseInt(input.trim());
            int updated = cart.getOrDefault(item.getId(), 0) + quantity;
            if (quantity <= 0 || !item.isActive() || item.getPrice() < 0 || updated > item.getStock()
                || !system.stall(item.getStall()).isOpen()) {
                message("Invalid quantity, unavailable product, or insufficient stock.");
                return;
            }
            cart.put(item.getId(), updated);
            Object[] options = {"Order More", "View Cart / Checkout"};
            int choice = JOptionPane.showOptionDialog(this,
                item.getName() + " x" + quantity + " added.\nCart: " + cart.size() + " different products across stalls.",
                "Added to Cart", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
            if (choice == 1) showCart();
            else showProducts(stall, query, type);
        } catch (NumberFormatException e) { message("Enter a valid whole-number quantity."); }
    }

    private void showCart() {
        JPanel panel = section("Your Cart | Multiple Stalls, One Receipt");
        JTable table = table("ID", "Stall", "Product", "Quantity", "Unit Price", "Subtotal", "Instructions");
        long total = 0;
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            MenuItem item = system.item(entry.getKey());
            if (item == null) continue;
            long subtotal = (long) item.getPrice() * entry.getValue();
            total += subtotal;
            addRow(table, item.getId(), system.stall(item.getStall()).getName(), item.getName(),
                entry.getValue(), item.getPrice(), subtotal, cartInstructions.getOrDefault(item.getId(), ""));
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = Theme.panel(new BorderLayout());
        footer.add(Theme.label("Total: PHP " + total, 22, Theme.INK), BorderLayout.WEST);
        JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(Theme.button("Order More", this::showStalls));
        actions.add(Theme.button("Add / Edit Instructions", () -> editCartInstructions(table)));
        actions.add(Theme.button("Remove Selected", () -> {
            int row = table.getSelectedRow();
            if (row < 0) { message("Select a product to remove."); return; }
            int id = (Integer) table.getValueAt(row, 0);
            if (confirm("Remove Cart Item", "Remove this item from your cart?")) {
                cart.remove(id);
                cartInstructions.remove(id);
                showCart();
            }
        }));
        actions.add(Theme.button("Clear Cart", () -> {
            if (!cart.isEmpty() && confirm("Clear Cart", "Remove all items from your cart?")) {
                cart.clear(); cartInstructions.clear(); showCart();
            }
        }));
        actions.add(Theme.button("Checkout / Pay at Stall", this::checkoutCart));
        footer.add(actions, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        page(panel);
    }

    private void editCartInstructions(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select an item in your cart first.");
            return;
        }
        int itemId = (Integer) table.getValueAt(row, 0);
        MenuItem item = system.item(itemId);
        if (item == null) return;
        JTextArea instructions = new JTextArea(cartInstructions.getOrDefault(itemId, ""), 4, 34);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel("Optional requests (e.g. no onions, less ice):"), BorderLayout.NORTH);
        form.add(new JScrollPane(instructions), BorderLayout.CENTER);
        form.add(new JLabel("Requests are not priced add-ons and require vendor approval."), BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, form, "Instructions: " + item.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String text = instructions.getText().replaceAll("[\r\n\t]+", " ").trim();
        if (text.length() > 180) {
            message("Please keep your instructions under 180 characters.");
            return;
        }
        if (text.isEmpty()) cartInstructions.remove(itemId);
        else cartInstructions.put(itemId, text);
        showCart();
    }

    private void checkoutCart() {
        if (!(currentUser instanceof Buyer) || cart.isEmpty()) {
            message("Your cart is empty."); return;
        }
        StringBuilder details = new StringBuilder("ORDER SUMMARY\n\n");
        long total = 0;
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            MenuItem item = system.item(entry.getKey());
            if (item == null) { message("An item in your cart is no longer available."); return; }
            long subtotal = (long) item.getPrice() * entry.getValue();
            total += subtotal;
            details.append(system.stall(item.getStall()).getName()).append("\n  ")
                .append(item.getName()).append(" x").append(entry.getValue())
                .append(" = PHP ").append(subtotal).append("\n");
            String instructions = cartInstructions.getOrDefault(item.getId(), "");
            if (!instructions.isEmpty()) details.append("  Instructions: ").append(instructions).append("\n");
        }
        details.append("\nTOTAL: PHP ").append(total)
            .append("\n\nOrders are recorded as pending. Pay at the respective stalls.\nConfirm checkout?");
        if (!confirm("Confirm Multi-Stall Checkout", details.toString())) return;
        try {
            String receiptId = system.checkout((Buyer) currentUser, cart, cartInstructions);
            if (!save()) {
                message("Checkout was applied in memory but saving failed. Do not repeat checkout; back up and retry saving.");
                return;
            }
            cart.clear();
            cartInstructions.clear();
            showReceipt(receiptId);
        } catch (InvalidOrderException e) { message("Checkout failed: " + e.getMessage()); showCart(); }
    }

    private void showReceipt(String receiptId) {
        JPanel panel = section("Combined Receipt | " + receiptId);
        JTable table = table("Stall", "Stall Order #", "Product", "Quantity", "Unit Price", "Subtotal", "Status", "Instructions", "Cancellation Reason");
        centerProductColumns(table);
        long total = 0;
        for (PurchaseRecord record : system.orders) {
            if (!receiptId.equals(record.getReceiptId())) continue;
            addRow(table, system.stall(record.getStall()).getName(),
                system.stallOrderNumber(record), record.getName(),
                record.getQuantity(), record.getPrice(), record.total(), record.getStatus(),
                record.getInstructions(), record.getCancellationReason());
            if (record.getStatus() != OrderStatus.CANCELLED) total += record.total();
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = Theme.panel(new BorderLayout());
        footer.add(Theme.label("Total payable: PHP " + total + "  |  Pay at each stall", 19, Theme.INK), BorderLayout.WEST);
        footer.add(Theme.button("Continue Shopping", this::showStalls), BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        page(panel);
    }

    private void editProduct(MenuItem item, int stall) {
        JTextField name = new JTextField(item == null ? "" : item.getName());
        JTextField category = new JTextField(item == null ? "" : item.getCategory());
        JTextField price = new JTextField(item == null ? "0" : "" + item.getPrice());
        JTextField stock = new JTextField(item == null ? "0" : "" + item.getStock());
        JComboBox<String> types = new JComboBox<>(new String[] {"Food", "Drink"});
        if (item != null) types.setSelectedItem(item.type());
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Name")); form.add(name);
        form.add(new JLabel("Category")); form.add(category);
        form.add(new JLabel("Type")); form.add(types);
        form.add(new JLabel("Price (-1 unknown)")); form.add(price);
        form.add(new JLabel("Stock")); form.add(stock);
        if (item != null) types.setEnabled(false);
        if (JOptionPane.showConfirmDialog(this, form, "Product details", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            int p = Integer.parseInt(price.getText().trim());
            int n = Integer.parseInt(stock.getText().trim());
            String title = name.getText().trim();
            String cat = category.getText().trim();
            if (title.isEmpty() || cat.isEmpty()) throw new IllegalArgumentException("Name and category required.");
            if (!confirm("Save Product", "Save product: " + title + "\nPrice: PHP " + p + "\nStock: " + n + "?")) return;
            if (item == null) {
                int id = system.items.stream().mapToInt(MenuItem::getId).max().orElse(0) + 1;
                if (stall == 0) throw new IllegalArgumentException("Choose a stall first.");
                MenuItem created = types.getSelectedItem().equals("Food") ? new Food(id, stall, title, cat, p, n) : new Drink(id, stall, title, cat, p, n);
                system.items.add(created);
            } else {
                item.updateInfo(p, n);
                item.updateDescription(title, cat);
            }
            if (save()) showProducts(stall, "", "");
        } catch (IllegalArgumentException e) {
            message(e.getMessage());
        }
    }

    private void showOrders() { page(ordersPanel(false)); }

    private JPanel ordersPanel(boolean overview) {
        JPanel panel = section(currentUser instanceof Buyer ? "My Orders" : "Recent Pre-Orders");
        JTable table = table("Stall Order #", "Receipt", "Items", "Quantity", "Total (PHP)", "Status", "Time", "Instructions", "Cancellation Reason");
        centerProductColumns(table);
        final ArrayList<PurchaseRecord> displayedOrders = new ArrayList<>();
        for (PurchaseRecord order : system.orders) {
            if (currentUser instanceof Buyer && !order.getBuyer().equals(currentUser.getId())) continue;
            if (currentUser instanceof Vendor && order.getStall() != scope()) continue;
            displayedOrders.add(order);
            String numberLabel = currentUser instanceof Buyer
                ? system.stall(order.getStall()).getName() + " #" + system.stallOrderNumber(order)
                : "#" + system.stallOrderNumber(order);
            addRow(table, numberLabel, order.getReceiptId(), order.getName(), order.getQuantity(),
                order.total(), order.getStatus(), order.getTime().toString().replace('T', ' '),
                order.getInstructions(), order.getCancellationReason());
        }
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (!selected) {
                    String status = String.valueOf(value);
                    setForeground(status.equals("PENDING") ? new Color(172, 29, 35)
                        : status.equals("CANCELLED") ? new Color(100, 90, 90)
                        : new Color(22, 77, 168));
                    setBackground(new Color(255, 244, 244));
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        if (currentUser instanceof Buyer) {
            JPanel buyerActions = Theme.panel(new GridLayout(1, 2, 10, 0));
            buyerActions.add(Theme.button("View Combined Receipt", () -> {
                int row = table.getSelectedRow();
                if (row < 0) { message("Select an order first."); return; }
                String receiptId = String.valueOf(table.getValueAt(row, 1));
                if (receiptId.isBlank()) { message("This older order has no combined receipt ID."); return; }
                showReceipt(receiptId);
            }));
            buyerActions.add(Theme.button("Order Details / Cancellation Reason", () -> {
                int row = table.getSelectedRow();
                if (row < 0) { message("Select an order first."); return; }
                PurchaseRecord selected = displayedOrders.get(table.convertRowIndexToModel(row));
                String details = "Order #" + system.stallOrderNumber(selected)
                    + " at " + system.stall(selected.getStall()).getName()
                    + "\nStatus: " + selected.getStatus()
                    + "\nInstructions: " + (selected.getInstructions().isBlank()
                        ? "None" : selected.getInstructions());
                if (selected.getStatus() == OrderStatus.CANCELLED)
                    details += "\nCancellation reason: " + selected.getCancellationReason();
                message(details);
            }));
            panel.add(buyerActions, BorderLayout.SOUTH);
        }
        if (currentUser instanceof Vendor) {
            JPanel orderActions = Theme.panel(new GridLayout(1, 3, 10, 0));
            orderActions.add(Theme.button("View Instructions", () -> {
                int row = table.getSelectedRow();
                if (row < 0) { message("Select an order first."); return; }
                int id = displayedOrders.get(table.convertRowIndexToModel(row)).getId();
                PurchaseRecord selected = null;
                for (PurchaseRecord record : system.orders) {
                    if (record.getId() == id && record.getStall() == scope()) {
                        selected = record;
                        break;
                    }
                }
                if (selected == null) return;
                String notes = selected.getInstructions().isBlank()
                    ? "No special instructions." : selected.getInstructions();
                String reason = selected.getStatus() == OrderStatus.CANCELLED
                    ? "\nCancellation reason: " + selected.getCancellationReason() : "";
                message("Stall Order #" + system.stallOrderNumber(selected)
                    + "\n" + notes + reason);
            }));
            orderActions.add(Theme.button("Complete Selected Order", () -> {
                int row = table.getSelectedRow();
                if (row < 0) { message("Select an order first."); return; }
                int id = displayedOrders.get(table.convertRowIndexToModel(row)).getId();
                PurchaseRecord selected = displayedOrders.get(table.convertRowIndexToModel(row));
                if (!confirm("Complete Stall Order", "Complete stall order #"
                        + system.stallOrderNumber(selected) + "? All pending items for this stall in this checkout will be completed.")) return;
                try {
                    system.completeStallOrder((Vendor) currentUser, id);
                    if (save()) showHome();
                } catch (InvalidOrderException ex) {
                    message("Completion failed: " + ex.getMessage());
                }
            }));
            orderActions.add(Theme.button("Cancel Selected Order", () -> {
                int row = table.getSelectedRow();
                if (row < 0) { message("Select an order first."); return; }
                int id = displayedOrders.get(table.convertRowIndexToModel(row)).getId();
                PurchaseRecord selected = displayedOrders.get(table.convertRowIndexToModel(row));
                String reason = JOptionPane.showInputDialog(this,
                    "Reason for cancelling stall order #" + system.stallOrderNumber(selected) + ":",
                    "Not picking up / Buyer request");
                if (reason == null) return;
                reason = reason.replaceAll("[\r\n\t]+", " ").trim();
                if (reason.isBlank()) { message("Please enter a cancellation reason."); return; }
                if (!confirm("Cancel Pending Order", "Cancel this stall's order #" + system.stallOrderNumber(displayedOrders.get(table.convertRowIndexToModel(row)))
                        + "?\nAll pending items in this stall's checkout group will be cancelled. Stock will be restored.")) return;
                try {
                    system.cancelOrder((Vendor) currentUser, id, reason);
                    if (save()) showHome();
                } catch (InvalidOrderException ex) {
                    message("Cancellation failed: " + ex.getMessage());
                }
            }));
            panel.add(orderActions, BorderLayout.SOUTH);
        }
        return panel;
    }

    private int units(MenuItem item) {
        int total = 0;
        for (PurchaseRecord order : system.orders) {
            if (order.getItem() == item.getId() && order.getStatus() == OrderStatus.COMPLETED) total += order.getQuantity();
        }
        return total;
    }

    private void showSales() {
        ArrayList<MenuItem> ranked = new ArrayList<>();
        for (MenuItem item : system.items) {
            if (scope() == 0 || item.getStall() == scope()) ranked.add(item);
        }
        ranked.sort(Comparator.comparingInt(this::units).reversed().thenComparing(MenuItem::getName));
        JPanel panel = section("Product Sales Ranking | Most to Least Sold");
        JTable table = table("Rank", "Product", "Stall", "Units Sold");
        DefaultTableCellRenderer centeredSales = new DefaultTableCellRenderer();
        centeredSales.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centeredSales);
        for (int i = 0; i < ranked.size(); i++) {
            MenuItem item = ranked.get(i);
            addRow(table, i + 1, item.getName(), system.stall(item.getStall()).getName(), units(item));
        }
        if (!ranked.isEmpty() && units(ranked.get(0)) > 0) {
            int max = units(ranked.get(0));
            int min = units(ranked.get(ranked.size() - 1));
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                    setHorizontalAlignment(SwingConstants.CENTER);
                    int sold = (Integer) t.getValueAt(row, 3);
                    if (!selected) {
                        c.setBackground(sold == max ? new Color(205, 241, 216) :
                            sold == min && min != max ? new Color(255, 224, 194) :
                            row % 2 == 0 ? new Color(255, 248, 248) : new Color(250, 237, 237));
                        c.setForeground(Theme.INK);
                    }
                    return c;
                }
            });
        }
        panel.add(new JScrollPane(table));
        page(panel);
    }

    private void sendFeedback(int stallId) {
        if (!(currentUser instanceof Buyer)) return;
        String stallName = system.stall(stallId).getName();
        JTextArea comment = new JTextArea(5, 32);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        JPanel form = Theme.panel(new BorderLayout(0, 8));
        form.add(new JLabel("Feedback for " + stallName + " (up to 500 characters):"), BorderLayout.NORTH);
        form.add(new JScrollPane(comment), BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(this, form, "Send Stall Feedback",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            system.submitFeedback((Buyer) currentUser, stallId, comment.getText());
            if (save()) message("Your feedback has been sent to " + stallName + ".");
        } catch (InvalidOrderException ex) {
            message(ex.getMessage());
        }
    }

    private void showFeedback() {
        if (!(currentUser instanceof Vendor)) return;
        JPanel panel = section("Feedback for " + system.stall(scope()).getName());
        JTable table = table("Date", "Stall", "Buyer ID", "Feedback");
        final ArrayList<StallFeedback> displayedFeedback = new ArrayList<>();
        for (StallFeedback feedback : system.feedbacks) {
            if (currentUser instanceof Vendor && feedback.getStallId() != scope()) continue;
            displayedFeedback.add(feedback);
            addRow(table, feedback.getTime().toString().replace('T', ' '),
                system.stall(feedback.getStallId()).getName(),
                feedback.getBuyerId(), feedback.getMessage());
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(Theme.button("Read Selected Feedback", () -> {
            int row = table.getSelectedRow();
            if (row < 0) { message("Select feedback first."); return; }
            StallFeedback selected = displayedFeedback.get(table.convertRowIndexToModel(row));
            message("Stall: " + system.stall(selected.getStallId()).getName()
                + "\nBuyer: " + selected.getBuyerId() + "\nDate: " + selected.getTime()
                + "\n\n" + selected.getMessage());
        }), BorderLayout.SOUTH);
        page(panel);
    }

    private void sendLandingHelp() {
        JTextField sender = new JTextField(25);
        JComboBox<String> accountType = new JComboBox<>(new String[] {"Buyer", "Vendor", "Other"});
        JTextArea details = new JTextArea(5, 30);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);

        JPanel fields = Theme.panel(new GridLayout(0, 1, 0, 5));
        fields.add(new JLabel("Your name or User ID:"));
        fields.add(sender);
        fields.add(new JLabel("I need help as a:"));
        fields.add(accountType);
        fields.add(new JLabel("Message for Admin (up to 500 characters):"));

        JPanel form = Theme.panel(new BorderLayout(0, 8));
        form.add(fields, BorderLayout.NORTH);
        form.add(new JScrollPane(details), BorderLayout.CENTER);

        if (JOptionPane.showConfirmDialog(this, form, "HELP | Contact Admin",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            system.submitLandingHelp(sender.getText(),
                String.valueOf(accountType.getSelectedItem()), details.getText());
            if (save()) message("Your help request was sent to Admin.");
        } catch (InvalidOrderException ex) {
            message(ex.getMessage());
        }
    }

    private void showHelpRequests() {
        if (!(currentUser instanceof Admin)) return;
        JPanel panel = section("HELP REQUESTS | Buyer and Vendor Support");
        JTable table = table("ID", "Date", "Role", "Sender", "Message", "Status");
        centerProductColumns(table);
        ArrayList<HelpRequest> displayed = new ArrayList<>(system.helpRequests);
        displayed.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        for (HelpRequest h : displayed)
            addRow(table, h.getId(), h.getTime().toString().replace('T', ' '),
                h.getSenderRole(), h.getSenderId(), h.getMessage(),
                h.isResolved() ? "RESOLVED" : "OPEN");
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = Theme.panel(new GridLayout(1, 2, 10, 0));
        actions.add(Theme.button("Read Request", () -> {
            int row = table.getSelectedRow();
            if (row < 0) { message("Select a help request first."); return; }
            HelpRequest selected = displayed.get(table.convertRowIndexToModel(row));
            message("From: " + selected.getSenderRole() + " " + selected.getSenderId()
                + "\nDate: " + selected.getTime() + "\n\n" + selected.getMessage());
        }));
        actions.add(Theme.button("Mark Resolved", () -> {
            int row = table.getSelectedRow();
            if (row < 0) { message("Select a help request first."); return; }
            HelpRequest selected = displayed.get(table.convertRowIndexToModel(row));
            if (selected.isResolved()) { message("Request is already resolved."); return; }
            if (!confirm("Resolve Help Request", "Mark help request #" + selected.getId() + " as resolved?")) return;
            try { system.resolveHelp(selected.getId()); if (save()) showHelpRequests(); }
            catch (InvalidOrderException ex) { message(ex.getMessage()); }
        }));
        panel.add(actions, BorderLayout.SOUTH);
        page(panel);
    }

    private void showRegistrations() {
        if (!(currentUser instanceof Admin)) return;
        JPanel panel = section("ACCOUNT REGISTRATION | Pending Admin Approval");
        JTable table = table("Request ID", "User ID", "Name", "Role", "Vendor Stall", "Requested At");
        centerProductColumns(table);
        ArrayList<RegistrationRequest> pending = new ArrayList<>();
        for (RegistrationRequest request : system.registrations) {
            if (request.getStatus() != RegistrationRequest.Status.PENDING) continue;
            pending.add(request);
            String stall = request.getStallId() > 0
                ? system.stall(request.getStallId()).getName() : "—";
            addRow(table, request.getId(), request.getUserId(), request.getName(),
                request.getRole(), stall, request.getRequestedAt().toString().replace('T', ' '));
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = Theme.panel(new GridLayout(1, 2, 10, 0));
        actions.add(Theme.button("APPROVE SELECTED ACCOUNT", () -> {
            int row = table.getSelectedRow();
            if (row < 0) { message("Select a registration request first."); return; }
            RegistrationRequest selected = pending.get(table.convertRowIndexToModel(row));
            if (!confirm("Approve Registration", "Activate " + selected.getRole()
                    + " account " + selected.getUserId() + "?")) return;
            try {
                system.approveRegistration(selected.getId());
                if (save()) { message("Account approved and activated."); showRegistrations(); }
            } catch (InvalidOrderException ex) { message(ex.getMessage()); }
        }));
        actions.add(Theme.button("REJECT SELECTED ACCOUNT", () -> {
            int row = table.getSelectedRow();
            if (row < 0) { message("Select a registration request first."); return; }
            RegistrationRequest selected = pending.get(table.convertRowIndexToModel(row));
            if (!confirm("Reject Registration", "Reject the registration of " + selected.getUserId() + "?")) return;
            try {
                system.rejectRegistration(selected.getId());
                if (save()) { message("Registration rejected."); showRegistrations(); }
            } catch (InvalidOrderException ex) { message(ex.getMessage()); }
        }));
        panel.add(actions, BorderLayout.SOUTH);
        page(panel);
    }

    private void showSettings() {
        JTable table = table("User ID", "Name", "Role", "Active");
centerProductColumns(table);
        JPanel panel = section("Account Management");
        if (currentUser instanceof Vendor) {
            panel.add(Theme.label(currentUser.getName() + " | " + system.stall(scope()).getName(), 20, Theme.INK));
            panel.add(Theme.button("Toggle stall availability", () -> {
                system.stall(scope()).toggle();
                if (save()) showStalls();
            }), BorderLayout.SOUTH);
        } else {
            for (User user : system.users) addRow(table, user.getId(), user.getName(), user.role(), user.isActive());
            table.setRowHeight(Math.max(table.getRowHeight(), 42));
            table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
                private boolean active;
                private boolean highlighted;
                {
                    setOpaque(false);
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(getFont().deriveFont(Font.BOLD, 12f));
                }
                @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                    super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                    active = Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
                    highlighted = selected;
                    setText(active ? "True" : "False");
                    setForeground(active ? new Color(21, 112, 67) : new Color(168, 43, 55));
                    setFont(getFont().deriveFont(Font.BOLD, 12f));
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setOpaque(false);
                    return this;
                }
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int width = Math.min(72, getWidth() - 16);
                    int height = Math.min(34, getHeight() - 8);
                    int x = (getWidth() - width) / 2;
                    int y = (getHeight() - height) / 2;
                    Color border = active ? new Color(41, 147, 89) : new Color(205, 70, 79);
                    Color fill = active ? new Color(226, 246, 233) : new Color(255, 231, 232);
                    if (highlighted) fill = active ? new Color(203, 237, 215) : new Color(250, 207, 210);
                    g.setColor(fill);
                    g.fillRoundRect(x, y, width - 1, height - 1, height, height);
                    g.setColor(border);
                    g.setStroke(new BasicStroke(1.6f));
                    g.drawRoundRect(x, y, width - 1, height - 1, height, height);
                    g.dispose();
                    super.paintComponent(graphics);
                }
            });
            panel.add(new JScrollPane(table));
            JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT));
            actions.add(Theme.button("Add account", this::addAccount));
            actions.add(Theme.button("Toggle account", () -> {
                int row = table.getSelectedRow();
                if (row < 0) { message("Select an account."); return; }
                User user = system.user(table.getValueAt(row, 0).toString());
                if (user instanceof Admin) { message("Admin accounts cannot be disabled."); return; }
                if (!confirm("Change Account Status", "Change " + user.getName() + " to " + (user.isActive() ? "Inactive" : "Active") + "?")) return;
                user.setActive(!user.isActive());
                if (save()) showSettings();
            }));
            panel.add(actions, BorderLayout.SOUTH);
        }
        page(panel);
    }

    private void addAccount() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[] {"Buyer", "Vendor", "Admin"});
        JComboBox<String> stall = new JComboBox<>();
        for (Stall s : system.stalls) stall.addItem(s.getName());
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("User ID")); form.add(id);
        form.add(new JLabel("Name")); form.add(name);
        form.add(new JLabel("Password")); form.add(password);
        form.add(new JLabel("Role")); form.add(role);
        form.add(new JLabel("Vendor stall")); form.add(stall);
        if (JOptionPane.showConfirmDialog(this, form, "Add account", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String uid = id.getText().trim();
        String display = name.getText().trim();
        String pw = new String(password.getPassword());
        if (uid.isEmpty() || display.isEmpty() || pw.isEmpty() || system.user(uid) != null) {
            message("All fields required; ID must be unique.");
            return;
        }
        User user;
        if (role.getSelectedItem().equals("Admin")) user = new Admin(uid, display, pw);
        else if (role.getSelectedItem().equals("Vendor")) user = new Vendor(uid, display, pw, stall.getSelectedIndex() + 1);
        else user = new Buyer(uid, display, pw);
        if (!confirm("Create Account", "Create " + user.role() + " account " + uid + "?")) return;
        system.users.add(user);
        if (save()) showSettings();
    }

    private boolean save() {
        try {
            FileHandler.saveData(system);
            return true;
        } catch (IOException e) {
            message("Unable to save CSV: " + e.getMessage() + ". Close Excel and restart before continuing.");
            return false;
        }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}
