package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.builder.JVMStartCommandBuilder;
import cn.powernukkitx.cli.data.locator.GraalJITLocator;
import cn.powernukkitx.cli.data.locator.GraalModuleLocator;
import cn.powernukkitx.cli.data.locator.JarLocator;
import cn.powernukkitx.cli.data.locator.JavaLocator;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.InputUtils;
import cn.powernukkitx.cli.util.OSUtils;
import picocli.CommandLine.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import static cn.powernukkitx.cli.util.NullUtils.Ok;
import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "start", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Start")
public final class StartCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Start");

    @Option(names = {"-g", "--generate-only"}, descriptionKey = "generate-only", help = true)
    public boolean generateOnly;

    @Option(names = {"-r", "--restart"}, descriptionKey = "restart", help = true, negatable = true)
    public boolean restart;

    @Option(names = "--stdin", descriptionKey = "stdin", help = true)
    public String stdin;

    @Parameters(index = "0..*", hidden = true)
    public String[] args;

    private String[] startCommand = null;

    @Override
    public Integer call() {
        var cmdBuilder = new JVMStartCommandBuilder();
        if (args != null && args.length > 0) {
            cmdBuilder.setOtherArgs(args);
        }
        var javaList = new JavaLocator("17", true).locate();
        if (javaList.size() == 0) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("no-java17"), OSUtils.getProgramName())).fgDefault());
            return 1;
        }
        var java = javaList.get(0);
        cmdBuilder.setJvmExecutable(java.getFile());
        System.out.println(ansi().fgBrightYellow().a(new Formatter().format(bundle.getString("using-jvm"), java.getInfo().getVendor())).fgDefault());
        var pnxList = new JarLocator(CLIConstant.userDir, "cn.nukkit.api.PowerNukkitXOnly").locate();
        if (pnxList.size() == 0) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("no-pnx"), OSUtils.getProgramName())).fgDefault());
            return 1;
        }
        var pnx = pnxList.get(0);
        cmdBuilder.addClassPath(pnx.getFile().getAbsolutePath());
        System.out.println(ansi().fgBrightYellow().a(new Formatter().format(bundle.getString("using-pnx"), Ok(pnx.getInfo().getGitInfo().orElse(null), info -> info.getMainVersion() + " - " + info.getCommitID(), "unknown"))).fgDefault());
        cmdBuilder.setStartTarget("cn.nukkit.Nukkit");
        cmdBuilder.addClassPath(new File(CLIConstant.userDir, "libs").getAbsolutePath() + File.separator + "*");
        cmdBuilder.addAddOpen("java.base/java.lang");
        cmdBuilder.addAddOpen("java.base/java.io");
        cmdBuilder.addProperty("file.encoding", "UTF-8");
        cmdBuilder.addProperty("jansi.passthrough", "true");
        cmdBuilder.addProperty("terminal.ansi", "true");
        cmdBuilder.addXxOption("UnlockExperimentalVMOptions", true);
        cmdBuilder.addXxOption("UseG1GC", true);
        cmdBuilder.addXxOption("UseStringDeduplication", true);
        cmdBuilder.addXxOption("EnableJVMCI", true);
        if (isLowVersionGraalVM(java.getInfo()) != GraalStatus.NotFound) {
            cmdBuilder.addXxOption("UseJVMCICompiler", true);
        } else {
            var graalJIT = new GraalJITLocator().locate();
            if (graalJIT.size() > 1) {
                cmdBuilder.addXxOption("UseJVMCICompiler", true);
                cmdBuilder.addUpgradeModuleArgs(graalJIT.get(0).getFile().getAbsolutePath());
                cmdBuilder.addUpgradeModuleArgs(graalJIT.get(1).getFile().getAbsolutePath());
            }
        }
        var graalModules = new GraalModuleLocator().locate();
        for (var module : graalModules) {
            cmdBuilder.addModulePath(module.getFile().getAbsolutePath());
        }
        if (generateOnly) {
            System.out.println(cmdBuilder.build());
            return 0;
        }
        startCommand = cmdBuilder.build().split(" ");
        if (restart) {
            var result = start();
            while (true) {
                if (!InputUtils.pressEnterToStopWithTimeLimit(10000)) {
                    result = start();
                } else {
                    return result;
                }
            }
        } else {
            return start();
        }
    }

    enum GraalStatus {
        NotFound,
        Standard,
        LowVersion
    }

    private GraalStatus isLowVersionGraalVM(JavaLocator.JavaInfo javaInfo) {
        var vendor = javaInfo.getVendor().toLowerCase();
        if (!vendor.contains("graal")) {
            return GraalStatus.NotFound;
        }
        var index = vendor.indexOf("2", vendor.indexOf("graalvm"));
        if (index == -1) {
            return GraalStatus.NotFound;
        }
        var version = vendor.substring(index, index + 4);
        return Integer.parseInt(version.replace(".", "")) < 222 ? GraalStatus.LowVersion : GraalStatus.Standard;
    }

    private int start() {
        System.gc();
        try {
            var process = new ProcessBuilder().command(startCommand).inheritIO().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));
            if (stdin != null && !"".equals(stdin.trim())) {
                var stdinFile = new File(CLIConstant.userDir, stdin);
                if (stdinFile.exists() && stdinFile.isFile() && stdinFile.canRead() && stdinFile.canWrite()) {
                    Main.getTimer().scheduleAtFixedRate(new TimerTask() {
                        long lastUpdateTime = -1;
                        @Override
                        public void run() {
                            try {
                                if (!process.isAlive()) {
                                    this.cancel();
                                }
                                if (stdinFile.lastModified() > lastUpdateTime) {
                                    process.getOutputStream().write(Files.readAllBytes(stdinFile.toPath()));
                                    Files.write(stdinFile.toPath(), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                                    lastUpdateTime = stdinFile.lastModified();
                                }
                            } catch (Exception ignore) {

                            }
                        }
                    }, 1000, 1000);
                }
            }
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
