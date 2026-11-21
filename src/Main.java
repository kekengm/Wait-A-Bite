import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                WaitABiteSystem system = FileHandler.loadData();

                WaitABiteFrame frame = new WaitABiteFrame(system);

                frame.setVisible(true);

            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                    null,
                    "Unable to start Wait A Bite:\n" + e.getMessage(),
                    "Application Error",
                    JOptionPane.ERROR_MESSAGE
                );

            }

        });

    }

} 