package cn.powernukkitx.cli;

import cn.powernukkitx.cli.cmd.*;
import cn.powernukkitx.cli.share.CLIConstant;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.ResourceBundle;
import java.util.concurrent.Callable;

@Command(name = "pnx", aliases = {"pnx", "PNX", "cli"}, version = CLIConstant.version, mixinStandardHelpOptions = true,
        resourceBundle = "cn.powernukkitx.cli.App", subcommands = {
        AboutCommand.class,
        SysInstallCommand.class,
        SponsorCommand.class,
        JVMCommand.class,
        ServerCommand.class,
        LibsCommand.class,
        StartCommand.class
})
public final class App implements Callable<Integer> {

    @Option(names = {"-l", "--lang", "--language"}, paramLabel = "<lang>", descriptionKey = "lang")
    private String ignoredLocale;

    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.App");

    @Override
    public Integer call() {
        if (ignoredLocale == null)
            CommandLine.usage(this, System.out);
        return 0;
    }
}
