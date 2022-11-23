package cn.powernukkitx.cli.util;

import cn.powernukkitx.cli.share.CLIConstant;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static cn.powernukkitx.cli.util.NullUtils.allOk;

public final class OSUtils {
    private OSUtils() {

    }

    public static EnumOS getOS() {
        var os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return EnumOS.WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return EnumOS.MACOS;
        } else if (os.contains("nux")) {
            return EnumOS.LINUX;
        } else {
            return EnumOS.UNKNOWN;
        }
    }

    public static String getProgramPath() {
        var path = OSUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (getOS() == EnumOS.WINDOWS) {
            path = path.substring(1);
        }
        return path;
    }

    public static String getProgramDir() {
        var tmp = Path.of(getProgramPath());
        return tmp.getParent().toString();
    }

    public static String getProgramName() {
        var tmp = Path.of(getProgramPath());
        var name = tmp.getFileName().toString();
        if (name.contains(".")) {
            return StringUtils.beforeLast(name, ".");
        }
        return name;
    }

    public static boolean addWindowsPath(String path) throws IOException, InterruptedException {
        return editWindowsPath("APPEND", path);
    }

    public static boolean removeWindowsPath(String path) throws IOException, InterruptedException {
        return !editWindowsPath("REMOVE", path);
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean editWindowsPath(String op, String path) throws IOException, InterruptedException {
        try (var pathedStream = OSUtils.class.getClassLoader().getResourceAsStream("bin/pathed.exe");
             var GSTStream = OSUtils.class.getClassLoader().getResourceAsStream("bin/GSharpTools.dll");
             var log4netStream = OSUtils.class.getClassLoader().getResourceAsStream("bin/log4net.dll")) {
            if (allOk(pathedStream, GSTStream, log4netStream)) {
                var pathedTmp = File.createTempFile("pathed", ".exe");
                var GSTTmp = new File(System.getProperty("java.io.tmpdir"), "GSharpTools.dll");
                var log4netTmp = new File(System.getProperty("java.io.tmpdir"), "log4net.dll");
                pathedTmp.deleteOnExit();
                GSTTmp.deleteOnExit();
                log4netTmp.deleteOnExit();
                var resultFile = File.createTempFile("pnxBuffer", null);
                var fos = new FileOutputStream(pathedTmp);
                pathedStream.transferTo(fos);
                fos.close();
                fos = new FileOutputStream(GSTTmp);
                GSTStream.transferTo(fos);
                fos.close();
                fos = new FileOutputStream(log4netTmp);
                log4netStream.transferTo(fos);
                fos.close();
                var process = new ProcessBuilder("\"" + pathedTmp.getAbsolutePath() + "\"", "/" + op, "\"" + path + "\"", "/USER")
                        .redirectOutput(resultFile).redirectError(resultFile).start();
                process.waitFor(30, TimeUnit.SECONDS);
                var result = Files.readString(resultFile.toPath(), Charset.defaultCharset());
                return result.contains(path);
            }
            return false;
        }
    }

    public static boolean setWindowsEnv(EnvEntry... entries) throws IOException, InterruptedException {
        var batBuilder = new StringBuilder("""
                @echo off
                                
                :: BatchGotAdmin
                :-------------------------------------
                REM  -->  Check for permissions
                >nul 2>&1 "%SYSTEMROOT%\\system32\\cacls.exe" "%SYSTEMROOT%\\system32\\config\\system"
                                
                REM --> If error flag set, we do not have admin.
                if '%errorlevel%' NEQ '0' (
                    echo Requesting administrative privileges...
                    goto UACPrompt
                ) else ( goto gotAdmin )
                                
                :UACPrompt
                    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\\getadmin.vbs"
                    echo UAC.ShellExecute "%~s0", "", "", "runas", 1 >> "%temp%\\getadmin.vbs"
                                
                    "%temp%\\getadmin.vbs"
                    exit /B
                                
                :gotAdmin
                    if exist "%temp%\\getadmin.vbs" ( del "%temp%\\getadmin.vbs" )
                    pushd "%CD%"
                    CD /D "%~dp0"
                :--------------------------------------
                                
                @echo on
                """);
        for (var each : entries) {
            batBuilder.append("setx \"").append(each.key()).append("\" \"").append(each.value()).append("\"\n");
        }
        batBuilder.append("echo pnxEnvOK\n");
        var batFile = File.createTempFile("pnxBat", ".bat");
        batFile.deleteOnExit();
        Files.writeString(batFile.toPath(), batBuilder.toString());
        System.out.println(batBuilder);
        var resultFile = File.createTempFile("pnxBuffer", null);
        resultFile.deleteOnExit();
        var process = new ProcessBuilder("\"" + batFile.getAbsolutePath() + "\"")
                .redirectOutput(resultFile).start();
        process.waitFor(30, TimeUnit.SECONDS);
        var result = Files.readString(resultFile.toPath());
        System.out.println(result);
        return result.contains("pnxEnvOK");
    }

    public static Locale getWindowsLocale() {
        try {
            var cmd = """
                    reg query "hklm\\system\\controlset001\\control\\nls\\language" /v Installlanguage
                    """;
            var cmdFile = File.createTempFile("pnx-cli-locate", ".cmd");
            cmdFile.deleteOnExit();
            //noinspection ResultOfMethodCallIgnored
            cmdFile.getParentFile().mkdirs();
            if (!cmdFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                cmdFile.createNewFile();
                try (var writer = new FileWriter(cmdFile)) {
                    writer.write(cmd);
                }
            }
            var process = new ProcessBuilder().command(cmdFile.getAbsolutePath())
                    .redirectErrorStream(true).start();
            process.waitFor(2500, TimeUnit.MILLISECONDS);
            try(var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                var s = "";
                while ((s = reader.readLine()) != null) {
                    if (s.contains("0804")) {
                        return Locale.forLanguageTag("zh-cn");
                    } else if (s.contains("0409 ")) {
                        return Locale.forLanguageTag("en-us");
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            //ignore
        }
        return null;
    }

    public static void registerWindowsUrlScheme(String scheme, String appPath) {
        appPath = appPath.replace("\\", "\\\\");
        var regFile = """
                Windows Registry Editor Version 5.00
                [HKEY_CLASSES_ROOT\\%scheme%]
                "URL Protocol"="%appPath%"
                @="%scheme%"
                [HKEY_CLASSES_ROOT\\%scheme%\\DefaultIcon]
                @="%appPath%,1"
                [HKEY_CLASSES_ROOT\\%scheme%\\shell]
                [HKEY_CLASSES_ROOT\\%scheme%\\shell\\open]
                [HKEY_CLASSES_ROOT\\%scheme%\\shell\\open\\command]
                @="\\"%appPath%\\" \\"%1\\""
                """.replace("%scheme%", scheme).replace("%appPath%", appPath);
        var file = new File(CLIConstant.programDir, "scheme.reg");
        try {
            Files.writeString(file.toPath(), regFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
