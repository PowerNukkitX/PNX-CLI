package cn.powernukkitx.cli.util;

import org.fusesource.jansi.Ansi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.fusesource.jansi.Ansi.ansi;

public final class Logger {
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Logger() {
        throw new UnsupportedOperationException();
    }

    public static void info(String msg) {
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBlue().a("INFO ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void info(Ansi msg) {
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBlue().a("INFO ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void warn(String msg) {
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgRed().a("WARN ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void warn(Ansi msg) {
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgRed().a("WARN ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void error(String msg) {
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBrightRed().a("ERROR").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void error(Ansi msg) {
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBrightRed().a("ERROR").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void raw(String msg) {
        System.out.println(msg);
    }
}
