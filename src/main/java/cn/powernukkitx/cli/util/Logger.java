package cn.powernukkitx.cli.util;

import org.fusesource.jansi.Ansi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fusesource.jansi.Ansi.ansi;

public final class Logger {
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final AtomicInteger ProgressPrefixLength = new AtomicInteger(0);

    private Logger() {
        throw new UnsupportedOperationException();
    }

    public static void clearProgress() {
        var len = ProgressPrefixLength.get();
        if (len != 0) {
            System.out.print(ansi().eraseLine().cursorLeft(len));
            len = ProgressPrefixLength.addAndGet(-len);
            if (len != 0) {
                clearProgress();
            }
        }
    }

    public static void info(String msg) {
        clearProgress();
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBlue().a("INFO ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void info(Ansi msg) {
        clearProgress();
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBlue().a("INFO ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void warn(String msg) {
        clearProgress();
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgRed().a("WARN ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void warn(Ansi msg) {
        clearProgress();
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgRed().a("WARN ").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void error(String msg) {
        clearProgress();
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBrightRed().a("ERROR").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void error(Ansi msg) {
        clearProgress();
        System.out.println(ansi().fgCyan().a(LocalDateTime.now().format(formatter))
                .fgDefault().a(" [").fgBrightRed().a("ERROR").fgDefault().a("] ").fgDefault().a(msg).reset());
    }

    public static void raw(String msg) {
        clearProgress();
        System.out.print(msg);
    }
}
