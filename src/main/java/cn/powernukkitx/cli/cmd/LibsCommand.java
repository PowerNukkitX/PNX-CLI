package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.data.locator.LibsLocator;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.LibsUtils;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "libs", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Libs")
public final class LibsCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Libs");

    @ArgGroup(multiplicity = "1")
    AllOptions options;

    static class AllOptions {
        @Option(names = {"update", "-u", "fix", "-f"}, help = true, descriptionKey = "update")
        boolean update;
        @Option(names = "check", help = true, descriptionKey = "check")
        boolean check;
    }

    @Override
    public Integer call() {
        if (options.update) {
            var libDir = new File(CLIConstant.userDir, "libs");
            if (!libDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                libDir.mkdirs();
            }
            var allSuccess = LibsUtils.checkAndUpdate();
            if (allSuccess) {
                System.out.println(ansi().fgBrightGreen().a(bundle.getString("successfully-update")).fgDefault());
                return 0;
            } else {
                return 1;
            }
        } else if (options.check) {
            var libs = new LibsLocator().locate();
            for (var each : libs) {
                var ansi = ansi();
                if (each.getInfo().isNeedsUpdate()) {
                    if (each.getFile().exists()) {
                        ansi.fgBrightYellow();
                    } else {
                        ansi.fgBrightRed();
                    }
                    ansi.a("- ");
                } else {
                    ansi.fgBrightGreen().a("+ ");
                }
                ansi.a(each.getInfo().getName()).fgDefault();
                System.out.println(ansi);
            }
            return 0;
        } else {
            CommandLine.usage(this, System.out);
        }
        return 0;
    }
}
