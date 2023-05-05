package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.Logger;
import picocli.CommandLine.Command;

import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "about", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.About")
public final class AboutCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.About");

    @Override
    public Integer call() {
        Logger.info(ansi().fgBrightDefault().bold().a("PowerNukkitX").fgDefault().boldOff());
        Logger.info(ansi().fgBrightDefault().bold().a("------------------------------").fgDefault().boldOff());
        Logger.info(ansi().a(bundle.getString("cli-version")).fgBrightYellow().a(CLIConstant.version).fgDefault());
        Logger.info(ansi().a(bundle.getString("author")).fgBrightCyan().a(String.join(", ", CLIConstant.authors)).fgDefault());
        return 0;
    }
}
