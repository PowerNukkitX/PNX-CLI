package cn.powernukkitx.cli.data.remote;

import cn.powernukkitx.cli.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionListHelper {
    public static final String OSS = "https://pnx-assets.oss-cn-hongkong.aliyuncs.com";
    public static final Pattern keyPattern = Pattern.compile("(?<=<Key>)(.*?)(?=</Key>)");
    public static final Pattern timePattern = Pattern.compile("(?<=<LastModified>)([0-9TZ:.-]*)(?=</LastModified>)");
    public static final SimpleDateFormat utcTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final SimpleDateFormat commonTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final WeakHashMap<String, String> cache = new WeakHashMap<>(3);

    static {
        utcTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        commonTimeFormat.setTimeZone(TimeZone.getDefault());
    }

    public static List<VersionEntry> listRemoteVersions(final String category) throws IOException, InterruptedException {
        if (cache.containsKey(category)) {
            return exactKeys(cache.get(category));
        } else {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder(URI.create(OSS + "?" +
                    "list-type=2" + "&" +
                    "prefix=" + category + "/&" +
                    "max-keys=30" + "&" +
                    "delimiter=/")).GET().build();
            final var result = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            cache.put(category, result);
            return exactKeys(result);
        }
    }

    public static List<LibEntry> listRemoteLibs() throws IOException, InterruptedException {
        if (cache.containsKey("libs")) {
            return exactLibs(cache.get("libs"));
        } else {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder(URI.create(OSS + "?" +
                    "list-type=2" + "&" +
                    "prefix=libs" + "/&" +
                    "max-keys=100" + "&" +
                    "delimiter=/")).GET().build();
            final var result = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            cache.put("libs", result);
            return exactLibs(result);
        }
    }

    private static List<VersionEntry> exactKeys(final String xml) {
        final Matcher keyMatcher = keyPattern.matcher(xml);
        final List<VersionEntry> out = new ArrayList<>(20);
        while (keyMatcher.find()) {
            final String[] tmp = keyMatcher.group(0).split("-");
            out.add(new VersionEntry().setBranch(StringUtils.afterFirst(tmp[0], "/")).setCommit(tmp[1]));
        }

        final Matcher timeMatcher = timePattern.matcher(xml);
        int i = 0;
        while (timeMatcher.find()) {
            try {
                out.get(i++).setTime(utcTimeFormat.parse(timeMatcher.group(0)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (out.get(0).getCommit().endsWith("/")) {
            out.remove(0);
        }

        out.sort((a, b) -> b.time.compareTo(a.time));
        return out;
    }

    private static List<LibEntry> exactLibs(final String xml) {
        final Matcher keyMatcher = keyPattern.matcher(xml);
        final List<LibEntry> out = new ArrayList<>(80);
        while (keyMatcher.find()) {
            final String name = keyMatcher.group(0);
            out.add(new LibEntry().setLibName(StringUtils.afterFirst(name, "/")));
        }

        final Matcher timeMatcher = timePattern.matcher(xml);
        int i = 0;
        while (timeMatcher.find()) {
            try {
                out.get(i++).setLastUpdate(utcTimeFormat.parse(timeMatcher.group(0)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if ("".equals(out.get(0).getLibName())) {
            out.remove(0);
        }

        return out;
    }

    public static final class VersionEntry {
        private String branch;
        private String commit;
        private Date time;

        public String getBranch() {
            return branch;
        }

        public VersionEntry setBranch(String branch) {
            this.branch = branch;
            return this;
        }

        public String getCommit() {
            return commit;
        }

        public VersionEntry setCommit(String commit) {
            this.commit = commit;
            return this;
        }

        public String getTime() {
            return commonTimeFormat.format(time);
        }

        public VersionEntry setTime(Date time) {
            this.time = time;
            return this;
        }

        @Override
        public String toString() {
            return "{" +
                    "branch='" + branch + '\'' +
                    ", commit='" + commit + '\'' +
                    ", time='" + time + '\'' +
                    '}';
        }
    }

    public static final class LibEntry {
        private String libName;
        private Date lastUpdate;

        public String getLibName() {
            return libName;
        }

        public LibEntry setLibName(String libName) {
            this.libName = libName;
            return this;
        }

        public Date getLastUpdate() {
            return lastUpdate;
        }

        public LibEntry setLastUpdate(Date lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }
    }
}
