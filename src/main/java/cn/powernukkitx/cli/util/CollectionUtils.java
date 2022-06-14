package cn.powernukkitx.cli.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CollectionUtils {
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor, int initialAllocSize) {
        ConcurrentHashMap<Object, Boolean> map = new ConcurrentHashMap<>(initialAllocSize);
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        return distinctByKey(keyExtractor, 16);
    }

    public static <T> boolean has(T[] arr, T obj) {
        for (final T e : arr) {
            if (Objects.equals(e, obj)) {
                return true;
            }
        }
        return false;
    }

    public static <T extends JsonElement> T hasAndGet(JsonArray arr, Class<T> clazz, Predicate<T> judgeFunc) {
        for (final var e : arr) {
            if (clazz.isInstance(e) && judgeFunc.test(clazz.cast(e))) {
                return clazz.cast(e);
            }
        }
        return null;
    }

    public static <T extends JsonElement> boolean has(JsonArray arr, Class<T> clazz, Predicate<T> judgeFunc) {
        return hasAndGet(arr, clazz, judgeFunc) != null;
    }
}
