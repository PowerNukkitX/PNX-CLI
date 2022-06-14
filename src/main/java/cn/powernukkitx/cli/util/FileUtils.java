package cn.powernukkitx.cli.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
}
