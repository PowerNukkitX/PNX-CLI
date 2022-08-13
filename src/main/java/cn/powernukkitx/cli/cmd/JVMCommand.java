package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.locator.JavaLocator;
import cn.powernukkitx.cli.data.locator.Location;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lingala.zip4j.ZipFile;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Formatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static cn.powernukkitx.cli.util.NullUtils.Ok;
import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "jvm", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.JVM")
public final class JVMCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.JVM");

    @ArgGroup(multiplicity = "1")
    AllOptions options;

    static class AllOptions {
        @Option(names = "check", required = true, descriptionKey = "check", help = true)
        boolean check;
        @Option(names = "remote", required = true, descriptionKey = "remote", help = true)
        boolean remote;
        @Option(names = {"install", "-i"}, required = true, descriptionKey = "install-jvm", help = true)
        String install = null;
        @Option(names = "uninstall", required = true, descriptionKey = "uninstall-jvm", help = true)
        boolean uninstall;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Integer call() {
        if (options.check) {
            printJVMs();
        } else if (options.remote) {
            var vms = getVMList();
            var ansi = ansi();
            ansi.fgBrightGreen().a(bundle.getString("available-remote")).fgDefault().a("\n");
            for (int i = 0, len = vms.size(); i < len; i++) {
                var entry = vms.get(i).getAsJsonObject();
                ansi.a(i + 1).a(". ").fgBrightDefault().a(Ok(entry.get("name"), JsonElement::getAsString, "Unknown VM")).fgDefault().a(" ");
                final var os = OSUtils.getOS();
                final var arch = System.getProperty("os.arch").toLowerCase();
                entry = Ok(entry.get("download"), JsonElement::getAsJsonObject, defaultDownloadObj());
                var size = "";
                if (os == EnumOS.WINDOWS) {
                    var obj = Ok(entry.get("windows-x86"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    size = obj.get("size").getAsString();
                } else if (os == EnumOS.LINUX) {
                    JsonObject obj;
                    if (arch.contains("aarch") || arch.contains("arm")) {
                        obj = Ok(entry.get("linux-aarch"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    } else {
                        obj = Ok(entry.get("linux-x86"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    }
                    size = obj.get("size").getAsString();
                } else if (os == EnumOS.MACOS) {
                    JsonObject obj;
                    if (arch.contains("aarch") || arch.contains("arm")) {
                        obj = Ok(entry.get("macos-aarch"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    } else {
                        obj = Ok(entry.get("macos-x86"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    }
                    size = obj.get("size").getAsString();
                }
                if (!"".equals(size)) {
                    ansi.a("(").a(size).a(")\n");
                }
            }
            System.out.println(ansi);
            return 0;
        } else if (options.install != null && !options.install.isBlank()) {
            var vms = getVMList();
            JsonObject vmEntry;
            if ((vmEntry = CollectionUtils.hasAndGet(vms, JsonObject.class,
                    e -> Ok(e.get("name"), JsonElement::getAsString, "").equalsIgnoreCase(options.install))) != null) {
                final var os = OSUtils.getOS();
                final var arch = System.getProperty("os.arch").toLowerCase();
                var url = "";
                var size = "unknown";
                var downloadEntry = vmEntry.get("download").getAsJsonObject();
                if (os == EnumOS.WINDOWS) {
                    var obj = Ok(downloadEntry.get("windows-x86"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    url = obj.get("url").getAsString();
                    size = obj.get("size").getAsString();
                } else if (os == EnumOS.LINUX) {
                    JsonObject obj;
                    if (arch.contains("aarch") || arch.contains("arm")) {
                        obj = Ok(downloadEntry.get("linux-aarch"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    } else {
                        obj = Ok(downloadEntry.get("linux-x86"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    }
                    url = obj.get("url").getAsString();
                    size = obj.get("size").getAsString();
                } else if (os == EnumOS.MACOS) {
                    JsonObject obj;
                    if (arch.contains("aarch") || arch.contains("arm")) {
                        obj = Ok(downloadEntry.get("macos-aarch"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    } else {
                        obj = Ok(downloadEntry.get("macos-x86"), JsonElement::getAsJsonObject, defaultDownloadObj());
                    }
                    url = obj.get("url").getAsString();
                    size = obj.get("size").getAsString();
                }
                if ("".equals(url) || "unknown".equals(size)) {
                    System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("no-this-arch"),
                            options.install, os.name().toLowerCase(), arch)).fgDefault().toString());
                    return 1;
                } else {
                    var localJavaDir = new File(CLIConstant.programDir, "java");
                    if (!localJavaDir.exists()) {
                        localJavaDir.mkdirs();
                    }
                    var suffix = StringUtils.uriSuffix(url);
                    var tmpFile = new File(CLIConstant.programDir, "tmp-" + options.install + "." + suffix);
                    var result = HttpUtils.downloadWithBar(url, tmpFile, options.install, Main.getTimer());
                    if (result) {
                        if (suffix.endsWith("zip")) {
                            try {
                                var zipFile = new ZipFile(tmpFile);
                                zipFile.extractAll(localJavaDir.getAbsolutePath());
                                zipFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println(ansi().fgBrightRed().a(bundle.getString("fail-to-uncompress")).fgDefault().toString());
                                return 1;
                            }
                        } else if (suffix.endsWith("tar.gz")) {
                            try {
                                GzipUtils.uncompressTGzipFile(tmpFile, localJavaDir);
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println(ansi().fgBrightRed().a(bundle.getString("fail-to-uncompress")).fgDefault().toString());
                                return 1;
                            }
                        }
                        System.out.println(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("successfully-install"), options.install)).fgDefault().toString());
                        return 0;
                    } else {
                        System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-install"), options.install)).fgDefault().toString());
                        return 1;
                    }
                }
            } else {
                System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("vm-not-found"), OSUtils.getProgramName())).fgDefault().toString());
                return 1;
            }
        } else if (options.uninstall) {
            var JVMs = printJVMs();
            var index = InputUtils.readIndex(bundle.getString("uninstall-index")) - 1;
            if (index >= JVMs.size()) {
                System.out.println(ansi().fgBrightRed().a(bundle.getString("index-out-of-range")).fgDefault().toString());
                return 1;
            } else {
                var vmInfo = JVMs.get(index);
                var vmFolder = vmInfo.getFile().getParentFile().getParentFile();
                FileUtils.deleteDir(vmFolder);
                if (!vmInfo.getFile().exists()) {
                    System.out.println(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("successfully-uninstall"), vmInfo.getInfo().getVendor())).fgDefault().toString());
                    return 0;
                } else {
                    System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-uninstall"), vmInfo.getInfo().getVendor())).fgDefault().toString());
                    return 1;
                }
            }
        }
        return 0;
    }

    private static JsonArray getVMList() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create("https://assets.powernukkitx.cn/jvms.json")).GET().build();
        try {
            var result = JsonParser.parseString(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
            if (result.isJsonArray()) {
                return result.getAsJsonArray();
            }
            return new JsonArray();
        } catch (IOException | InterruptedException e) {
            return new JsonArray();
        }
    }

    private static JsonObject defaultDownloadObj() {
        var tmp = new JsonObject();
        tmp.addProperty("url", "");
        tmp.addProperty("size", "unknown");
        return tmp;
    }

    /**
     * 打印本机已经安装了的所有JVM
     *
     * @return 所有JVM
     */
    private List<Location<JavaLocator.JavaInfo>> printJVMs() {
        var javaLocations = new JavaLocator(null, true).locate();
        var out = new StringBuilder(bundle.getString("installed-JVMs")).append('\n');
        for (int i = 0, javaLocationsSize = javaLocations.size(); i < javaLocationsSize; i++) {
            var each = javaLocations.get(i);
            out.append(ansi().a(i + 1).a(". ").fgBrightYellow().a(each.getInfo().getVendor()).fgDefault()
                    .a(" - ").fgBrightCyan().a("java ").a(each.getInfo().getMajorVersion()).fgDefault().a("\n   ")
                    .a(each.getFile().getAbsolutePath()).a("\n").toString());
        }
        System.out.println(out);
        return javaLocations;
    }
}
