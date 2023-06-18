package cn.powernukkitx.cli.util;

import cn.powernukkitx.cli.share.CLIConstant;
import com.google.gson.JsonParser;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public final class CheckUpdateUtil {
    private CheckUpdateUtil() {

    }

    public static void scheduleCheckUpdate(@NotNull Timer timer) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                var apiURL = HttpUtils.getAPIUrl("/git/latest-release/PowerNukkitX/PNX-CLI");
                var client = HttpUtils.getClient();
                try {
                    var result = client.send(HttpRequest.newBuilder(URI.create(apiURL)).GET()
                            .timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString()).body();
                    var jsonObject = JsonParser.parseString(result).getAsJsonObject();
                    var tagName = jsonObject.get("tagName").getAsString();
                    if (compareVersion(tagName, CLIConstant.version) > 0) {
                        var str = ResourceBundle.getBundle("cn.powernukkitx.cli.util.CheckUpdate")
                                .getString("new-version-detected");
                        Logger.info(Ansi.ansi().fgBrightYellow().a(str.formatted(tagName)).fgDefault());
                    }
                } catch (Exception ignored) {
                    // ignored
                }
            }
        }, 1000 * 30, 1000 * 60 * 60 * 24);
    }

    /**
     * 比较版本号（如0.1.2）
     *
     * @param a 版本号a
     * @param b 版本号b
     * @return a > b 返回1，a = b 返回0，a < b 返回-1
     */
    public static int compareVersion(@NotNull String a, @NotNull String b) {
        var aSplit = a.split("\\.");
        var bSplit = b.split("\\.");
        var length = Math.min(aSplit.length, bSplit.length);
        for (var i = 0; i < length; i++) {
            var ai = Integer.parseInt(aSplit[i]);
            var bi = Integer.parseInt(bSplit[i]);
            if (ai > bi) {
                return 1;
            } else if (ai < bi) {
                return -1;
            }
        }
        if (aSplit.length > bSplit.length) {
            return 1;
        } else if (aSplit.length < bSplit.length) {
            return -1;
        }
        return 0;
    }
}
