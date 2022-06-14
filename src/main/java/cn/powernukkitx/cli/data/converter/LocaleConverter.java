package cn.powernukkitx.cli.data.converter;

import picocli.CommandLine;

import java.util.Locale;

public class LocaleConverter implements CommandLine.ITypeConverter<Locale> {
    @Override
    public Locale convert(String s) {
        return Locale.forLanguageTag(s);
    }
}
