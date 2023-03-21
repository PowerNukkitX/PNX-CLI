package cn.powernukkitx.cli.data.bean;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RequestIDBean(@NotNull String uuid, long acceptTime) {
    public RequestIDBean(@NotNull UUID uuid) {
        this(uuid.toString(), System.currentTimeMillis());
    }

    @Contract("_ -> new")
    public static @NotNull RequestIDBean from(@NotNull JsonObject jsonObject) {
        if (jsonObject.has("acceptTime")) {
            return new RequestIDBean(
                    jsonObject.get("uuid").getAsString(),
                    jsonObject.get("acceptTime").getAsLong()
            );
        } else {
            return new RequestIDBean(
                    UUID.fromString(jsonObject.get("uuid").getAsString())
            );
        }
    }
}
