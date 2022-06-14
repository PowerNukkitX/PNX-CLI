package cn.powernukkitx.cli.data.locator;

import cn.powernukkitx.cli.data.remote.VersionListHelper;
import cn.powernukkitx.cli.share.CLIConstant;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class LibsLocator extends Locator<LibsLocator.LibInfo> {
    @Override
    public List<Location<LibInfo>> locate() {
        try {
            var libEntries = VersionListHelper.listRemoteLibs();
            return libEntries.stream().map(each -> {
                final File file = new File(CLIConstant.userDir, "./libs/" + each.getLibName());
                return new Location<>(file, new LibInfo(each.getLibName(), each.getLastUpdate(), file.exists(), !file.exists() || each.getLastUpdate().getTime() > file.lastModified()));
            }).collect(Collectors.toList());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public static final class LibInfo {
        private String name;
        private Date lastUpdate;
        private boolean exists;
        private boolean needsUpdate;

        public LibInfo(String name, Date lastUpdate, boolean exists, boolean needsUpdate) {
            this.name = name;
            this.lastUpdate = lastUpdate;
            this.exists = exists;
            this.needsUpdate = needsUpdate;
        }

        public String getName() {
            return name;
        }

        public LibInfo setName(String name) {
            this.name = name;
            return this;
        }

        public boolean isExists() {
            return exists;
        }

        public LibInfo setExists(boolean exists) {
            this.exists = exists;
            return this;
        }

        public Date getLastUpdate() {
            return lastUpdate;
        }

        public LibInfo setLastUpdate(Date lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public boolean isNeedsUpdate() {
            return needsUpdate;
        }

        public LibInfo setNeedsUpdate(boolean needsUpdate) {
            this.needsUpdate = needsUpdate;
            return this;
        }
    }
}
