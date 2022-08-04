package cn.powernukkitx.cli.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public final class ComponentsHelper {
    public static WeakReference<List<ComponentEntry>> cache = new WeakReference<>(null);

    private static JsonArray getComponents() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create("https://assets.powernukkitx.cn/components.json")).GET().build();
        try {
            var result = JsonParser.parseString(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
            if (result.isJsonArray()) {
                return result.getAsJsonArray();
            }
            return new JsonArray();
        } catch (IOException | InterruptedException e) {
            return new JsonArray();
        }
    }

    public static List<ComponentEntry> listRemoteComponents() {
        if (cache.get() != null) {
            return cache.get();
        } else {
            var data = getComponents();
            final List<ComponentEntry> out = new ArrayList<>(1);
            for (var each : data) {
                var obj = each.getAsJsonObject();
                var descriptionObj = obj.get("description").getAsJsonObject();
                String description = "Unknown";
                var languageId = Locale.getDefault().toLanguageTag().toLowerCase();
                var shortLanguageId = languageId.substring(0, 2);
                if (descriptionObj.has(languageId)) {
                    description = descriptionObj.get(languageId).getAsString();
                } else if (descriptionObj.has(shortLanguageId)) {
                    description = descriptionObj.get(shortLanguageId).getAsString();
                } else {
                    description = descriptionObj.get("en").getAsString();
                }
                var componentEntry = new ComponentEntry()
                        .setName(obj.get("name").getAsString()).setDescription(description).setVersion(obj.get("version").getAsString());
                var clearJsonArray = obj.get("clear").getAsJsonArray();
                var clearFiles = new String[clearJsonArray.size()];
                for (int i = 0, len = clearFiles.length; i < len; i++) {
                    clearFiles[i] = clearJsonArray.get(i).getAsString();
                }
                componentEntry.setClearFiles(clearFiles);
                var fileObjs = obj.get("files").getAsJsonArray();
                final ComponentFile[] componentFiles = new ComponentFile[fileObjs.size()];
                for (int i = 0, len = componentFiles.length; i < len; i++) {
                    var fileObj = fileObjs.get(i).getAsJsonObject();
                    componentFiles[i] = new ComponentFile(fileObj.get("into").getAsString(), fileObj.get("url").getAsString());
                }
                componentEntry.setComponentFiles(componentFiles);
                cache = new WeakReference<>(out);
                out.add(componentEntry);
            }
            return out;
        }
    }

    public static final class ComponentEntry {
        private String name;
        private String version;
        private String description;
        private String[] clearFiles;
        private ComponentFile[] componentFiles;

        public ComponentEntry() {

        }

        public ComponentEntry(String name, String version, String description, String[] clearFiles, ComponentFile[] componentFiles) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.clearFiles = clearFiles;
            this.componentFiles = componentFiles;
        }

        public String getName() {
            return name;
        }

        public ComponentEntry setName(String name) {
            this.name = name;
            return this;
        }

        public String getVersion() {
            return version;
        }

        public ComponentEntry setVersion(String version) {
            this.version = version;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public ComponentEntry setDescription(String description) {
            this.description = description;
            return this;
        }

        public ComponentFile[] getComponentFiles() {
            return componentFiles;
        }

        public ComponentEntry setComponentFiles(ComponentFile[] componentFiles) {
            this.componentFiles = componentFiles;
            return this;
        }

        public ComponentEntry setClearFiles(String[] clearFiles) {
            this.clearFiles = clearFiles;
            return this;
        }

        public String[] getClearFiles() {
            return clearFiles;
        }

        @Override
        public String toString() {
            return "ComponentEntry{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", description='" + description + '\'' +
                    ", componentFiles=" + Arrays.toString(componentFiles) +
                    '}';
        }
    }

    public static final class ComponentFile {
        private String fileName;
        private String downloadPath;

        public ComponentFile() {

        }

        public ComponentFile(String fileName, String downloadPath) {
            this.fileName = fileName;
            this.downloadPath = downloadPath;
        }

        public String getFileName() {
            return fileName;
        }

        public ComponentFile setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public ComponentFile setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
            return this;
        }

        @Override
        public String toString() {
            return "ComponentFile{" +
                    "fileName='" + fileName + '\'' +
                    ", downloadPath='" + downloadPath + '\'' +
                    '}';
        }
    }
}
