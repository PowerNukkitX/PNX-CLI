package cn.powernukkitx.cli.util;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CompressUtils {
    private static final int BUFFER = 4096;

    private CompressUtils() {

    }

    public static void uncompressZipFile(File source, File folder) throws IOException {
        if (!folder.exists()) folder.mkdirs();
        else if (!folder.isDirectory()) throw new IllegalArgumentException(folder + " must be directory!");
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(source)));
        unZip(zip, folder.getAbsolutePath());
        zip.close();
    }

    public static void uncompressTGzipFile(File source, File folder) throws IOException {
        if (!folder.exists()) folder.mkdirs();
        else if (!folder.isDirectory()) throw new IllegalArgumentException(folder + " must be directory!");
        TarInputStream tis = new TarInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(source))));
        unTar(tis, folder.getAbsolutePath());
        tis.close();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void unTar(TarInputStream tis, String destFolder) throws IOException {
        BufferedOutputStream dest;

        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
            int count;
            byte[] data = new byte[BUFFER];

            if (entry.isDirectory()) {
                new File(destFolder + "/" + entry.getName()).mkdirs();
                continue;
            } else {
                int di = entry.getName().lastIndexOf('/');
                if (di != -1) {
                    new File(destFolder + "/" + entry.getName().substring(0, di)).mkdirs();
                }
            }

            FileOutputStream fos = new FileOutputStream(destFolder + "/" + entry.getName());
            dest = new BufferedOutputStream(fos);

            while ((count = tis.read(data)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void unZip(ZipInputStream tis, String destFolder) throws IOException {
        BufferedOutputStream dest;

        ZipEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
            int count;
            byte[] data = new byte[BUFFER];

            if (entry.isDirectory()) {
                new File(destFolder + "/" + entry.getName()).mkdirs();
                continue;
            } else {
                int di = entry.getName().lastIndexOf('/');
                if (di != -1) {
                    new File(destFolder + "/" + entry.getName().substring(0, di)).mkdirs();
                }
            }

            FileOutputStream fos = new FileOutputStream(destFolder + "/" + entry.getName());
            dest = new BufferedOutputStream(fos);

            while ((count = tis.read(data)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
        }
    }
}
