package cn.powernukkitx.cli.util;

import cn.powernukkitx.cli.share.CLIConstant;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConfigUtils {
    private static Map<String, String> configMap = new LinkedHashMap<>();
    private final static AtomicBoolean hasChanged = new AtomicBoolean(false);

    public static void init() {
        final File configFile = new File(CLIConstant.programDir, "pnx-cli-config.ini");
        try (final InputStream stream1 = ConfigUtils.class.getClassLoader()
                .getResourceAsStream("lang/" + Locale.getDefault().toLanguageTag().toLowerCase() + "/sampleConfig.ini")) {
            if (!configFile.exists() && stream1 != null) {
                Files.copy(stream1, configFile.toPath());
            } else {
                try (final InputStream stream2 = ConfigUtils.class.getClassLoader()
                        .getResourceAsStream("lang/en-us/sampleConfig.ini")) {
                    if (!configFile.exists() && stream2 != null) {
                        Files.copy(stream2, configFile.toPath());
                    }
                }
            }
        } catch (IOException ignore) {

        }
        try (final InputStream stream = ConfigUtils.class.getClassLoader()
                .getResourceAsStream("config.ini")) {
            if (stream != null) {
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    configMap = INIParser.parseINI(reader);
                }
            }
        } catch (IOException e) {
            configMap = new HashMap<>(0);
        }
        if (configFile.exists() && configFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                configMap.putAll(INIParser.parseINI(reader));
            } catch (IOException ignore) {
                configMap = new HashMap<>(0);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (hasChanged.get()) {
                try (var writer = new BufferedWriter(new FileWriter(configFile))) {
                    INIParser.writeINI(configMap, writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    public static String graalvmVersion() {
        return configMap.get("graalvm.version");
    }

    public static String adoptOpenJDKVersion() {
        return configMap.get("adopt.version");
    }

    public static String startCommand() {
        return configMap.get("start-cmd");
    }

    public static int minRestartTime() {
        return Integer.parseInt(configMap.getOrDefault("min-restart-time", "30000"));
    }

    public static boolean autoRestart() {
        return Boolean.parseBoolean(configMap.getOrDefault("auto-restart", "false"));
    }

    public static String get(String key) {
        return configMap.get(key);
    }

    public static void set(String key, String value) {
        hasChanged.set(true);
        configMap.put(key, value);
    }

    public static String forceLang() {
        return configMap.get("language");
    }

    public static String preferredJVM() {
        return configMap.getOrDefault("preferredJVM", "GraalVM");
    }

    public static boolean displayLaunchCommand() {
        return Boolean.parseBoolean(configMap.getOrDefault("displayLaunchCommand", "false"));
    }

    public static String[] customJVMPaths() {
        return configMap.getOrDefault("jvmPath", "").split(File.pathSeparator);
    }
}
