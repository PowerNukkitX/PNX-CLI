package cn.powernukkitx.cli.data.bean;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public record BuildBean(
        ArtifactBean libs,
        ArtifactBean full,
        ArtifactBean core,
        ArtifactBean hashes
) {
    public static @NotNull BuildBean from(@NotNull JsonObject jsonObject) {
        return new BuildBean(
                ArtifactBean.from(jsonObject.get("libs").getAsJsonObject()),
                ArtifactBean.from(jsonObject.get("full").getAsJsonObject()),
                ArtifactBean.from(jsonObject.get("core").getAsJsonObject()),
                ArtifactBean.from(jsonObject.get("hashes").getAsJsonObject())
        );
    }
}
