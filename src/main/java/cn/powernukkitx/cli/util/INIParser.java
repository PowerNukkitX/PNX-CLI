package cn.powernukkitx.cli.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class INIParser {
    public static Map<String, String> parseINI(BufferedReader reader) throws IOException {
        Map<String, String> d = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == ';') {
                d.put(line, null);
                continue;
            }
            String[] t = line.split("=", 2);
            if (t.length < 2) {
                d.put(line, null);
                continue;
            }
            String key = t[0];
            String value = t[1];
            if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
            }
            if (value.isEmpty()) {
                d.put(line, null);
                continue;
            }
            d.put(key, value);
        }
        return d;
    }

    public static void writeINI(Map<String, String> iniMap, BufferedWriter writer) throws IOException {
        for (var each : iniMap.entrySet()) {
            writer.write(each.getKey());
            if (each.getValue() != null) {
                writer.write("=");
                writer.write(each.getValue());
            }
            writer.write("\n");
        }
    }
}
