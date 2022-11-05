package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.locator.JarLocator;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.HttpUtils;
import cn.powernukkitx.cli.util.InputUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "server", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Server")
public final class ServerCommand implements Callable<Integer> {
    public static final SimpleDateFormat utcTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final SimpleDateFormat commonTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Server");

    @Option(names = {"update", "-u", "install", "-i"}, help = true, descriptionKey = "update")
    public boolean update;

    @Option(names = "--latest", help = true, descriptionKey = "latest")
    public boolean latest = false;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Integer call() throws ParseException {
        if (update) {
            var versionCores = listVersion();
            System.out.println(ansi().fgBrightDefault().a(bundle.getString("available-version")).fgDefault());
            if (!latest) {
                for (int i = 0, len = versionCores.size(); i < len; i++) {
                    var entry = versionCores.get(i).getAsJsonObject();
                    var time = utcTimeFormat.parse(entry.get("lastModified").getAsString());
                    var names = entry.get("name").getAsString().split("-");
                    System.out.println((i + 1) + ". " + names[1] + " > " + names[0] + " (" + commonTimeFormat.format(time) + ")");
                }
            }
            var index = latest ? 0 : InputUtils.readIndex(bundle.getString("choose-version")) - 1;
            new JarLocator(CLIConstant.userDir, "cn.nukkit.api.PowerNukkitXOnly").locate().forEach(each -> each.getFile().delete());
            var name = versionCores.get(index).getAsJsonObject().get("name").getAsString();
            var result = HttpUtils.downloadWithBar(versionCores.get(index).getAsJsonObject().get("url").getAsString(),
                    new File(CLIConstant.userDir, "PowerNukkitX-" + name + ".jar"), "PowerNukkitX (" + name + ")", Main.getTimer());
            if (result) {
                System.out.println(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("successfully-installed"), name)).fgDefault());
                return 0;
            } else {
                System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-install"), name)).fgDefault());
                return 1;
            }
        } else {
            CommandLine.usage(this, System.out);
        }
        return 0;
    }

    @SuppressWarnings("DuplicatedCode")
    private static JsonArray listVersion() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create("https://api.powernukkitx.cn/get-core-manifest")).GET().build();
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
}
