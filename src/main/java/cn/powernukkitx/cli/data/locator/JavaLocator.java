package cn.powernukkitx.cli.data.locator;

import cn.powernukkitx.cli.share.CLIConstant;
import cn.powernukkitx.cli.util.CollectionUtils;
import cn.powernukkitx.cli.util.ConfigUtils;
import cn.powernukkitx.cli.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JavaLocator extends Locator<JavaLocator.JavaInfo> {
    private final String version;
    private final boolean sort4GraalVM;

    public JavaLocator(String version) {
        this.version = version;
        this.sort4GraalVM = false;
    }

    public JavaLocator(String version, boolean sort4GraalVM) {
        this.version = version;
        this.sort4GraalVM = sort4GraalVM;
    }

    @Override
    public List<Location<JavaInfo>> locate() {
        final List<Location<JavaInfo>> javaExecutableList = new ArrayList<>();
        final File localJavaDir = new File(CLIConstant.programDir, "java");
        final List<File> binDirs = new ArrayList<>();
        { // 探测当前运行环境
            var javaHome = System.getProperty("java.home");
            if (javaHome != null)
                binDirs.add(new File(javaHome));
        }
        { // 当前文件夹下缓存探测
            if (localJavaDir.exists()) {
                final File[] files = localJavaDir.listFiles();
                if (files != null) {
                    for (File each : files) {
                        File binDir = new File(each, "bin");
                        if (binDir.exists() && isJavaDir(binDir)) {
                            binDirs.add(binDir);
                        }
                    }
                }
            }
        }
        { // JAVA*(_HOME)环境变量探测
            for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
                final String key = entry.getKey();
                if (key.contains("JAVA") || key.contains("java") || key.contains("Java") || key.contains("GRAAL")
                        || key.contains("graal") || key.contains("Graal") || key.contains("JDK") || key.contains("jdk")
                        || key.contains("JRE") || key.contains("jre")) {
                    final File binDir = new File(entry.getValue());
                    if (binDir.exists() && binDir.isDirectory()) {
                        if (isJavaDir(binDir)) {
                            binDirs.add(binDir);
                        } else {
                            final File innerBinDir = new File(binDir, "bin");
                            if (isJavaDir(innerBinDir)) {
                                binDirs.add(innerBinDir);
                            }
                        }
                    }
                }
            }
        }
        { // where/which探测
            try {
                Process process = new ProcessBuilder().command("where", "java").redirectErrorStream(true).start();
                testSystemJava(binDirs, process);
            } catch (IOException | InterruptedException ignore) {

            }
            try {
                Process process = new ProcessBuilder().command("which", "java").redirectErrorStream(true).start();
                testSystemJava(binDirs, process);
            } catch (IOException | InterruptedException ignore) {

            }
        }
        { // 用户自定义探测
            for (var each : ConfigUtils.customJVMPaths()) {
                if (each == null || "".equals(each)) {
                    continue;
                }
                final File binDir = new File(CLIConstant.userDir, each);
                if (binDir.exists() && binDir.isDirectory()) {
                    if (isJavaDir(binDir)) {
                        binDirs.add(binDir);
                    } else {
                        final File innerBinDir = new File(binDir, "bin");
                        if (isJavaDir(innerBinDir)) {
                            binDirs.add(innerBinDir);
                        }
                    }
                }
            }
        }
        for (final File binDir : binDirs) {
            Optional<JavaInfo> jv = getJavaVersion(binDir);
            if (jv.isPresent()) {
                JavaInfo v = jv.get();
                if (version != null && !greaterOrEqual(version, v.getMajorVersion())) {
                    continue;
                }
                javaExecutableList.add(new Location<>(new File(binDir, "java" + Locator.platformSuffix()), v));
            } else if (version == null) {
                javaExecutableList.add(new Location<>(new File(binDir, "java" + Locator.platformSuffix()),
                        new JavaInfo("Unknown", "Unknown", "Unknown")));
            }
        }
        // 去重、排序并返回
        final List<Location<JavaInfo>> out = javaExecutableList.stream()
                .filter(CollectionUtils.distinctByKey(each -> each.getFile().getAbsolutePath()))
                .sorted(Comparator.comparing(a -> a.getInfo().getMajorVersion()))
                .collect(Collectors.toList());
        if (sort4GraalVM) {
            out.sort((a, b) -> {
                if (a.equals(b)) return 0;
                final boolean a1 = a.getInfo().getVendor().contains(ConfigUtils.preferredJVM());
                final boolean b1 = b.getInfo().getVendor().contains(ConfigUtils.preferredJVM());
                if (a1 && !b1) {
                    return -1;
                } else if (!a1 && b1) {
                    return 1;
                } else {
                    return 0;
                }
            });
        }
        return out;
    }

    private boolean greaterOrEqual(String targetVersion, String givenVersion) {
        try {
            var target = Integer.parseInt(targetVersion);
            var given = Integer.parseInt(givenVersion);
            return given >= target;
        } catch (Exception ignore) {
            return targetVersion.equals(givenVersion);
        }
    }

    private void testSystemJava(List<File> binDirs, Process process) throws InterruptedException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        process.waitFor(1000, TimeUnit.MILLISECONDS);
        String s;
        while ((s = reader.readLine()) != null) {
            if (s.contains("java")) {
                binDirs.add(new File(s).getParentFile());
            }
        }
        process.destroy();
    }

    private boolean isJavaDir(File binDir) {
        if (!binDir.exists()) return false;
        final File javaExecutable = new File(binDir, "java" + Locator.platformSuffix());
        return javaExecutable.exists();
    }

    private Optional<JavaInfo> getJavaVersion(File binDir) {
        final File javaExecutable = new File(binDir, "java" + Locator.platformSuffix());
        if (!javaExecutable.canExecute()) {
            boolean r = javaExecutable.setExecutable(true);
            if (!r) {
                return Optional.empty();
            }
        }
        try {
            Process process = new ProcessBuilder().command(StringUtils.tryWrapQuotation(javaExecutable.getAbsolutePath()), "-version")
                    .redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor(1000, TimeUnit.MILLISECONDS);
            String s;
            String fullVersion = null;
            String majorVersion = null;
            String vendor = null;
            while ((s = reader.readLine()) != null) {
                if (s.contains("version")) {
                    String[] t = s.split("\"");
                    if (t.length >= 2) {
                        fullVersion = t[1];
                        String[] tmp = fullVersion.split("\\.");
                        majorVersion = tmp[0];
                        if ("1".equals(tmp[0])) {
                            majorVersion = tmp[1];
                        }
                        majorVersion = majorVersion.replace("-internal", "");
                    }
                } else if (s.contains("Server VM") || s.contains("Runtime")) {
                    vendor = StringUtils.beforeLast(s, " (build");
                }
            }
            process.destroy();
            if (majorVersion != null && vendor != null)
                return Optional.of(new JavaInfo(majorVersion, fullVersion, vendor));
        } catch (IOException | InterruptedException ignore) {

        }
        return getJavaVersionByJVMTI(binDir);
    }

    private Optional<JavaInfo> getJavaVersionByJVMTI(File binDir) {
        var includeDir = new File(binDir.getParentFile(), "include");
        if (!includeDir.exists()) {
            return Optional.empty();
        }
        var jvmtiFile = new File(includeDir, "jvmti.h");
        if (!jvmtiFile.exists()) {
            return Optional.empty();
        }
        try {
            var lines = Files.readAllLines(jvmtiFile.toPath());
            for (var line : lines) {
                var tmp = line.trim();
                if (tmp.startsWith("JVMTI_VERSION = 0x30000000 + (")) {
                    var i = tmp.indexOf(" * 0x10000");
                    var majorVersion = tmp.substring(30, i);
                    var fullVersion = majorVersion + ".0.0";
                    return Optional.of(new JavaInfo(majorVersion, fullVersion, "Unknown"));
                }
            }
        } catch (Exception ignore) {

        }
        return Optional.empty();
    }

    public static final class JavaInfo {
        private String majorVersion;
        private String fullVersion;
        private String vendor;

        public JavaInfo(String majorVersion, String fullVersion, String vendor) {
            this.majorVersion = majorVersion;
            this.fullVersion = fullVersion;
            this.vendor = vendor;
        }

        public String getMajorVersion() {
            return majorVersion;
        }

        public JavaInfo setMajorVersion(String majorVersion) {
            this.majorVersion = majorVersion;
            return this;
        }

        public String getFullVersion() {
            return fullVersion;
        }

        public JavaInfo setFullVersion(String fullVersion) {
            this.fullVersion = fullVersion;
            return this;
        }

        public String getVendor() {
            return vendor;
        }

        public JavaInfo setVendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        @Override
        public String toString() {
            return "JavaInfo{" +
                    "majorVersion='" + majorVersion + '\'' +
                    ", fullVersion='" + fullVersion + '\'' +
                    ", vendor='" + vendor + '\'' +
                    '}';
        }
    }
}
