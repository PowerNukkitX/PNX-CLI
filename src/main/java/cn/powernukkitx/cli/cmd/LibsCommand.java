package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.bean.ReleaseBean;
import cn.powernukkitx.cli.data.bean.RemoteFileBean;
import cn.powernukkitx.cli.data.remote.VersionListHelperV2;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.FileUtils;
import cn.powernukkitx.cli.util.HttpUtils;
import cn.powernukkitx.cli.util.InputUtils;
import cn.powernukkitx.cli.util.Logger;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static cn.powernukkitx.cli.util.StringUtils.commonTimeFormat;
import static org.fusesource.jansi.Ansi.ansi;

@Command(name = "libs", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Libs")
public final class LibsCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Libs");

    @ArgGroup(multiplicity = "1")
    AllOptions options;

    static class AllOptions {
        @Option(names = {"update", "-u", "fix", "-f"}, help = true, descriptionKey = "update")
        boolean update;
        @Option(names = {"check", "-c"}, help = true, descriptionKey = "check")
        boolean check;
    }

    @ArgGroup(multiplicity = "1")
    VersionOptions versionOptions;

    static class VersionOptions {
        @Option(names = "--latest", help = true, descriptionKey = "latest")
        public boolean latest = false;
        @Option(names = "--dev", help = true, descriptionKey = "dev")
        public boolean dev = false;
    }

    @Override
    public Integer call() {
        if (options != null && options.update) {
            var libDir = new File(CLIConstant.userDir, "libs");
            if (!libDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                libDir.mkdirs();
            }
            if (versionOptions != null && versionOptions.dev) {
                return updateDev(false);
            } else if (versionOptions != null && versionOptions.latest) {
                return updateLatest(false);
            } else {
                return updateAllVersion(false);
            }
        } else if (options != null && options.check) {
            if (versionOptions != null && versionOptions.latest) {
                return updateLatest(true);
            } else if (versionOptions != null && versionOptions.dev) {
                return updateDev(true);
            } else {
                return updateAllVersion(true);
            }
        } else {
            CommandLine.usage(this, System.out);
        }
        return 0;
    }

    public @NotNull Integer updateLatest(boolean check) {
        Logger.info(ansi().fgBrightYellow().a(bundle.getString("fetching-manifest")).fgDefault());
        var future = VersionListHelperV2.getLatestReleaseLibs();
        var remoteFileBeanMap = future.exceptionally(throwable -> {
            Logger.error(ansi().fgBrightRed().a(bundle.getString("fail-to-get-manifest")).fgDefault());
            throwable.printStackTrace();
            return null;
        }).join();
        if (remoteFileBeanMap == null) {
            return 1;
        } else {
            if (check) {
                return check(remoteFileBeanMap);
            } else {
                return install(remoteFileBeanMap);
            }
        }
    }

    public @NotNull Integer updateDev(boolean check) {
        Logger.info(ansi().fgBrightYellow().a(bundle.getString("fetching-manifest")).fgDefault());
        var future = VersionListHelperV2.getLatestBuildLibs();
        var remoteFileBeanMap = future.exceptionally(throwable -> {
            Logger.error(ansi().fgBrightRed().a(bundle.getString("fail-to-get-manifest")).fgDefault());
            throwable.printStackTrace();
            return null;
        }).join();
        if (remoteFileBeanMap == null) {
            return 1;
        } else {
            if (check) {
                return check(remoteFileBeanMap);
            } else {
                return install(remoteFileBeanMap);
            }
        }
    }

    public @NotNull Integer updateAllVersion(boolean check) {
        // 获取所有版本
        ReleaseBean[] allReleases;
        try {
            allReleases = VersionListHelperV2.getAllReleases();
        } catch (IOException | InterruptedException e) {
            Logger.error(ansi().fgBrightRed().a(bundle.getString("fail-to-get-version-list")).fgDefault());
            e.printStackTrace();
            return -1;
        }
        Logger.info(ansi().fgBrightDefault().a(bundle.getString("available-version")).fgDefault());
        for (int i = 0, len = allReleases.length; i < len; i++) {
            var entry = allReleases[i];
            var time = commonTimeFormat.format(entry.publishedAt());
            Logger.info((i + 1) + ". " + entry.tagName() + " (" + time + ")");
        }
        // 等待用户选择
        var index = InputUtils.readIndex(bundle.getString("choose-version")) - 1;
        if (index < 0 || index >= allReleases.length) {
            Logger.error(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("invalid-index"), allReleases.length)).fgDefault());
            return 1;
        }
        var release = allReleases[index];
        Logger.info(ansi().fgBrightYellow().a(bundle.getString("fetching-manifest")).fgDefault());
        var future = VersionListHelperV2.getReleaseLibsFromArtifact(Arrays.stream(release.artifacts())
                .filter(artifact -> artifact.name().equalsIgnoreCase("libs.tar.gz")).findFirst().orElseThrow());
        var remoteFileBeanMap = future.exceptionally(throwable -> {
            Logger.error(ansi().fgBrightRed().a(bundle.getString("fail-to-get-manifest")).fgDefault());
            throwable.printStackTrace();
            return null;
        }).join();
        if (remoteFileBeanMap == null) {
            return 1;
        } else {
            if (check) {
                return check(remoteFileBeanMap);
            } else {
                return install(remoteFileBeanMap);
            }
        }
    }

    public @NotNull Integer check(Map<String, RemoteFileBean> remoteFileBeanMap) {
        var libDir = new File(CLIConstant.userDir, "libs");
        if (!libDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            libDir.mkdirs();
        }
        var remoteFileBeanMapCopy = new HashMap<>(remoteFileBeanMap);
        for (var childFile : Objects.requireNonNull(libDir.listFiles())) {
            if (remoteFileBeanMapCopy.containsKey(childFile.getName())) {
                var remoteFileBean = remoteFileBeanMapCopy.remove(childFile.getName());
                try {
                    if (FileUtils.getMD5(childFile).equals(remoteFileBean.md5())) {
                        Logger.info(ansi().fgBrightGreen().a("+ ").a(childFile.getName()).fgDefault());
                    } else {
                        Logger.info(ansi().fgBrightYellow().a("~ ").a(childFile.getName()).fgDefault());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Logger.info(ansi().fgBrightRed().a("- ").a(childFile.getName()).fgDefault());
            }
        }
        for (var remoteFileBean : remoteFileBeanMapCopy.values()) {
            Logger.info(ansi().fgBrightRed().a("+ ").a(remoteFileBean.fileName()).fgDefault());
        }
        return 0;
    }

    public @NotNull Integer install(Map<String, RemoteFileBean> remoteFileBeanMap) {
        var libDir = new File(CLIConstant.userDir, "libs");
        if (!libDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            libDir.mkdirs();
        }
        var remoteFileBeanMapCopy = new HashMap<>(remoteFileBeanMap);
        for (var childFile : Objects.requireNonNull(libDir.listFiles())) {
            if (remoteFileBeanMapCopy.containsKey(childFile.getName())) {
                var remoteFileBean = remoteFileBeanMapCopy.remove(childFile.getName());
                try {
                    if (!FileUtils.getMD5(childFile).equals(remoteFileBean.md5())) {
                        var result = childFile.delete();
                        if (!result) {
                            Logger.warn(ansi().fgBrightRed().a(bundle.getString("fail-to-update")
                                    .formatted(remoteFileBean.fileName())).fgDefault());
                            continue;
                        }
                        HttpUtils.downloadWithBar(HttpUtils.getAPIUrl("/download/") + remoteFileBean.downloadID(),
                                childFile, remoteFileBean.fileName(), Main.getTimer());
                    }
                } catch (IOException e) {
                    Logger.warn(ansi().fgBrightRed().a(bundle.getString("fail-to-update")
                            .formatted(remoteFileBean.fileName())).fgDefault());
                    e.printStackTrace();
                }
            } else {
                var result = childFile.delete();
                if (!result) {
                    Logger.warn(ansi().fgBrightRed().a(bundle.getString("fail-to-update")
                            .formatted(childFile.getName())).fgDefault());
                }
            }
        }
        for (var remoteFileBean : remoteFileBeanMapCopy.values()) {
            var file = new File(libDir, remoteFileBean.fileName());
            try {
                HttpUtils.downloadWithBar(HttpUtils.getAPIUrl("/download/") + remoteFileBean.downloadID(),
                        file, remoteFileBean.fileName(), Main.getTimer());
            } catch (Exception e) {
                Logger.warn(ansi().fgBrightRed().a(bundle.getString("fail-to-update")
                        .formatted(remoteFileBean.fileName())).fgDefault());
                e.printStackTrace();
            }
        }
        Logger.info(ansi().fgBrightGreen().a(bundle.getString("successfully-update")).fgDefault());
        return 0;
    }
}
