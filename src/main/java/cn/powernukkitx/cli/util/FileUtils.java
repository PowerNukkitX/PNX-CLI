package cn.powernukkitx.cli.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

public final class FileUtils {
    private FileUtils() {

    }

    public static void deleteDir(File dir) {
        try (var walk = Files.walk(dir.toPath())) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignore) {

                        }
                    });
        } catch (IOException ignore) {

        }
    }

    public static @NotNull String getMD5(@NotNull File file) throws IOException {
        try (var bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
            var MD5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[4096];
            int length;
            while ((length = bufferedInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return bytesToHex(MD5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true, value = "_ -> new")
    public static @NotNull String bytesToHex(byte @NotNull [] bytes) {
        var result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
