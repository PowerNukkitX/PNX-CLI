package cn.powernukkitx.cli.share;

import cn.powernukkitx.cli.util.OSUtils;

import java.io.File;
import java.util.List;
import java.util.Timer;

public interface CLIConstant {
    String version = "0.0.6";
    List<String> authors = List.of("超神的冰凉");
    File userDir = new File(System.getProperty("user.dir"));
    File programDir = new File(OSUtils.getProgramDir());
}
