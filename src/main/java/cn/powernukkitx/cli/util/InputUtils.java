package cn.powernukkitx.cli.util;

import java.util.ResourceBundle;
import java.util.Scanner;

import static org.fusesource.jansi.Ansi.ansi;

public final class InputUtils {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.util.Input");

    private InputUtils() {

    }

    public static int readIndex(String prompt) {
        System.out.print(prompt + " ");
        var scanner = new Scanner(System.in);
        return scanner.nextInt();
    }
    public static char readChar() {
        var scanner = new Scanner(System.in);
        return scanner.next().charAt(0);
    }
    public static String readLine() {
        var scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static void pressEnterToContinue() {
        System.out.println(bundle.getString("press-enter-to-continue"));
        try {
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean pressEnterToStopWithTimeLimit(long timeLimit) {
        System.out.println(ansi().fgBrightDefault().bold().a(bundle.getString("press-enter-to-stop-with-time-limit")).fgDefault().boldOff());
        try {
            var startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeLimit) {
                if (System.in.available() > 0) {
                    return true;
                } else {
                    //noinspection BusyWait
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
