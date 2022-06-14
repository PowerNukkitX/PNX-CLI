package cn.powernukkitx.cli.util;

import java.util.function.Function;

public final class NullUtils {
    private NullUtils() {

    }

    public static boolean Ok(Object o) {
        return o != null;
    }

    public static boolean allOk(Object... objects) {
        for (var each : objects) {
            if (each == null) {
                return false;
            }
        }
        return true;
    }

    public static <T> T Ok(T o, T ifNull) {
        return o == null ? ifNull : o;
    }

    public static <T, R1> R1 Ok(T o, Function<T, R1> f1, R1 fallBack) {
        if (o == null) {
            return fallBack;
        }
        var tmp = f1.apply(o);
        return Ok(tmp, fallBack);
    }

    public static <T, R1, R2> R2 Ok(T o, Function<T, R1> f1, Function<R1, R2> f2, R2 fallBack) {
        if (o == null) {
            return fallBack;
        }
        var tmp = f1.apply(o);
        if (tmp == null) return fallBack;
        var tmp2 = f2.apply(tmp);
        return Ok(tmp2, fallBack);
    }

    public static <T, R1, R2, R3> R3 Ok(T o, Function<T, R1> f1, Function<R1, R2> f2, Function<R2, R3> f3, R3 fallBack) {
        var tmp = Ok(o, f1, f2, null);
        return Ok(tmp, f3, fallBack);
    }
}
