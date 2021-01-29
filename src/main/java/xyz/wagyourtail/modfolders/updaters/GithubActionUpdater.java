package xyz.wagyourtail.modfolders.updaters;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import xyz.wagyourtail.modfolders.Config;
import xyz.wagyourtail.modfolders.FolderMod;
import xyz.wagyourtail.modfolders.IOLib;
import xyz.wagyourtail.modfolders.ModUpdater;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GithubActionUpdater extends ModUpdater {
    
    public GithubActionUpdater(File folder, Config.AutoUpdate update) {
        super(folder, update);
    }
    
    private void getAPIKey() throws IOException, InterruptedException {
        String resp = IOLib.toString(IOLib.urlPost(new URL("https://github.com/login/device/code?client_id=0ba202fb43e4297c214c&scope=repo"), map, null));
        final Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator('=').split(resp);
        
        Desktop.getDesktop().browse(URI.create(URLDecoder.decode(map.get("verification_uri"), StandardCharsets.UTF_8.name())));
        int waitTime = Integer.parseInt(map.get("interval")) * 1000;
        Object waiter = new Object();
        Thread t = new Thread(() -> {
            try {
                do {
                    synchronized (waiter) {
                        waiter.wait(waitTime);
                    }
                    String resp2 = IOLib.toString(IOLib.urlPost(new URL("https://github.com/login/oauth/access_token?client_id=0ba202fb43e4297c214c&grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=" + map.get("device_code")), map, null));
                    
                    final Map<String, String> map2 = Splitter.on('&').trimResults().withKeyValueSeparator('=').split(resp2);
                    
                    FolderMod.config.github_api_key = map2.get("access_token");
                    
                    try (FileWriter writer = new FileWriter(FolderMod.configFile)) {
                        writer.write(FolderMod.gson.toJson(FolderMod.config));
                    }
                    
                } while (FolderMod.config.github_api_key == null);
            } catch (InterruptedException | IOException ignored) {
            }
        });
        
        final JFrame parent = new JFrame();
        JButton btn = new JButton();
        JLabel text = new JLabel("  In order to update github api mods automatically you must authorize with github  ");
        btn.setText("Close");
        btn.addActionListener((b) -> {
            parent.dispose();
            if (t.isAlive())
                t.interrupt();
        });
        JLabel text2 = new JLabel(map.get("user_code"), SwingConstants.CENTER);
        parent.setLayout(new GridLayout(3, 1));
        parent.add(text);
        parent.add(text2);
        parent.add(btn);
        parent.pack();
        parent.setVisible(true);
        
        parent.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (t.isAlive())
                    t.interrupt();
                parent.dispose();
            }
        });
        
        t.start();
        t.join(Integer.parseInt(map.get("expires_in")) * 1000L);
        t.interrupt();
        parent.dispose();
    }
    
    @Override
    public void updateMod() throws IOException, InterruptedException {
        File modFile = getFile();
        String hash = null;
        if (modFile != null) {
            hash = hashFile(modFile);
        }
        final JsonObject runs = FolderMod.gson.fromJson(IOUtils.toString(update.url, Charsets.UTF_8), JsonObject.class);
        JsonArray workflow_runs = runs.getAsJsonArray("workflow_runs");
        if (workflow_runs.size() == 0) {
            throw new IOException(String.format("Failed to check for updates, no runs at url %s", update.url));
        }
        JsonObject newest = workflow_runs.get(0).getAsJsonObject();
        String newHash = newest.get("head_sha").getAsString().substring(0, 7);
        if (!newHash.equals(hash)) {
            if (FolderMod.config.github_api_key == null) {
                getAPIKey();
            }
            String artifactURL = newest.get("artifacts_url").getAsString();
            final JsonObject artifacts = FolderMod.gson.fromJson(IOUtils.toString(URI.create(artifactURL), Charsets.UTF_8), JsonObject.class);
            JsonArray artifactList = artifacts.getAsJsonArray("artifacts");
            if (artifactList.size() == 0) {
                throw new IOException(String.format("Failed to check for updates, no artifacts at url %s", artifactURL));
            }
            JsonObject firstArtifact = artifactList.get(0).getAsJsonObject();
            String downloadURL = firstArtifact.get("archive_download_url").getAsString();
            downloadNewMod(downloadURL, newHash, false);
            if (modFile != null && !modFile.delete()) throw new IOException("Failed to delete old mod file!");
            success(newHash);
        }
    }
    
    public void downloadNewMod(String url, String hash, boolean second) throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>(map);
        headers.put("Authorization", "Bearer " + FolderMod.config.github_api_key);
        
        try (InputStream stream = IOLib.urlGet(new URL(url), headers)) {
            if (stream == null && !second) {
                getAPIKey();
                downloadNewMod(url, hash, true);
                return;
            }
            assert stream != null;
            try (ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(stream, 1024))) {
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
    
}
