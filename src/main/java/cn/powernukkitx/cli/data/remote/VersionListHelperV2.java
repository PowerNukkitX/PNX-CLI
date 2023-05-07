package cn.powernukkitx.cli.data.remote;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.bean.*;
import cn.powernukkitx.cli.util.HttpUtils;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static cn.powernukkitx.cli.util.HttpUtils.getAPIUrl;

public final class VersionListHelperV2 {
    private VersionListHelperV2() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull ReleaseBean getLatestRelease() throws IOException, InterruptedException {
        var client = HttpUtils.getClient();
        var request = HttpRequest.newBuilder(URI.create(getAPIUrl() + "/git/latest-release/PowerNukkitX/PowerNukkitX")).GET().build();
        var future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        var result = HttpUtils.joinFutureWithPlaceholder(future).body();
        return ReleaseBean.from(JsonParser.parseString(result).getAsJsonObject());
    }

    public static @NotNull ReleaseBean @NotNull [] getAllReleases() throws IOException, InterruptedException {
        var client = HttpUtils.getClient();
        var request = HttpRequest.newBuilder(URI.create(getAPIUrl() + "/git/all-releases/PowerNukkitX/PowerNukkitX")).GET().build();
        var future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        var result = HttpUtils.joinFutureWithPlaceholder(future).body();
        var jsonArray = JsonParser.parseString(result).getAsJsonArray();
        var releases = new ReleaseBean[jsonArray.size()];
        for (int i = 0; i < releases.length; i++) {
            releases[i] = ReleaseBean.from(jsonArray.get(i).getAsJsonObject());
        }
        return releases;
    }

    public static @NotNull BuildBean getLatestBuild() throws IOException, InterruptedException {
        var client = HttpUtils.getClient();
        var request = HttpRequest.newBuilder(URI.create(getAPIUrl() + "/git/latest-build/PowerNukkitX/PowerNukkitX")).GET().build();
        var future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        var result = HttpUtils.joinFutureWithPlaceholder(future).body();
        return BuildBean.from(JsonParser.parseString(result).getAsJsonObject());
    }

    public static @NotNull CompletableFuture<Map<String, RemoteFileBean>> getLatestReleaseLibs() {
        var client = HttpUtils.getClient();
        var request = HttpRequest.newBuilder(URI.create(getAPIUrl() + "/git/latest-release/PowerNukkitX/PowerNukkitX")).GET().build();
        return HttpUtils.warpFutureWithPlaceholder(
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)))
                .thenApply(HttpResponse::body)
                .thenApply(JsonParser::parseString)
                .thenApply(jsonElement -> {
                    var releaseBean = ReleaseBean.from(jsonElement.getAsJsonObject());
                    for (var artifact : releaseBean.artifacts()) {
                        if (artifact.name().equalsIgnoreCase("libs.tar.gz")) {
                            return artifact;
                        }
                    }
                    throw new IllegalStateException("No libs.tar.gz found in the latest release");
                })
                .thenCompose(VersionListHelperV2::getReleaseLibsFromArtifact);
    }

    public static @NotNull CompletableFuture<Map<String, RemoteFileBean>> getLatestBuildLibs() {
        var client = HttpUtils.getClient();
        var request = HttpRequest.newBuilder(URI.create(getAPIUrl() + "/git/latest-build/PowerNukkitX/PowerNukkitX")).GET().build();
        return HttpUtils.warpFutureWithPlaceholder(
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)))
                .thenApply(HttpResponse::body)
                .thenApply(JsonParser::parseString)
                .thenApply(jsonElement -> BuildBean.from(jsonElement.getAsJsonObject()).libs())
                .thenCompose(VersionListHelperV2::getReleaseLibsFromArtifact);
    }

    public static @NotNull CompletableFuture<Map<String, RemoteFileBean>> getReleaseLibsFromArtifact(@NotNull ArtifactBean artifactBean) {
        var client = HttpUtils.getClient();
        var decompressRequest = HttpRequest.newBuilder()
                .uri(URI.create(getAPIUrl() + "/download/decompress/" + artifactBean.downloadId()))
                .GET()
                .build();
        return HttpUtils.warpFutureWithPlaceholder(
                client.sendAsync(decompressRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)))
                .thenApply(HttpResponse::body)
                .thenApply(JsonParser::parseString)
                .thenCompose(jsonElement ->
                        HttpUtils.warpFutureWithPlaceholder(HttpUtils.getDelayedResponse(Main.getTimer(), RequestIDBean.from(jsonElement.getAsJsonObject()))))
                .thenApply(JsonParser::parseString)
                .thenApply(jsonElement -> jsonElement.getAsJsonObject().entrySet())
                .thenApply(entries -> {
                    var map = new HashMap<String, RemoteFileBean>();
                    for (var entry : entries) {
                        var bean = RemoteFileBean.from(entry.getValue().getAsJsonObject());
                        map.put(bean.fileName(), bean);
                    }
                    return map;
                });
    }
}
