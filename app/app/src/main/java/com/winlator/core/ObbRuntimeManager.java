package com.winlator.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.preference.PreferenceManager;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public abstract class ObbRuntimeManager {
    private static final String PREF_WINE_ADDONS_STAMP = "obb_wine_addons_stamp";

    public static File findMainObb(Context context) {
        String packageName = context.getPackageName();
        int versionCode = AppUtils.getVersionCode(context);

        File[] searchDirs = new File[]{
            context.getObbDir(),
            new File(Environment.getExternalStorageDirectory(), "Android/obb/"+packageName)
        };

        for (File dir : searchDirs) {
            File result = findMainObb(dir, packageName, versionCode);
            if (result != null) return result;
        }
        return null;
    }

    private static File findMainObb(File dir, String packageName, int versionCode) {
        if (dir == null) return null;

        File exactFile = new File(dir, "main."+versionCode+"."+packageName+".obb");
        if (exactFile.isFile()) return exactFile;

        File[] candidates = dir.listFiles((file, name) -> name.startsWith("main.") && name.endsWith("."+packageName+".obb"));
        return candidates != null && candidates.length > 0 ? candidates[0] : null;
    }

    public static boolean hasMainObb(Context context) {
        return findMainObb(context) != null;
    }

    public static String readTextEntry(Context context, String entryName) {
        byte[] data = readEntry(context, entryName);
        return data != null ? new String(data, StandardCharsets.UTF_8) : null;
    }

    public static byte[] readEntry(Context context, String entryName) {
        File obbFile = findMainObb(context);
        return obbFile != null ? ZipUtils.read(obbFile, normalizeEntry(entryName)) : null;
    }

    public static boolean extractEntry(Context context, String entryName, File destination) {
        File obbFile = findMainObb(context);
        if (obbFile == null) return false;

        entryName = normalizeEntry(entryName);
        try (ZipFile zipFile = new ZipFile(obbFile)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.isUnixSymlink() || !entry.getName().equals(entryName)) continue;

                File parent = destination.getParentFile();
                if (parent != null && !parent.isDirectory()) parent.mkdirs();

                try (InputStream inStream = zipFile.getInputStream(entry);
                     BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE)) {
                    if (!StreamUtils.copy(inStream, outStream)) return false;
                }

                FileUtils.chmod(destination, 0771);
                return destination.isFile();
            }
        }
        catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean extractDirectory(Context context, String prefix, File destination) {
        File obbFile = findMainObb(context);
        if (obbFile == null) return false;

        prefix = normalizePrefix(prefix);
        boolean extracted = false;

        try (ZipFile zipFile = new ZipFile(obbFile)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix)) continue;

                String relativePath = name.substring(prefix.length());
                if (relativePath.isEmpty()) continue;

                File file = new File(destination, relativePath);
                if (entry.isDirectory()) {
                    if (!file.isDirectory()) file.mkdirs();
                    extracted = true;
                    continue;
                }
                if (entry.isUnixSymlink()) continue;

                File parent = file.getParentFile();
                if (parent != null && !parent.isDirectory()) parent.mkdirs();

                try (InputStream inStream = zipFile.getInputStream(entry);
                     BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file), StreamUtils.BUFFER_SIZE)) {
                    if (!StreamUtils.copy(inStream, outStream)) return false;
                }

                FileUtils.chmod(file, 0771);
                extracted = true;
            }
        }
        catch (Exception e) {
            return false;
        }

        return extracted;
    }

    public static File getRuntimeDir(Context context) {
        File dir = new File(context.getFilesDir(), "obb_runtime");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    public static File getWineAddonsDir(Context context) {
        File dir = new File(getRuntimeDir(context), "wine_addons");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    public static File syncWineAddonsIfNeeded(Context context) {
        File obbFile = findMainObb(context);
        if (obbFile == null) return null;

        File destination = getWineAddonsDir(context);
        String stamp = obbFile.getAbsolutePath()+":"+obbFile.lastModified()+":"+obbFile.length();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String oldStamp = preferences.getString(PREF_WINE_ADDONS_STAMP, "");

        if (!stamp.equals(oldStamp) || FileUtils.isEmpty(destination)) {
            FileUtils.delete(destination);
            destination.mkdirs();
            if (!extractDirectory(context, "wine_addons/", destination)) return null;
            preferences.edit().putString(PREF_WINE_ADDONS_STAMP, stamp).apply();
        }
        return destination.isDirectory() ? destination : null;
    }

    private static String normalizeEntry(String entryName) {
        while (entryName.startsWith("/")) entryName = entryName.substring(1);
        return entryName;
    }

    private static String normalizePrefix(String prefix) {
        prefix = normalizeEntry(prefix);
        return prefix.endsWith("/") ? prefix : prefix+"/";
    }
}
