package cn.powernukkitx.cli;

import cn.powernukkitx.cli.util.ConfigUtils;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.util.Timer;

public final class Main {
    static Timer timer = null;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        try {
            new CommandLine(new Preprocessor()).parseArgs(args);
        } catch (Exception ignore) {

        }
        ConfigUtils.init();
        var exitCode = new CommandLine(new App()).execute(args);
        if (timer != null)
            timer.cancel();
        System.exit(exitCode);
    }

    public static Timer getTimer() {
        if (timer == null) {
            timer = new Timer();
        }
        return timer;
    }
}
