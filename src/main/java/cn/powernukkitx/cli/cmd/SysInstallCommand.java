package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.util.EnumOS;
import cn.powernukkitx.cli.util.Logger;
import cn.powernukkitx.cli.util.OSUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            Logger.error(ansi().fgBrightRed().a(bundle.getString("executable-only")).fgDefault().toString());
            return 1;
        }
        if (os == EnumOS.UNKNOWN || os == EnumOS.MACOS) {
            Logger.error(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("unsupportedOS"), System.getProperty("os.name"))).fgDefault().toString());
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
                            Logger.info(ansi().fgBrightGreen().a(bundle.getString("success-uninstall")).fgDefault().toString());
                            Logger.info(ansi().fgBrightGreen().a(bundle.getString("windows-cmd")).fgDefault().toString());
                            return 0;
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    Logger.error(ansi().fgBrightRed().a(bundle.getString("fail-uninstall")).fgDefault().toString());
                    return 1;
                } else {
                    Logger.info(ansi().fgBrightGreen().a(bundle.getString("have-not")).fgDefault().toString());
                    return 0;
                }
            }
        } else {
            if (os == EnumOS.WINDOWS) {
                var sysPath = System.getenv("PATH");
                var programDir = OSUtils.getProgramDir();
                if (sysPath.contains(programDir)) {
                    Logger.info(ansi().fgBrightGreen().a(bundle.getString("already")).fgDefault().toString());
                    return 0;
                } else {
                    try {
                        boolean ok = OSUtils.addWindowsPath(programDir);
                        if (ok) {
                            Logger.info(ansi().fgBrightGreen().a(bundle.getString("success")).fgDefault().toString());
                            Logger.info(ansi().fgBrightGreen().a(bundle.getString("windows-cmd")).fgDefault().toString());
                            return 0;
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    Logger.error(ansi().fgBrightRed().a(bundle.getString("fail")).fgDefault().toString());
                    return 1;
                }
            } else if (os == EnumOS.LINUX) {
                var sysPath = System.getenv("PATH");
                var programDir = OSUtils.getProgramDir();
                if (sysPath.contains(programDir)) {
                    Logger.info(ansi().fgBrightGreen().a(bundle.getString("already")).fgDefault().toString());
                    return 0;
                } else {
                    try {
                        var homeDir = System.getProperty("user.home");
                        var profilePath = homeDir + "/.profile";
                        if (!Files.exists(Path.of(profilePath))) {
                            profilePath = homeDir + "/.bash_profile";
                        }
                        if (!Files.exists(Path.of(profilePath))) {
                            Logger.error(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("unsupportedOS"), System.getProperty("os.name"))).fgDefault().toString());
                            return 1;
                        }
                        var profile = Files.readAllLines(Path.of(profilePath));
                        var ok = false;
                        for (int i = 0, len = profile.size(); i < len; i++) {
                            var line = profile.get(i);
                            if (line.startsWith("export") && line.contains("$PATH")) {
                                profile.set(i, profile.get(i) + ":" + programDir);
                                ok = true;
                                break;
                            }
                        }
                        if (!ok) {
                            profile.add("export PATH=" + programDir + ":$PATH");
                        }
                        Files.write(Path.of(profilePath), profile);
                        Logger.info(ansi().fgBrightGreen().a(bundle.getString("success")).fgDefault().toString());
                        Logger.info(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("linux-cmd"), profilePath)).fgDefault().toString());
                        return 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Logger.error(ansi().fgBrightRed().a(bundle.getString("fail")).fgDefault().toString());
                    return 1;
                }
            }
        }

        return 0;
    }
}
