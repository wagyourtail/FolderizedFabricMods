package xyz.wagyourtail.modfolders.updaters;

import xyz.wagyourtail.modfolders.Config;
import xyz.wagyourtail.modfolders.IOLib;
import xyz.wagyourtail.modfolders.ModUpdater;

import java.io.*;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenXMLUpdater extends ModUpdater {
    
    protected MavenXMLUpdater(File folder, Config.AutoUpdate update) {
        super(folder, update);
    }
    
    @Override
    public void updateMod() throws IOException {
        File modFile = getFile();
        String hash = null;
        if (modFile != null) {
            hash = hashFile(modFile);
        }
        String resp = IOLib.toString(IOLib.urlGet(update.url, map));
        Matcher m = Pattern.compile("<latest>(.+?)</latest>").matcher(resp);
        Matcher m2 = Pattern.compile("<artifactId>(.+?)</artifactId>").matcher(resp);
        if (m.find() && m2.find()) {
            String newHash = m.group(1);
            String fname;
            URL url = new URL(update.url.toString() + (fname = m2.group(1) + "-" + newHash + ".jar"));
            if (!newHash.equals(hash)) {
                downloadNewMod(url, fname);
                if (modFile != null && !modFile.delete()) throw new IOException("Failed to delete old mod file!");
            } else if (update.hashType != Config.HashType.VERSION_NUM) {
                new MavenLatestUpdater(folder, new Config.AutoUpdate(update.modfilename, Config.OverrideType.MAVEN_LATEST, update.hashType, url)).updateMod();
            }
        } else {
            throw new IOException("Failed to find <latest> tag in metadata at " + update.url);
        }
    }
    
    public void downloadNewMod(URL url, String fname) throws IOException {
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
