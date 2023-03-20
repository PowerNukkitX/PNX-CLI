package cn.powernukkitx.cli.data.bean;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public record ArtifactBean(
        String name,
        Date createAt,
        Date expiresAt,
        long sizeInBytes,
        long downloadId
) {
    @Contract("_ -> new")
    public static @NotNull ArtifactBean from(@NotNull JsonObject jsonObject) {
        return new ArtifactBean(
                jsonObject.get("name").getAsString(),
                new Date(jsonObject.get("createAt").getAsLong()),
                new Date(jsonObject.get("expiresAt").getAsLong()),
                jsonObject.get("sizeInBytes").getAsLong(),
                jsonObject.get("downloadId").getAsLong()
        );
    }
}
