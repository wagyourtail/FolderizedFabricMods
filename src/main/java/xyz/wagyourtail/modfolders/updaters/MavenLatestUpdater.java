package xyz.wagyourtail.modfolders.updaters;

import xyz.wagyourtail.modfolders.Config;
import xyz.wagyourtail.modfolders.IOLib;
import xyz.wagyourtail.modfolders.ModUpdater;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class MavenLatestUpdater extends ModUpdater {
    public MavenLatestUpdater(File folder, Config.AutoUpdate update) {
        super(folder, update);
    }
    
    @Override
    public void updateMod() throws IOException {
        File modFile = getFile();
        String hash = null;
        if (modFile != null) {
            hash = hashFile(modFile);
        }
        URL t = new URL(update.url.toString() + "." + update.hashType.toString().toLowerCase());
        String newHash = IOLib.toString(IOLib.urlGet(t, map));
        String[] tree = update.url.toString().split("/");
        String fname = tree[tree.length - 1];
        if (!newHash.equals(hash)) {
            File parent = modFile.getParentFile();
            File newf = new File(parent, modFile.getName() + ".delete");
            renameTo(modFile, newf);
            try {
                downloadNewMod(update.url, fname, map);
                if (newf.exists() && !newf.delete()) throw new IOException("Failed to delete old mod file!");
                success(newHash);
            } catch (Throwable tx) {
                renameTo(newf, modFile);
            }
        }
    }
    
    public File renameTo(File f, File newf) throws IOException {
        if (newf.exists() && !newf.delete()) throw new IOException("Failed to delete old .delete file!");
        if (!f.renameTo(newf)) throw new IOException("Failed to move old mod file!");
        return newf;
    }
    public void downloadNewMod(URL url, String fname, Map<String, String> map) throws IOException {
        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(IOLib.urlGet(url, map)))) {
            try (OutputStream outStream = new FileOutputStream(new File(folder, fname))) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = stream.read(buffer, 0, 1024)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        }
    
    }
}
