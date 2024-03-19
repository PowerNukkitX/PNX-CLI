package cn.powernukkitx.cli.data.bean;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public class GitHubArtifactBean {
    private long id;
    private String nodeId;
    private String name;
    private long sizeInBytes;
    private String url;
    private String archiveDownloadUrl;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private WorkflowRun workflowRun;

    public GitHubArtifactBean(long id, String nodeId, String name, long sizeInBytes, String url, String archiveDownloadUrl, boolean expired, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt, WorkflowRun workflowRun) {
        this.id = id;
        this.nodeId = nodeId;
        this.name = name;
        this.sizeInBytes = sizeInBytes;
        this.url = url;
        this.archiveDownloadUrl = archiveDownloadUrl;
        this.expired = expired;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.workflowRun = workflowRun;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getArchiveDownloadUrl() {
        return archiveDownloadUrl;
    }

    public void setArchiveDownloadUrl(String archiveDownloadUrl) {
        this.archiveDownloadUrl = archiveDownloadUrl;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public WorkflowRun getWorkflowRun() {
        return workflowRun;
    }

    public void setWorkflowRun(WorkflowRun workflowRun) {
        this.workflowRun = workflowRun;
    }

    public static final DateTimeFormatter TIME_FORMATTER;

    static {
        TIME_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(ISO_LOCAL_TIME)
                .appendLiteral('Z')
                .toFormatter(Locale.getDefault(Locale.Category.FORMAT));
    }


    @Contract("_ -> new")
    public static GitHubArtifactBean from(JsonObject jsonObject) {
        return new GitHubArtifactBean(
                jsonObject.get("id").getAsLong(),
                jsonObject.get("node_id").getAsString(),
                jsonObject.get("name").getAsString(),
                jsonObject.get("size_in_bytes").getAsLong(),
                jsonObject.get("url").getAsString(),
                jsonObject.get("archive_download_url").getAsString(),
                jsonObject.get("expired").getAsBoolean(),
                LocalDateTime.parse(jsonObject.get("created_at").getAsString(), TIME_FORMATTER),
                LocalDateTime.parse(jsonObject.get("updated_at").getAsString(), TIME_FORMATTER),
                LocalDateTime.parse(jsonObject.get("expires_at").getAsString(), TIME_FORMATTER),
                WorkflowRun.from(jsonObject.getAsJsonObject("workflow_run"))
        );
    }

    @Override
    public String toString() {
        return "GitHubArtifactBean{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", name='" + name + '\'' +
                ", sizeInBytes=" + sizeInBytes +
                ", url='" + url + '\'' +
                ", archiveDownloadUrl='" + archiveDownloadUrl + '\'' +
                ", expired=" + expired +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", expiresAt=" + expiresAt +
                ", workflowRun=" + workflowRun +
                '}';
    }

    public static class WorkflowRun {
        private long id;
        private long repositoryId;
        private long headRepositoryId;
        private String headBranch;
        private String headSha;

        public WorkflowRun(long id, long repositoryId, long headRepositoryId, String headBranch, String headSha) {
            this.id = id;
            this.repositoryId = repositoryId;
            this.headRepositoryId = headRepositoryId;
            this.headBranch = headBranch;
            this.headSha = headSha;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getRepositoryId() {
            return repositoryId;
        }

        public void setRepositoryId(long repositoryId) {
            this.repositoryId = repositoryId;
        }

        public long getHeadRepositoryId() {
            return headRepositoryId;
        }

        public void setHeadRepositoryId(long headRepositoryId) {
            this.headRepositoryId = headRepositoryId;
        }

        public String getHeadBranch() {
            return headBranch;
        }

        public void setHeadBranch(String headBranch) {
            this.headBranch = headBranch;
        }

        public String getHeadSha() {
            return headSha;
        }

        public void setHeadSha(String headSha) {
            this.headSha = headSha;
        }

        public String getTag() {
            return getHeadSha().substring(0, 7);
        }

        @Contract("_ -> new")
        public static WorkflowRun from(JsonObject jsonObject) {
            return new WorkflowRun(
                    jsonObject.get("id").getAsLong(),
                    jsonObject.get("repository_id").getAsLong(),
                    jsonObject.get("head_repository_id").getAsLong(),
                    jsonObject.get("head_branch").getAsString(),
                    jsonObject.get("head_sha").getAsString()
            );
        }

        @Override
        public String toString() {
            return "WorkflowRun{" +
                    "id=" + id +
                    ", repositoryId=" + repositoryId +
                    ", headRepositoryId=" + headRepositoryId +
                    ", headBranch='" + headBranch + '\'' +
                    ", headSha='" + headSha + '\'' +
                    '}';
        }
    }
}
