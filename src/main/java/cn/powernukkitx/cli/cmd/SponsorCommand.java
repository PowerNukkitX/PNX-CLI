package cn.powernukkitx.cli.cmd;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static cn.powernukkitx.cli.util.ConfigUtils.debug;
import static cn.powernukkitx.cli.util.NullUtils.Ok;
import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "sponsor", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Sponsor")
public final class SponsorCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Sponsor");

    @Override
    public Integer call() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create("https://api.powernukkitx.cn/get-afdian-sponsor")).GET().build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            var sponsorArray = JsonParser.parseString(response).getAsJsonArray();
            var namesBuilder = new StringBuilder();
            for (var each : sponsorArray) {
                var sponsor = each.getAsJsonObject();
                var plan = Ok(sponsor.get("current_plan"), JsonElement::getAsJsonObject, e -> e.get("name"), JsonElement::getAsString, "");
                if (Ok(plan) && !"".equals(plan)) {
                    namesBuilder.append(Ok(sponsor.get("user"), JsonElement::getAsJsonObject, e -> e.get("name"), JsonElement::getAsString, "")).append(", ");
                }
            }
            var names = namesBuilder.toString();
            names = names.substring(0, names.length() - 2);
            System.out.println(ansi().fgBrightYellow().a(new Formatter().format(bundle.getString("thank"), names)).fgDefault());
        } catch (IOException | InterruptedException e) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail"), request.uri().toString())).fgDefault());
            if (debug()) {
                e.printStackTrace();
            }
            return 1;
        }
        return 0;
    }
}
