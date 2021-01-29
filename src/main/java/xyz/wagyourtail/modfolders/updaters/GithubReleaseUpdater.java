package xyz.wagyourtail.modfolders.updaters;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import xyz.wagyourtail.modfolders.Config;
import xyz.wagyourtail.modfolders.FolderMod;
import xyz.wagyourtail.modfolders.IOLib;
import xyz.wagyourtail.modfolders.ModUpdater;

import java.io.*;
import java.net.URL;
import java.util.Objects;

public class GithubReleaseUpdater extends ModUpdater {

    public GithubReleaseUpdater(File folder, Config.AutoUpdate update) {
        super(folder, update);
    }
    
    @Override
    public void updateMod() throws IOException {
        File modFile = getFile();
        String hash = null;
        if (modFile != null) {
            hash = hashFile(modFile);
        }
        final JsonArray releases = FolderMod.gson.fromJson(IOUtils.toString(update.url, Charsets.UTF_8), JsonArray.class);
        if (releases.size() == 0) {
            throw new IOException(String.format("Failed to check for updates, no releases at url %s", update.url));
        }
        String tag_name = releases.get(0).getAsJsonObject().get("tag_name").getAsString();
        if (tag_name.equals(hash)) {
            String download_url = null;
            String fname = null;
            boolean flag = true;
            JsonArray assets = releases.get(0).getAsJsonObject().get("assets").getAsJsonArray();
            for (int i = 0; i < assets.size(); ++i) {
                JsonObject asset = assets.get(i).getAsJsonObject();
                if ((fname = asset.get("name").getAsString()).matches(update.modfilename)) {
                    download_url = asset.get("browser_download_url").getAsString();
                    flag = false;
                    break;
                }
            } // else
            if (flag) {
                throw new IOException("Failed to find a matching asset on build " + tag_name);
            }
            downloadNewMod(download_url, fname, hash);
            if (modFile != null && !modFile.delete()) throw new IOException("Failed to delete old mod file!");
            success(tag_name);
        }
    }
    
    public void downloadNewMod(String url, String fname, String hash) throws IOException {
        if (!fname.endsWith(hash+".jar")) {
            fname = fname.substring(0, fname.length() - 4) + hash + ".jar";
        }
        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(IOLib.urlGet(new URL(url), map)), 1024)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            try (OutputStream outStream = new FileOutputStream(new File(folder, fname))) {
                while ((bytesRead = stream.read(buffer, 0, 1024)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }
    
}
