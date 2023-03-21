package cn.powernukkitx.cli.util;

import cn.powernukkitx.cli.data.bean.RequestIDBean;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static cn.powernukkitx.cli.util.ConfigUtils.debug;
import static cn.powernukkitx.cli.util.StringUtils.displayableBytes;
import static org.fusesource.jansi.Ansi.ansi;

public final class HttpUtils {
    private HttpUtils() {

    }

    private static final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.util.Http");
    public static final String DOWNLOAD_API = "https://www.powernukkitx.com/api/download";
    public static final String QUERY_API = "https://www.powernukkitx.com/api/delayed/query";

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

    public static @NotNull CompletableFuture<String> getDelayedResponse(@NotNull Timer timer, RequestIDBean requestIDBean) {
        var completableFuture = new CompletableFuture<String>();
        var timerTask = new TimerTask() {
            private int retry = 0;

            @Override
            public void run() {
                try {
                    this.retry++;
                    var request = HttpRequest.newBuilder(URI.create(QUERY_API + "/" + requestIDBean.uuid())).GET().build();
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
        return downloadWithBar(DOWNLOAD_API + "/" + downloadId, target, displayName, estimatedSize, timer);
    }

    public static boolean downloadWithBar(String downloadURL, File target, String displayName, Timer timer) {
        return downloadWithBar(downloadURL, target, displayName, 0, timer);
    }

    public static boolean downloadWithBar(String downloadURL, File target, String displayName, long estimatedSize, Timer timer) {
        try {
            var client = getClient();
            var request = HttpRequest.newBuilder(URI.create(downloadURL)).GET().build();
            System.out.println(ansi().fgBrightDefault().a(new Formatter().format(bundle.getString("connecting"), downloadURL)).fgDefault().toString());
            System.out.println();
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
            System.out.println(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("success"), displayName)).fgDefault());
            return true;
        } catch (Exception e) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail"), displayName)).fgDefault());
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
