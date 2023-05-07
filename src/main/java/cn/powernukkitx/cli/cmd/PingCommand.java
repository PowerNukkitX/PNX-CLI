package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.util.ConfigUtils;
import cn.powernukkitx.cli.util.HttpUtils;
import cn.powernukkitx.cli.util.Logger;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "ping", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Ping")
public final class PingCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Ping");

    @Override
    public Integer call() throws Exception {
        Logger.info(ansi().fgBrightYellow().a(bundle.getString("checking")));
        var pingMap = HttpUtils.pingAPIEndpoints();
        var resultList = new ArrayList<HttpUtils.EndpointAndPing>(pingMap.size());
        pingMap.forEach((endPoint, future) -> future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                Logger.warn(ansi().fgBrightRed().a(bundle.getString("error").formatted(endPoint, throwable.getLocalizedMessage())));
            } else if (result.ping() == Long.MAX_VALUE) {
                Logger.warn(ansi().fgBrightRed().a(bundle.getString("error").formatted(endPoint, bundle.getString("timeout"))));
            } else {
                Logger.info(ansi().fgGreen().a(bundle.getString("success").formatted(endPoint, result.ping())).reset());
                resultList.add(result);
            }
        }));
        HttpUtils.joinFutureWithPlaceholder(CompletableFuture.allOf(pingMap.values().toArray(CompletableFuture[]::new)));
        if (resultList.isEmpty()) {
            Logger.error(ansi().fgBrightRed().a(bundle.getString("no-available")));
        } else {
            resultList.sort(Comparator.comparingLong(HttpUtils.EndpointAndPing::ping));
            var best = resultList.get(0);
            Logger.info(ansi().fgBrightGreen().a(bundle.getString("selected-source-lag").formatted(best.endpoint(), best.ping())));
            ConfigUtils.set("api-endpoint", best.endpoint());
        }
        return 0;
    }
}
