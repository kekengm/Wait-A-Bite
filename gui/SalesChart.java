import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public class SalesChart extends JPanel {

    private final long[] totals = new long[7];

    public SalesChart(WaitABiteSystem system) {
        setBackground(Color.WHITE);
        LocalDate start = LocalDate.now().minusDays(6);
        for (PurchaseRecord order : system.orders) {
            if (order.getStatus() != OrderStatus.COMPLETED) continue;
            for (int i = 0; i < totals.length; i++) {
                if (order.getTime().toLocalDate().equals(start.plusDays(i))) {
                    totals[i] += order.total();
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 65;
        int bottom = getHeight() - 45;
        int height = Math.max(1, bottom - 25);
        int width = Math.max(1, getWidth() - left - 35);
        long max = 1;
        for (long total : totals) max = Math.max(max, total);
        for (int i = 0; i <= 4; i++) {
            int y = bottom - height * i / 4;
            graphics.setColor(new Color(225, 210, 210));
            graphics.drawLine(left, y, left + width, y);
            graphics.setColor(Theme.INK);
            graphics.drawString("" + (max * i / 4), 8, y + 4);
        }
        graphics.setStroke(new BasicStroke(3));
        for (int i = 0; i < 7; i++) {
            int x = left + width * i / 6;
            int y = bottom - (int) (height * (double) totals[i] / max);
            graphics.setColor(Theme.RED);
            if (i > 0) {
                int previousY = bottom - (int) (height * (double) totals[i - 1] / max);
                graphics.drawLine(left + width * (i - 1) / 6, previousY, x, y);
            }
            graphics.fillOval(x - 4, y - 4, 8, 8);
            graphics.setColor(Theme.INK);
            graphics.drawString(LocalDate.now().minusDays(6 - i).toString().substring(5), x - 17, bottom + 22);
        }
        graphics.dispose();
    }
}
