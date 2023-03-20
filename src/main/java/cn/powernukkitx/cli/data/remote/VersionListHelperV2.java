package cn.powernukkitx.cli.data.remote;

import cn.powernukkitx.cli.data.bean.ReleaseBean;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class VersionListHelperV2 {
    public static final String API_URL = "https://www.powernukkitx.com/api";

    private VersionListHelperV2() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull ReleaseBean getLatestRelease() throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(API_URL + "/git/latest-release/PowerNukkitX/PowerNukkitX")).GET().build();
        var result = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        return ReleaseBean.from(JsonParser.parseString(result).getAsJsonObject());
    }

    public static @NotNull ReleaseBean @NotNull [] getAllReleases() throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(API_URL + "/git/all-releases/PowerNukkitX/PowerNukkitX")).GET().build();
        var result = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        var jsonArray = JsonParser.parseString(result).getAsJsonArray();
        var releases = new ReleaseBean[jsonArray.size()];
        for (int i = 0; i < releases.length; i++) {
            releases[i] = ReleaseBean.from(jsonArray.get(i).getAsJsonObject());
        }
        return releases;
    }

}
