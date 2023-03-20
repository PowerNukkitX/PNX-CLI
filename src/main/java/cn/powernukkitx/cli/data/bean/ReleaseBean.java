package cn.powernukkitx.cli.data.bean;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.stream.StreamSupport;

public record ReleaseBean(
        String name,
        String tagName,
        String body,
        Date publishedAt,
        ArtifactBean[] artifacts
) {
    @Contract("_ -> new")
    public static @NotNull ReleaseBean from(@NotNull JsonObject jsonObject) {
        return new ReleaseBean(
                jsonObject.get("name").getAsString(),
                jsonObject.get("tagName").getAsString(),
                jsonObject.get("body").getAsString(),
                new Date(jsonObject.get("publishedAt").getAsLong()),
                StreamSupport.stream(jsonObject.get("artifacts").getAsJsonArray().spliterator(), false)
                        .map(JsonObject.class::cast)
                        .map(ArtifactBean::from)
                        .toArray(ArtifactBean[]::new)
        );
    }
}
