package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.locator.ComponentsLocator;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.HttpUtils;
import cn.powernukkitx.cli.util.OSUtils;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "components", aliases = "comp", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Components")
public final class ComponentsCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Components");

    @ArgGroup(multiplicity = "1")
    AllOptions options;

    static final class AllOptions {
        @Option(names = {"install", "-i", "update", "-u"}, help = true, descriptionKey = "update")
        String update;
        @Option(names = {"check", "-c"}, help = true, descriptionKey = "check")
        boolean check;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Integer call() {
        if (options.check) {
            var libs = new ComponentsLocator().locate();
            for (var each : libs) {
                var ansi = ansi();
                if (each.getFile().exists()) {
                    ansi.fgBrightGreen().a("+ ");
                } else {
                    ansi.fgBrightRed().a("- ");
                }
                ansi.a(each.getInfo().getName()).fgDefault().a(" - ").bold().a(each.getInfo().getVersion()).fgDefault().boldOff().a("  ").a(each.getInfo().getDescription());
                System.out.println(ansi);
            }
            return 0;
        } else if (options.update != null && !options.update.isBlank()) {
            var componentList = new ComponentsLocator().locate();
            var componentDir = new File(CLIConstant.userDir, "components");
            if (!componentDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                componentDir.mkdirs();
            }
            for (var each : componentList) {
                var componentInfo = each.getInfo();
                if (componentInfo.getName().equalsIgnoreCase(options.update)) {
                    System.out.println(ansi().fgBrightYellow().a(new Formatter().format(bundle.getString("installing"), componentInfo.getName() + " - " + componentInfo.getVersion())).fgDefault());
                    for (var clearFileName : componentInfo.getClearFiles()) {
                        var file = new File(CLIConstant.userDir, clearFileName);
                        if (file.exists()) {
                            if (file.isDirectory()) {
                                Arrays.stream(Objects.requireNonNull(file.listFiles())).sequential().forEach(File::delete);
                            } else if (file.isFile()) {
                                file.delete();
                            }
                        }
                    }
                    var ok = true;
                    for (var compFile : componentInfo.getComponentFiles()) {
                        ok = ok && HttpUtils.downloadWithBar(compFile.getDownloadPath(), new File(CLIConstant.userDir, compFile.getFileName()), componentInfo.getName() + " - " + componentInfo.getVersion(), Main.getTimer());
                    }
                    if (ok) {
                        System.out.println(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("successfully-update"), componentInfo.getName() + " - " + componentInfo.getVersion())).fgDefault());
                        return 0;
                    } else {
                        System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-update"), componentInfo.getName() + " - " + componentInfo.getVersion())).fgDefault());
                        return 1;
                    }
                }
            }
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("component-not-found"), OSUtils.getProgramName())).fgDefault());
            return 1;
        } else {
            CommandLine.usage(this, System.out);
        }
        return 0;
    }
}
