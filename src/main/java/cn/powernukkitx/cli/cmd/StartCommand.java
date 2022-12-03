package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.builder.JVMStartCommandBuilder;
import cn.powernukkitx.cli.data.locator.GraalJITLocator;
import cn.powernukkitx.cli.data.locator.GraalModuleLocator;
import cn.powernukkitx.cli.data.locator.JarLocator;
import cn.powernukkitx.cli.data.locator.JavaLocator;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.ConfigUtils;
import cn.powernukkitx.cli.util.InputUtils;
import cn.powernukkitx.cli.util.LibsUtils;
import cn.powernukkitx.cli.util.OSUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;
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
            try {
                System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("no-pnx"), OSUtils.getProgramName())).fgDefault());
                var download = new ServerCommand();
                download.update = true;
                download.latest = false;
                download.call();
            } catch (ParseException e) {
                e.printStackTrace();
                return 1;
            }
            pnxList = new JarLocator(CLIConstant.userDir, "cn.nukkit.api.PowerNukkitXOnly").locate();
        }
        var libDir = new File(CLIConstant.userDir, "libs");
        if (!libDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            libDir.mkdirs();
        }
        var oldLibFiles = new LinkedList<>(Arrays.asList(Objects.requireNonNull(libDir.listFiles((dir, name) -> name.endsWith(".jar")))));
        if (oldLibFiles.size() < 32) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("no-libs"), OSUtils.getProgramName())).fgDefault());
            System.out.println(ansi().fgBrightRed().a(new Formatter().format("1: true")).fgDefault());
            System.out.println(ansi().fgBrightRed().a(new Formatter().format("2: false")).fgDefault());
            var input = InputUtils.readLine();
            if (input.charAt(0) == '1' || input.charAt(0) == 'T' || input.charAt(0) == 't' || input.equals("true") || input.equals("TRUE")) {
                LibsUtils.checkAndUpdate();
            }
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
        cmdBuilder.addXOption("mx", ConfigUtils.maxVMMemory());
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
        for (var each : ConfigUtils.addOpens()) {
            cmdBuilder.addAddOpen(each);
        }
        for (var each : ConfigUtils.xOptions()) {
            cmdBuilder.addXOption(each);
        }
        for (var each : ConfigUtils.xxOptions()) {
            cmdBuilder.addXxOption(each);
        }
        if (generateOnly) {
            System.out.println(cmdBuilder.build());
            return 0;
        }
        cmdBuilder.addProperty("pnx.cli.path", OSUtils.getProgramPath());
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
            var useStdinFile = stdin != null && !"".equals(stdin.trim());
            var builder = new ProcessBuilder().command(startCommand);
            if (useStdinFile) {
                builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT);
            } else {
                builder.inheritIO();
            }
            var process = builder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));
            if (useStdinFile) {
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
                                    var tmp = Files.readAllBytes(stdinFile.toPath());
                                    process.getOutputStream().write(tmp);
                                    process.getOutputStream().flush();
                                    try (var fileWriter = new FileWriter(stdinFile)) {
                                        fileWriter.write("");// 清空
                                        fileWriter.flush();
                                    }
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
