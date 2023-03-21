package cn.powernukkitx.cli.data.bean;

import cn.powernukkitx.cli.util.StringUtils;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public record RemoteFileBean(
        String fullPath,
        long size,
        Date lastUpdateTime,
        String md5,
        long downloadID
) {
    @Contract("_ -> new")
    public static @NotNull RemoteFileBean from(@NotNull JsonObject jsonObject) {
        return new RemoteFileBean(
                jsonObject.get("fileName").getAsString(),
                jsonObject.get("size").getAsLong(),
                new Date(jsonObject.get("lastUpdateTime").getAsLong()),
                jsonObject.get("md5").getAsString(),
                jsonObject.get("downloadID").getAsLong()
        );
    }

    public @NotNull String fileName() {
        if (fullPath.contains("/"))
            return StringUtils.afterLast(fullPath, "/");
        return fullPath;
    }
}
