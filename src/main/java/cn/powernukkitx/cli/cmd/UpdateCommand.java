package cn.powernukkitx.cli.cmd;

import cn.powernukkitx.cli.data.bean.GitHubArtifactBean;
import cn.powernukkitx.cli.data.locator.JarLocator;
import cn.powernukkitx.cli.data.remote.GithubHelper;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.*;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Formatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "update", mixinStandardHelpOptions = true, resourceBundle = "cn.powernukkitx.cli.cmd.Update")
public class UpdateCommand implements Callable<Integer> {
    private final ResourceBundle bundle = ResourceBundle.getBundle("cn.powernukkitx.cli.cmd.Update");

    @CommandLine.Option(names = {"-c", "--core"}, descriptionKey = "update-core", help = true)
    public boolean core;

    @CommandLine.Option(names = {"-l", "--libs"}, descriptionKey = "update-libs", help = true)
    public boolean libs;
    @CommandLine.Option(names = {"-a", "--all"}, descriptionKey = "update-all", help = true)
    public boolean all;
    boolean latest = false;

    @Override
    public Integer call() throws Exception {
        List<GitHubArtifactBean> allReleases;
        if (core) {
            // 获取最新的10个版本
            allReleases = GithubHelper.listAllArtifacts(GithubHelper.ArtifactType.CORE, 10);
        } else if (libs) {
            allReleases = GithubHelper.listAllArtifacts(GithubHelper.ArtifactType.LIBS, 10);
        } else if (all) {
            allReleases = GithubHelper.listAllArtifacts(GithubHelper.ArtifactType.CORE, 10);
        } else {
            CommandLine.usage(this, System.out);
            return 0;
        }
        int index;
        if (latest) {
            Logger.info(ansi().fgBrightYellow().a(bundle.getString("update-latest")).fgDefault());
            index = 1;
        } else {
            Logger.info(ansi().fgBrightDefault().a(bundle.getString("available-version")).fgDefault());
            for (int i = 0, len = allReleases.size(); i < len; i++) {
                var entry = allReleases.get(i);
                var time = entry.getCreatedAt().format(StringUtils.commonLocalTimeFormat);
                int number = i + 1;
                Logger.info(number + "." + " ".repeat(3 - (int) Math.log10(number)) + entry.getWorkflowRun().getTag() + " (" + time + ")");
            }
            // 等待用户选择
            index = InputUtils.readIndex(bundle.getString("choose-version")) - 1;
            if (index < 0 || index >= allReleases.size()) {
                System.out.println(ansi().fgBrightRed().a(new Formatter().format(bundle.getString("invalid-index"), allReleases.size())).fgDefault());
                return 1;
            }
        }
        // 获取用户选择的版本
        var release = allReleases.get(index);
        if (all) {
            GitHubArtifactBean core = GithubHelper.getGitHubArtifact(release.getId());
            GitHubArtifactBean libs = GithubHelper.getGitHubArtifact(release.getId() + 1);
            if (core == null || libs == null) return 1;
            boolean r = updateCore(core);
            r &= updateLibs(libs);
            return r ? 0 : 1;
        } else if (core) {
            return updateCore(release) ? 0 : 1;
        } else if (libs) {
            return updateLibs(release) ? 0 : 1;
        } else {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    private static boolean updateLibs(GitHubArtifactBean release) throws IOException {
        String displayName = "PowerNukkitX-Libs v" + release.getWorkflowRun().getTag();
        release.setSizeInBytes((long) Math.ceil(release.getSizeInBytes() * 0.635));//github 返回的是未压缩的大小，libs压缩率大概在0.635
        String libsName = "libs.zip";
        boolean result = GithubHelper.downloadArtifact(displayName, libsName, release);
        if (result) {
            File libs = new File("libs");
            FileUtils.deleteDir(libs);
            File file = new File(libsName);
            CompressUtils.uncompressZipFile(file, libs);
            Files.deleteIfExists(file.toPath());
        }
        return result;
    }

    private static boolean updateCore(GitHubArtifactBean release) throws IOException {
        String displayName = "PowerNukkitX-Core v" + release.getWorkflowRun().getTag();
        release.setSizeInBytes((long) Math.ceil(release.getSizeInBytes() * 0.944));//github 返回的是未压缩的大小，core压缩率大概在0.944
        String coreName = "powernukkitx.zip";
        boolean result = GithubHelper.downloadArtifact(displayName, coreName, release);
        if (result) {
            // 删除旧的核心文件
            //noinspection ResultOfMethodCallIgnored
            new JarLocator(CLIConstant.userDir, "cn.nukkit.PlayerHandle").locate().forEach(each -> each.getFile().delete());
            File file = new File(coreName);
            CompressUtils.uncompressZipFile(file, new File(""));
            Files.deleteIfExists(file.toPath());
        }
        return result;
    }
}
