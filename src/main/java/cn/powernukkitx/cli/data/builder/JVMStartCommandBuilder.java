package cn.powernukkitx.cli.data.builder;

import cn.powernukkitx.cli.util.StringUtils;
import com.sun.management.OperatingSystemMXBean;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.*;

public final class JVMStartCommandBuilder {
    private File jvmExecutable;
    private Map<String, String> properties = new LinkedHashMap<>(); // -Dxxx=xxx
    private Map<String, Object> xxOptions = new LinkedHashMap<>(); // -XX:
    private Map<String, Object> xOptions = new LinkedHashMap<>(); // -X:
    private List<String> modulePath = new ArrayList<>(2);
    private List<String> upgradeModulePath = new ArrayList<>(2);
    private List<String> classPath = new ArrayList<>(2);
    private Map<String, String> addOpens = new LinkedHashMap<>();
    private List<String> otherArgs = new ArrayList<>(0);
    private String startTarget;

    public JVMStartCommandBuilder useMaxPhysicalMemory() {
        var osMxb = (OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean();
        addXOption("mx" + (osMxb.getTotalMemorySize() / 1024 / 1024) + "m");
        return this;
    }

    public File getJvmExecutable() {
        return jvmExecutable;
    }

    public JVMStartCommandBuilder setJvmExecutable(File jvmExecutable) {
        this.jvmExecutable = jvmExecutable;
        return this;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public JVMStartCommandBuilder addProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }

    public JVMStartCommandBuilder setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, Object> getXxOptions() {
        return xxOptions;
    }

    public JVMStartCommandBuilder addXxOption(String key, Object value) {
        xxOptions.put(key, value);
        return this;
    }

    public JVMStartCommandBuilder addXxOption(String key) {
        xxOptions.put(key, null);
        return this;
    }

    public JVMStartCommandBuilder setXxOptions(Map<String, Object> xxOptions) {
        this.xxOptions = xxOptions;
        return this;
    }

    public <T> T getXxOption(String key, Class<T> clazz) {
        var tmp = xxOptions.get(key);
        if (clazz.isInstance(tmp)) {
            return clazz.cast(tmp);
        }
        return null;
    }

    public Map<String, Object> getXOptions() {
        return xOptions;
    }

    public JVMStartCommandBuilder addXOption(String key, Object value) {
        xOptions.put(key, value);
        return this;
    }

    public JVMStartCommandBuilder addXOption(String key) {
        xOptions.put(key, null);
        return this;
    }

    public JVMStartCommandBuilder setXOptions(Map<String, Object> xxOptions) {
        this.xOptions = xxOptions;
        return this;
    }

    public <T> T getXOption(String key, Class<T> clazz) {
        var tmp = xOptions.get(key);
        if (clazz.isInstance(tmp)) {
            return clazz.cast(tmp);
        }
        return null;
    }

    public List<String> getOtherArgs() {
        return otherArgs;
    }

    public JVMStartCommandBuilder setOtherArgs(List<String> otherArgs) {
        this.otherArgs = otherArgs;
        return this;
    }

    public JVMStartCommandBuilder setOtherArgs(String... args) {
        this.otherArgs = Arrays.asList(args);
        return this;
    }

    public JVMStartCommandBuilder addOtherArgs(String... args) {
        otherArgs.addAll(List.of(args));
        return this;
    }

    public List<String> getModulePath() {
        return modulePath;
    }

    public JVMStartCommandBuilder setModulePath(List<String> modulePath) {
        this.modulePath = modulePath;
        return this;
    }

    public JVMStartCommandBuilder setModulePath(String... modulePath) {
        this.modulePath = Arrays.asList(modulePath);
        return this;
    }

    public JVMStartCommandBuilder addModulePath(String... modulePath) {
        this.modulePath.addAll(List.of(modulePath));
        return this;
    }

    public String getStartTarget() {
        return startTarget;
    }

    public JVMStartCommandBuilder setStartTarget(String startTarget) {
        this.startTarget = startTarget;
        return this;
    }

    public List<String> getUpgradeModulePath() {
        return upgradeModulePath;
    }

    public JVMStartCommandBuilder setUpgradeModulePath(List<String> upgradeModulePath) {
        this.upgradeModulePath = upgradeModulePath;
        return this;
    }

    public JVMStartCommandBuilder setUpgradeModulePath(String... upgradeModulePath) {
        this.upgradeModulePath = Arrays.asList(upgradeModulePath);
        return this;
    }

    public JVMStartCommandBuilder addUpgradeModuleArgs(String... upgradeModulePath) {
        this.upgradeModulePath.addAll(List.of(upgradeModulePath));
        return this;
    }

    public List<String> getClassPath() {
        return classPath;
    }

    public JVMStartCommandBuilder setClassPath(List<String> classPath) {
        this.classPath = classPath;
        return this;
    }

    public JVMStartCommandBuilder setClassPath(String... classPath) {
        this.classPath = Arrays.asList(classPath);
        return this;
    }

    public JVMStartCommandBuilder addClassPath(String... classPath) {
        this.classPath.addAll(List.of(classPath));
        return this;
    }

    public Map<String, String> getAddOpens() {
        return addOpens;
    }

    public JVMStartCommandBuilder setAddOpens(Map<String, String> addOpens) {
        this.addOpens = addOpens;
        return this;
    }

    public JVMStartCommandBuilder addAddOpen(String key, String value) {
        addOpens.put(key, value);
        return this;
    }

    public JVMStartCommandBuilder addAddOpen(String key) {
        addOpens.put(key, "ALL-UNNAMED");
        return this;
    }

    public String build() {
        var sb = new StringBuilder();
        sb.append(StringUtils.tryWrapQuotation(jvmExecutable.getAbsolutePath())).append(" ");
        for (var entry : properties.entrySet()) {
            sb.append(StringUtils.tryWrapQuotation("-D" + entry.getKey() + "=" + entry.getValue())).append(" ");
        }
        for (var iterator = otherArgs.iterator(); iterator.hasNext(); ) {
            var each = iterator.next();
            if (each.startsWith("-D")) {
                sb.append(StringUtils.tryWrapQuotation(each)).append(" ");
                iterator.remove();
            }
        }
        for (var entry : xxOptions.entrySet()) {
            sb.append("-XX:");
            if (entry.getValue() != null) {
                if (entry.getValue() instanceof Boolean bool) {
                    sb.append(bool ? "+" : "-").append(entry.getKey());
                } else {
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                }
            } else {
                sb.append(entry.getKey());
            }
            sb.append(" ");
        }
        for (var iterator = otherArgs.iterator(); iterator.hasNext(); ) {
            var each = iterator.next();
            if (each.startsWith("-XX:")) {
                sb.append(each).append(" ");
                iterator.remove();
            }
        }
        for (var entry : xOptions.entrySet()) {
            sb.append("-X").append(entry.getKey());
            if (entry.getValue() != null) {
                sb.append(entry.getValue());
            }
            sb.append(" ");
        }
        for (var iterator = otherArgs.iterator(); iterator.hasNext(); ) {
            var each = iterator.next();
            if (each.startsWith("-X")) {
                sb.append(each).append(" ");
                iterator.remove();
            }
        }
        sb.append(StringUtils.tryWrapQuotation("--module-path=" + String.join(File.pathSeparator, modulePath) + File.pathSeparator)).append(" ");
        sb.append(StringUtils.tryWrapQuotation("--upgrade-module-path=" + String.join(File.pathSeparator, upgradeModulePath) + File.pathSeparator)).append(" ");
        for (var entry : addOpens.entrySet()) {
            sb.append("--add-opens ").append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
        }
        sb.append("-cp ").append(StringUtils.tryWrapQuotation(String.join(File.pathSeparator, classPath))).append(" ");
        sb.append(startTarget);
        for (var arg : otherArgs) {
            sb.append(" ").append(StringUtils.tryWrapQuotation(arg));
        }
        return sb.toString();
    }
}
