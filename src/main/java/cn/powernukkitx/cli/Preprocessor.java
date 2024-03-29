package cn.powernukkitx.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.util.List;
import java.util.Locale;

@Command(resourceBundle = "cn.powernukkitx.cli.Preprocessor")
public class Preprocessor {
    @Option(names = { "-l", "--lang", "--language" }, descriptionKey = "lang")
    public void setLocale(String locale) {
        Locale.setDefault(Locale.forLanguageTag(locale));
    }

    @Unmatched
    public List<String> remainder;
}
