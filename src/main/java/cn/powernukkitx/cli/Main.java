package cn.powernukkitx.cli;

import cn.powernukkitx.cli.util.ConfigUtils;
import cn.powernukkitx.cli.util.EnumOS;
import cn.powernukkitx.cli.util.OSUtils;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.util.Locale;
import java.util.Timer;

public final class Main {
    static Timer timer = null;
    public static volatile boolean pnxRunning = false;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        ConfigUtils.init();
        // 先设置语言
        if (ConfigUtils.forceLang() != null) {
            Locale.setDefault(Locale.forLanguageTag(ConfigUtils.forceLang().toLowerCase()));
        } else {
            if (OSUtils.getOS() == EnumOS.WINDOWS) {
                var locale = OSUtils.getWindowsLocale();
                if (locale != null) {
                    Locale.setDefault(locale);
                    ConfigUtils.set("language", locale.toLanguageTag());
                }
            }
        }
        var realArgs = args;
        if (ConfigUtils.forceArguments() != null) {
            realArgs = ConfigUtils.forceArguments();
        }
        try {
            new CommandLine(new Preprocessor()).parseArgs(realArgs);
        } catch (Exception ignore) {

        }
        // 解析命令行
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
