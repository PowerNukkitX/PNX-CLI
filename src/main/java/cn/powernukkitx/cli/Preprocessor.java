package cn.powernukkitx.cli;

import cn.powernukkitx.cli.util.CheckUpdateUtil;
import picocli.CommandLine.*;

import java.util.List;
import java.util.Locale;

@Command(resourceBundle = "cn.powernukkitx.cli.Preprocessor")
public class Preprocessor {
    @Option(names = { "-l", "--lang", "--language" }, descriptionKey = "lang")
    public void setLocale(String locale) {
        Locale.setDefault(Locale.forLanguageTag(locale));
    }

    @Option(names = {"-u", "--update"}, descriptionKey = "update", negatable = true, defaultValue = "true")
    public void setCheckUpdate(boolean update) {
        if (update) {
            CheckUpdateUtil.scheduleCheckUpdate(Main.getTimer());
        }
    }

    @Unmatched
    public List<String> remainder;
}
