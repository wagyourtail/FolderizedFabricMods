package xyz.wagyourtail.modfolders.updaters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import xyz.wagyourtail.modfolders.Config;
import xyz.wagyourtail.modfolders.FolderMod;
import xyz.wagyourtail.modfolders.IOLib;
import xyz.wagyourtail.modfolders.ModUpdater;

import java.io.*;
import java.net.URL;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GitlabCIUpdater extends ModUpdater {

    public GitlabCIUpdater(File folder, Config.AutoUpdate update) {
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
        JsonArray pipelines = FolderMod.gson.fromJson(resp, JsonArray.class);
        if (pipelines.size() == 0) throw new IOException(String.format("Failed to check for updates, no pipelines at url %s", update.url));
        int id = pipelines.get(0).getAsJsonObject().get("id").getAsInt();
        String newHash = pipelines.get(0).getAsJsonObject().get("sha").getAsString().substring(0, 7);
        if (!newHash.equals(hash)) {
            String newurl;
            String jobResp = IOLib.toString(IOLib.urlGet(new URL(newurl = update.url.toString().split("\\?")[0] + "/" + id + "/jobs"), map));
            JsonArray jobs = FolderMod.gson.fromJson(jobResp, JsonArray.class);
            boolean flag = true;
            JsonObject job = null;
            for (int i = 0; i < jobs.size(); ++i) {
                job = jobs.get(i).getAsJsonObject();
                if (job.has("artifacts_file")) {
                    flag = false;
                    break;
                }
            } // else
            if (flag) {
                throw new IOException(String.format("Failed to check for updates, no jobs with valid artifacts at url %s", newurl));
            }
            String jobid = job.get("id").getAsString();
            downloadNewMod(newurl.split("pipelines")[0] + "/jobs/" + jobid + "/artifacts", newHash);
            if (modFile != null && !modFile.delete()) throw new IOException("Failed to delete old mod file!");
            success(newHash);
        }
    }
    
    public void downloadNewMod(String url, String hash) throws IOException {
        try (ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(Objects.requireNonNull(IOLib.urlGet(new URL(url), map)), 1024))) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null)
                if (!entry.getName().endsWith("-dev.jar") && !entry.getName().endsWith("-sources.jar") && entry.getName().endsWith(".jar")) break;
            assert entry != null;
            String[] tree = entry.getName().split("/");
            String fname = tree[tree.length - 1];
            if (!fname.endsWith(hash + ".jar")) {
                fname = fname.substring(0, fname.length() - 4) + hash + ".jar";
            }
            try (OutputStream outStream = new FileOutputStream(new File(folder, fname))) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = zipStream.read(buffer, 0, 1024)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }
    
}
