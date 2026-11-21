import java.util.Scanner;
class InputHelper {
    private final Scanner in=new Scanner(System.in);
    String text(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s=in.nextLine().trim();
            if (!s.isEmpty())return s;
            System.out.println("Input required.");
        }
    }
    int number(String prompt,int min,int max) {
        while (true) {
            try {
                int n=Integer.parseInt(text(prompt));
                if (n>=min&&n<=max)return n;
            }
            catch (NumberFormatException e) {
            }
            System.out.println("Enter a whole number from "+min+" to "+max);
        }
    }
}
