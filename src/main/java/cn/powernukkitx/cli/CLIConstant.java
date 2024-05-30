package cn.powernukkitx.cli;

import cn.powernukkitx.cli.util.OSUtils;

import java.io.File;
import java.util.List;

public interface CLIConstant {
    String version = "0.2.4";
    List<String> authors = List.of("超神的冰凉", "CoolLoong");
    File userDir = new File(System.getProperty("user.dir"));
    File programDir = new File(OSUtils.getProgramDir());
}
