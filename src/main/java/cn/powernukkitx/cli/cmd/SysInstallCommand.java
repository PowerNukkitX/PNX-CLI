package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.util.EnumOS;
import cn.powernukkitx.cli.util.OSUtils;
import org.fusesource.jansi.Ansi;
import picocli.CommandLine.*;

import java.io.IOException;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "sys-install", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.SysInstall")
public final class SysInstallCommand implements Callable<Integer> {
    @Option(names = {"-u", "--uninstall"}, help = true, descriptionKey = "uninstall")
    boolean uninstall = false;

    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.SysInstall");

    @Override
    public Integer call() {
        var os = OSUtils.getOS();
        if (!"Substrate VM".equals(System.getProperty("java.vm.name"))) {
            System.out.println(ansi().fgBrightRed().a(bundle.getString("executable-only")).fgDefault().toString());
            return 1;
        }
        if (os == EnumOS.UNKNOWN || os == EnumOS.MACOS || os == EnumOS.LINUX) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("unsupportedOS"), System.getProperty("os.name"))).fgDefault().toString());
            return 1;
        }
        if (uninstall) {
            if (os == EnumOS.WINDOWS) {
                var sysPath = System.getenv("PATH");
                var programDir = OSUtils.getProgramDir();
                if (sysPath.contains(programDir)) {
                    try {
                        boolean ok = OSUtils.removeWindowsPath(programDir);
                        if (ok) {
                            System.out.println(ansi().fgBrightGreen().a(bundle.getString("success-uninstall")).fgDefault().toString());
                            System.out.println(ansi().fgBrightGreen().a(bundle.getString("windows-cmd")).fgDefault().toString());
                            return 0;
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(ansi().fgBrightRed().a(bundle.getString("fail-uninstall")).fgDefault().toString());
                    return 1;
                } else {
                    System.out.println(ansi().fgBrightGreen().a(bundle.getString("have-not")).fgDefault().toString());
                    return 0;
                }
            }
        } else {
            if (os == EnumOS.WINDOWS) {
                var sysPath = System.getenv("PATH");
                var programDir = OSUtils.getProgramDir();
                if (sysPath.contains(programDir)) {
                    System.out.println(ansi().fgBrightGreen().a(bundle.getString("already")).fgDefault().toString());
                    return 0;
                } else {
                    try {
                        boolean ok = OSUtils.addWindowsPath(programDir);
                        if (ok) {
                            System.out.println(ansi().fgBrightGreen().a(bundle.getString("success")).fgDefault().toString());
                            System.out.println(ansi().fgBrightGreen().a(bundle.getString("windows-cmd")).fgDefault().toString());
                            return 0;
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(ansi().fgBrightRed().a(bundle.getString("fail")).fgDefault().toString());
                    return 1;
                }
            }
        }

        return 0;
    }
}
