import javax.swing.*;

public class Main {

    public static void main(String[] args) {

            try {

                WaitABiteSystem system = FileHandler.loadData();

                WaitABiteFrame frame = new WaitABiteFrame(system);

                frame.setVisible(true);

            }

    }
}