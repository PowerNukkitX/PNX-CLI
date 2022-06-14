package cn.powernukkitx.cli.data.locator;

import cn.powernukkitx.cli.util.EnumOS;
import cn.powernukkitx.cli.util.OSUtils;

import java.io.File;
import java.util.List;

public abstract class Locator<T> {
    public abstract List<Location<T>> locate();

    public static String platformSuffix() {
        if (OSUtils.getOS() == EnumOS.WINDOWS) {
            return ".exe";
        } else {
            return "";
        }
    }

    public static String platformSplitter() {
        return File.separator;
    }
}