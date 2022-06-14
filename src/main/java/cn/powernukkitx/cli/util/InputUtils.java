package cn.powernukkitx.cli.util;

import java.util.Scanner;

public final class InputUtils {
    private InputUtils() {

    }

    public static int readIndex(String prompt) {
        System.out.print(prompt + " ");
        var scanner = new Scanner(System.in);
        return scanner.nextInt();
    }
}
