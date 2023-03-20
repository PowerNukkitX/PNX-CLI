package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.bean.ArtifactBean;
import cn.powernukkitx.cli.data.bean.ReleaseBean;
import cn.powernukkitx.cli.data.locator.JarLocator;
import cn.powernukkitx.cli.data.remote.VersionListHelperV2;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.HttpUtils;
import cn.powernukkitx.cli.util.InputUtils;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "server", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Server")
public final class ServerCommand implements Callable<Integer> {
    public static final SimpleDateFormat commonTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Server");

    @Option(names = {"update", "-u", "install", "-i"}, help = true, descriptionKey = "update")
    public boolean update;

    @Option(names = "--latest", help = true, descriptionKey = "latest")
    public boolean latest = false;

    @Override
    public Integer call() throws ParseException {
        if (update) {
            if (latest) {
                return updateLatest();
            } else {
                return updateAllVersion();
            }
        } else {
            CommandLine.usage(this, System.out);
        }
        return 0;
    }

    public @NotNull Integer updateLatest() {
        String displayName = "PowerNukkitX-Core Unknown";
        try {
            // 获取最新版本
            var release = VersionListHelperV2.getLatestRelease();
            displayName = "PowerNukkitX-Core v" + release.tagName();
            return downloadCore(displayName, release);
        } catch (IOException | InterruptedException e) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-install"), displayName)).fgDefault());
            e.printStackTrace();
            return 1;
        }
    }

    public @NotNull Integer updateAllVersion() {
        String displayName = "PowerNukkitX-Core Unknown";
        try {
            // 获取所有版本
            var allReleases = VersionListHelperV2.getAllReleases();
            System.out.println(ansi().fgBrightDefault().a(bundle.getString("available-version")).fgDefault());
            for (int i = 0, len = allReleases.length; i < len; i++) {
                var entry = allReleases[i];
                var time = commonTimeFormat.format(entry.publishedAt());
                System.out.println((i + 1) + ". " + entry.tagName() + " (" + time + ")");
            }
            // 等待用户选择
            var index = InputUtils.readIndex(bundle.getString("choose-version")) - 1;
            if (index < 0 || index >= allReleases.length) {
                System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("invalid-index"), allReleases.length)).fgDefault());
                return 1;
            }
            // 获取用户选择的版本
            var release = allReleases[index];
            displayName = "PowerNukkitX-Core v" + release.tagName();
            return downloadCore(displayName, release);
        } catch (IOException | InterruptedException e) {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-install"), displayName)).fgDefault());
            e.printStackTrace();
            return 1;
        }
    }

    public @NotNull Integer downloadCore(String displayName, ReleaseBean release) {
        // 删除旧的核心文件
        //noinspection ResultOfMethodCallIgnored
        new JarLocator(CLIConstant.userDir, "cn.nukkit.api.PowerNukkitXOnly").locate().forEach(each -> each.getFile().delete());
        // 查找核心工件
        var coreArtifact = getCoreArtifact(release);
        var result = HttpUtils.downloadWithBar(coreArtifact.downloadId(),
                new File(CLIConstant.userDir, "PowerNukkitX-" + release.tagName() + ".jar"), displayName,
                coreArtifact.sizeInBytes(), Main.getTimer());
        if (result) {
            System.out.println(ansi().fgBrightGreen().a(new Formatter().format(bundle.getString("successfully-installed"), displayName)).fgDefault());
            return 0;
        } else {
            System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("fail-to-install"), displayName)).fgDefault());
            return 1;
        }
    }

    private static @NotNull ArtifactBean getCoreArtifact(@NotNull ReleaseBean release) {
        return Arrays.stream(release.artifacts()).filter(each -> each.name().equalsIgnoreCase("powernukkitx.jar")).findFirst().orElseThrow();
    }
}
