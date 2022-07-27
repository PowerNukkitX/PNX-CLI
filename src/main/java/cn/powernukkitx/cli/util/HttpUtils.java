package cn.powernukkitx.cli.util;

import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static cn.powernukkitx.cli.util.ConfigUtils.debug;
import static cn.powernukkitx.cli.util.StringUtils.displayableBytes;
import static org.fusesource.jansi.Ansi.ansi;

public final class HttpUtils {
    private HttpUtils() {

    }

    private static final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.util.Http");

    public static boolean downloadWithBar(String downloadURL, File target, String displayName, Timer timer) {
        try {
            var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
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
                    contentLength.set(response.headers().firstValueAsLong("Content-Length").orElse(0));
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
