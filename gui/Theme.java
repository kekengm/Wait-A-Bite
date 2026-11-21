import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Theme {

    public static final Color RED = new Color(0xFF3131);
    public static final Color DARK = new Color(0x301010);
    public static final Color PINK = new Color(250, 225, 225);
    public static final Color INK = new Color(92, 25, 29);

    public static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    public static JButton button(String text, Runnable action) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setPaint(new GradientPaint(0, 0, RED, getWidth(), 0, DARK));
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                graphics.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    public static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(new Color(255, 241, 241, 205));
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                graphics.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    private static final BufferedImage LOGO = loadLogo();

    private static BufferedImage loadLogo() {
        try {
            File file = new File("assets/wait-a-bite.png");
            return file.isFile() ? ImageIO.read(file) : null;
        } catch (java.io.IOException e) { return null; }
    }

    public static JLabel brandLogo() { return brandLogo(220); }

    public static JLabel brandLogo(int maxWidth) {
        if (LOGO == null) return label("Wait A Bite", 22, INK);
        JLabel label = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
                int[] fitted = fittedSize(LOGO.getWidth(), LOGO.getHeight(),
                    Math.max(1, getWidth() - 4), Math.max(1, getHeight() - 4));
                g2.drawImage(LOGO, (getWidth() - fitted[0]) / 2,
                    (getHeight() - fitted[1]) / 2, fitted[0], fitted[1], null);
                g2.dispose();
            }
        };
        int idealHeight = Math.max(1, (int) Math.ceil(
            maxWidth * (double) LOGO.getHeight() / LOGO.getWidth()));
        label.setPreferredSize(new Dimension(maxWidth, idealHeight + 4));
        label.setMinimumSize(new Dimension(1, 1));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private static int[] fittedSize(int sourceW, int sourceH, int limitW, int limitH) {
        double scale = Math.min(limitW / (double) sourceW, limitH / (double) sourceH);
        scale = Math.min(1.0, Math.max(0.0, scale)); // Never upscale the original pixels.
        return new int[] { Math.max(1, (int) Math.round(sourceW * scale)),
            Math.max(1, (int) Math.round(sourceH * scale)) };
    }

    public static JPanel background(LayoutManager layout) {
        return new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setPaint(new RadialGradientPaint(getWidth() / 2f,
                    getHeight() / 2f, Math.max(1f, (float) Math.hypot(getWidth() / 2.0, getHeight() / 2.0)),
                    new float[] {0f, 1f}, new Color[] {RED, DARK}));
                graphics.fillRect(0, 0, getWidth(), getHeight());

                if (LOGO != null) {
                    boolean dashboard = Boolean.TRUE.equals(getClientProperty("dashboard"));
                    int contentLeft = dashboard ? 255 : 0;
                    int availableWidth = getWidth() - contentLeft;
                    int[] fitted = fittedSize(LOGO.getWidth(), LOGO.getHeight(),
                        Math.max(1, (int) (availableWidth * 0.78)),
                        Math.max(1, (int) (getHeight() * 0.65)));
                    int width = fitted[0];
                    int height = fitted[1];
                    graphics.setComposite(AlphaComposite.SrcOver.derive(0.25f));
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(LOGO, contentLeft + (availableWidth - width) / 2,
                        (getHeight() - height) / 2, width, height, null);
                }
                graphics.dispose();
            }
        };
    }
}
