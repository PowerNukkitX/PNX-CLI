package cn.powernukkitx.cli.util;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.locator.LibsLocator;
import cn.powernukkitx.cli.share.CLIConstant;

import java.io.File;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;

public class LibsUtils {
    public static boolean checkAndUpdate() {
        var libs = new LibsLocator().locate();
        var libDir = new File(CLIConstant.userDir, "libs");
        var oldLibFiles = new LinkedList<>(Arrays.asList(Objects.requireNonNull(libDir.listFiles((dir, name) -> name.endsWith(".jar")))));
        var allSuccess = true;
        for (var each : libs) {
            oldLibFiles.removeIf(file -> file.getName().equals(each.getFile().getName()));
            if (each.getInfo().isNeedsUpdate()) {
                var result = HttpUtils.downloadWithBar("https://assets.powernukkitx.cn/libs/" + each.getInfo().getName(), each.getFile(), each.getInfo().getName(), Main.getTimer());
                if (!result) {
                    allSuccess = false;
                    System.out.println(ansi().fgBrightRed().a(new Formatter().format(ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Libs").getString("fail-to-update"), each.getInfo().getName())).fgDefault());
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        oldLibFiles.forEach(File::delete);
        return allSuccess;
    }
}
