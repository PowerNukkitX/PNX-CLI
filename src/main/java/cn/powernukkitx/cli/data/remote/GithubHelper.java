package cn.powernukkitx.cli.data.remote;

import cn.powernukkitx.cli.Main;
import cn.powernukkitx.cli.data.bean.GitHubArtifactBean;
import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.HttpUtils;
import cn.powernukkitx.cli.util.Logger;
import cn.powernukkitx.cli.util.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static cn.powernukkitx.cli.util.HttpUtils.getClient;
import static org.fusesource.jansi.Ansi.ansi;

public final class GithubHelper {
    public static String github_token_readonly = "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";

    public enum ArtifactType {
        CORE("PowerNukkitX-Core"),
        LIBS("PowerNukkitX-Libs");

        String name;

        ArtifactType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static GitHubArtifactBean getGitHubArtifact(long artifactId) {
        var request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/PowerNukkitX/PowerNukkitX/actions/artifacts/%s".formatted(artifactId)))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            var response = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject jsonObject = StringUtils.GSON.fromJson(response.body(), JsonObject.class);
                return GitHubArtifactBean.from(jsonObject);
            } else {
                Logger.error(ansi().fgBrightRed().a("getGitHubArtifact request error").fgDefault().toString());
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<GitHubArtifactBean> listAllArtifacts(ArtifactType artifactType, int pageNumber) {
        var request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/PowerNukkitX/PowerNukkitX/actions/artifacts?per_page=%s&name=%s".formatted(100, artifactType.getName())))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            var response = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject jsonObject = StringUtils.GSON.fromJson(response.body(), JsonObject.class);
                JsonArray artifacts = jsonObject.get("artifacts").getAsJsonArray();
                List<GitHubArtifactBean> gitHubArtifactBeans = new ArrayList<>();
                for (var art : artifacts) {
                    gitHubArtifactBeans.add(GitHubArtifactBean.from(art.getAsJsonObject()));
                }
                List<GitHubArtifactBean> result = gitHubArtifactBeans.stream()
                        .filter(g -> g.getWorkflowRun().getHeadBranch().equals("master"))
                        .sorted(Comparator.comparing(GitHubArtifactBean::getCreatedAt).reversed())
                        .toList();
                return result.subList(0, pageNumber);
            } else {
                Logger.error(ansi().fgBrightRed().a("listAllCoreArtifacts request error").fgDefault().toString());
                return Collections.EMPTY_LIST;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean downloadArtifact(String displayName, String fileName, GitHubArtifactBean coreArtifact) {
        String archiveDownloadUrl = coreArtifact.getArchiveDownloadUrl();
        var request = HttpRequest.newBuilder(URI.create(archiveDownloadUrl))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Authorization", "Bearer " + github_token_readonly)
                .GET()
                .build();
        return HttpUtils.downloadWithBar(request,
                new File(CLIConstant.userDir, fileName), displayName,
                coreArtifact.getSizeInBytes(), Main.getTimer());
    }
}
