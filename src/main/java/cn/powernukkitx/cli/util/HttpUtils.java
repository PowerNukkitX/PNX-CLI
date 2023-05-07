package cn.powernukkitx.cli.util;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.bean.RequestIDBean;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static cn.powernukkitx.cli.util.ConfigUtils.debug;
import static cn.powernukkitx.cli.util.StringUtils.displayableBytes;
import static org.fusesource.jansi.Ansi.ansi;

public final class HttpUtils {
    private HttpUtils() {

    }

    private static final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.util.Http");
    public static final Map<String, String> API_ADDRESSES = Map.of(
            "official", "www.powernukkitx.com",
            "nullatom", "pnx.nullatom.com"
    );
    private static String selectedApi = null;

    public static HttpClient client = null;

    public static HttpClient getClient() {
        if (client == null) {
            var builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL);
            if (ConfigUtils.httpProxy() != null) {
                builder.proxy(ProxySelector.of(ConfigUtils.httpProxy()));
            }
            client = builder.build();
        }
        return client;
    }

    public static @NotNull String getAPIUrl() {
        return getAPIUrl("");
    }

    public record EndpointAndPing(String endpoint, long ping) {
    }

    public static @NotNull String getAPIUrl(@NotNull String path) {
        if (selectedApi == null) {
            if (ConfigUtils.apiEndpoint() != null) {
                selectedApi = ConfigUtils.apiEndpoint();
                Logger.info(bundle.getString("using-source").formatted(selectedApi));
            } else {
                Logger.info(ansi().fgBrightYellow().a(bundle.getString("detecting")));
                // ping and find the fastest API endpoint use /api/ping
                var pingList = pingAPIEndpoints().values();
                // if any api endpoint is available, return it
                var result = ((EndpointAndPing) CompletableFuture.anyOf(pingList.toArray(CompletableFuture[]::new)).join());
                for (var future : pingList) {
                    if (!future.isDone()) future.cancel(true);
                }
                selectedApi = result.endpoint();
                ConfigUtils.set("api-endpoint", selectedApi);
                Logger.info(ansi().fgBrightGreen().a(bundle.getString("selected-source-lag").formatted(selectedApi, result.ping())));
            }
        }
        return "https://" + API_ADDRESSES.get(selectedApi) + "/api" + path;
    }

    public static @NotNull Map<String, CompletableFuture<EndpointAndPing>> pingAPIEndpoints() {
        var pingList = new LinkedHashMap<String, CompletableFuture<EndpointAndPing>>();
        for (var entry : API_ADDRESSES.entrySet()) {
            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://" + entry.getValue() + "/api/ping"))
                    .build();
            try {
                var future = getClient().sendAsync(request, HttpResponse.BodyHandlers.discarding());
                var start = System.currentTimeMillis();
                pingList.put(entry.getKey(), future.thenApply(response -> ((System.currentTimeMillis() - start) / 2))
                        .exceptionally(e -> Long.MAX_VALUE).thenApply(e -> new EndpointAndPing(entry.getKey(), e)));
            } catch (Exception ignore) {
            }
        }
        return pingList;
    }

    public static <T> T joinFutureWithPlaceholder(@NotNull CompletableFuture<T> connectionFuture) {
        return joinFutureWithPlaceholder(Main.getTimer(), connectionFuture);
    }

    public static <T> T joinFutureWithPlaceholder(@NotNull Timer timer, @NotNull CompletableFuture<T> connectionFuture) {
        if (connectionFuture.isDone()) {
            return connectionFuture.join();
        }
        if (Ansi.isEnabled()) {
            var atomicBoolean = new AtomicBoolean(false);
            var timerTask = new TimerTask() {
                public int time = 0;

                @Override
                public void run() {
                    if (connectionFuture.isDone()) {
                        this.cancel();
                        atomicBoolean.set(true);
                        Logger.clearProgress();
                        return;
                    }
                    time++;
                    Logger.raw(ansi().fgBrightYellow().a("[").a("/-\\|".charAt(time % 4))
                            .a("] ").a(".".repeat(time % 7)).reset().toString());
                    Logger.ProgressPrefixLength.addAndGet(4 + time % 7);
                }
            };
            timer.schedule(timerTask, 10, 250);
            var tmp = connectionFuture.join();
            if (!atomicBoolean.get()) {
                timerTask.cancel();
                Logger.clearProgress();
            }
            return tmp;
        }
        return connectionFuture.join();
    }

    public static <T> @NotNull CompletableFuture<T> warpFutureWithPlaceholder(@NotNull CompletableFuture<T> connectionFuture) {
        return warpFutureWithPlaceholder(Main.getTimer(), connectionFuture);
    }

    public static <T> @NotNull CompletableFuture<T> warpFutureWithPlaceholder(@NotNull Timer timer, @NotNull CompletableFuture<T> connectionFuture) {
        if (connectionFuture.isDone()) {
            return connectionFuture;
        }
        if (Ansi.isEnabled()) {
            return CompletableFuture.supplyAsync(() -> {
                var atomicBoolean = new AtomicBoolean(false);
                var timerTask = new TimerTask() {
                    public int time = 0;

                    @Override
                    public void run() {
                        if (connectionFuture.isDone()) {
                            this.cancel();
                            atomicBoolean.set(true);
                            Logger.clearProgress();
                            return;
                        }
                        time++;
                        Logger.raw(ansi().fgBrightYellow().a("[").a("/-\\|".charAt(time % 4))
                                .a("] ").a(".".repeat(time % 7)).reset().toString());
                        Logger.ProgressPrefixLength.addAndGet(4 + time % 7);
                    }
                };
                timer.schedule(timerTask, 10, 250);
                var tmp = connectionFuture.join();
                if (!atomicBoolean.get()) {
                    timerTask.cancel();
                    Logger.clearProgress();
                }
                return tmp;
            });
        }
        return connectionFuture;
    }

    public static @NotNull CompletableFuture<String> getDelayedResponse(@NotNull Timer timer, RequestIDBean requestIDBean) {
        var completableFuture = new CompletableFuture<String>();
        var timerTask = new TimerTask() {
            private int retry = 0;

            @Override
            public void run() {
                try {
                    this.retry++;
                    var request = HttpRequest.newBuilder(URI.create(getAPIUrl("/delayed/query/") + requestIDBean.uuid())).GET().build();
                    var response = getClient().send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        completableFuture.complete(response.body());
                        this.cancel();
                        completableFuture.complete(response.body());
                    }
                    if (response.statusCode() == 404) {
                        completableFuture.completeExceptionally(new IllegalAccessError("404"));
                        this.cancel();
                    }
                    if (this.retry > 40) {
                        completableFuture.completeExceptionally(new IllegalAccessError("Retry too many times"));
                        this.cancel();
                    }
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            }
        };
        timer.schedule(timerTask, 200, 500);
        return completableFuture;
    }

    public static boolean downloadWithBar(long downloadId, File target, String displayName, long estimatedSize, Timer timer) {
        return downloadWithBar(getAPIUrl("/download/") + downloadId, target, displayName, estimatedSize, timer);
    }

    public static boolean downloadWithBar(String downloadURL, File target, String displayName, Timer timer) {
        return downloadWithBar(downloadURL, target, displayName, 0, timer);
    }

    public static boolean downloadWithBar(String downloadURL, File target, String displayName, long estimatedSize, Timer timer) {
        try {
            var client = getClient();
            var request = HttpRequest.newBuilder(URI.create(downloadURL)).GET().build();
            Logger.info(ansi().fgBrightDefault().a(new Formatter().format(bundle.getString("connecting"), downloadURL)).fgDefault().toString());
            Logger.raw("");
            var contentLength = new AtomicLong();
            var atomicLong = new AtomicLong(0);
            var task = new TimerTask() {
                @Override
                public void run() {
                    if (contentLength.get() > 0) {
                        try {
                            final long finished = target.length();
                            final long total = contentLength.get();
                            final long speed = finished - atomicLong.get();
                            atomicLong.set(finished);
                            bar((float) (finished * 1.0 / total), displayableBytes(finished) + "/" +
                                    displayableBytes(total) + " (" + displayableBytes(speed) + "/s)");
                            if (finished == total) {
                                this.cancel();
                            }
                        } catch (Exception e) {
                            if (debug()) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };
            timer.scheduleAtFixedRate(task, 500, 500);
            if (!target.exists()) {
                //noinspection ResultOfMethodCallIgnored
                target.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                target.createNewFile();
            }
            try (var fos = new FileOutputStream(target)) {
                var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (var fis = response.body()) {
                    contentLength.set(response.headers().firstValueAsLong("Content-Length").orElse(estimatedSize));
                    fis.transferTo(fos);
                }
            }
            AnsiConsole.out().println(ansi().saveCursorPosition().cursorUpLine().eraseLine());
            task.cancel();
            Logger.info(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("success"), displayName)).fgDefault());
            return true;
        } catch (Exception e) {
            Logger.error(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail"), displayName)).fgDefault());
            if (debug()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static void bar(float percent, String append) {
        percent = Math.min(1, percent);
        final int width = AnsiConsole.getTerminalWidth();
        final var ansi = ansi().cursorUpLine().eraseLine().fgBrightGreen().a("[");
        final int barWidth = Math.max(width - 12 - StringUtils.getPrintLength(append), 8);
        final int finishedWidth = Math.round(percent * barWidth);
        ansi.a("=".repeat(finishedWidth));
        ansi.fgBrightYellow().a("-".repeat(barWidth - finishedWidth));
        ansi.a("] ").reset().a(String.format("%.2f%%", percent * 100)).a(" ").a(append);
        System.out.println(ansi);
    }
}
