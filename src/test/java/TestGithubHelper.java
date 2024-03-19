import cn.powernukkitx.cli.data.remote.GithubHelper;

public class TestGithubHelper {
    public static void main(String[] args) {
        GithubHelper.listAllArtifacts(GithubHelper.ArtifactType.CORE, 30).forEach(System.out::println);
        GithubHelper.listAllArtifacts(GithubHelper.ArtifactType.LIBS, 30).forEach(System.out::println);
        System.out.println(GithubHelper.getGitHubArtifact(1333960932));
        System.out.println(GithubHelper.getGitHubArtifact(1333960933));
    }
}
