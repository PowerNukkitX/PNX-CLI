package cn.powernukkitx.cli.util;

import java.util.ResourceBundle;
import java.util.Scanner;

public final class InputUtils {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.util.Input");

    private InputUtils() {

    }

    public static int readIndex(String prompt) {
        System.out.print(prompt + " ");
        var scanner = new Scanner(System.in);
        return scanner.nextInt();
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
}
