package xyz.wagyourtail.modfolders;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.modfolders.updaters.GithubActionUpdater;
import xyz.wagyourtail.modfolders.updaters.GithubReleaseUpdater;
import xyz.wagyourtail.modfolders.updaters.GitlabCIUpdater;
import xyz.wagyourtail.modfolders.updaters.MavenLatestUpdater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public abstract class ModUpdater {
    protected static final Map<String, String> map = new HashMap<>();
    static {
        map.put("User-Agent", "FolderizedFabricMod/1.1.0");
    }
    protected final Config.AutoUpdate update;
    protected final File folder;
    
    protected ModUpdater(File folder, Config.AutoUpdate update) {
        this.update = update;
        this.folder = folder;
    }
    
    public abstract void updateMod() throws IOException, URISyntaxException, InterruptedException;
    
    public void failed(String reason) {
        FolderMod.LOGGER.warn("[" + FolderMod.class.getSimpleName() + "] failed to update mod " + update + " due to " + reason);
    }
    
    public void success(String hash) {
        FolderMod.LOGGER.info("[" + FolderMod.class.getSimpleName() + "] successfully updated " + update + " to " + hash);
    }
    
    public File getFile() {
        for (File f : folder.listFiles()) {
            if (f.getName().matches(update.modfilename) && f.getName().endsWith(".jar")) {
                return f;
            }
        }
        return null;
    }
    
    public String hashFile(File file) throws IOException {
        HashFunction hasher;
        switch (update.hashType) {
            case SHA1:
                hasher = Hashing.sha1();
                break;
            case SHA256:
                hasher = Hashing.sha256();
                break;
            case MD5:
                hasher = Hashing.md5();
                break;
            case GIT:
                String[] parts = file.getName().split("-");
                String hashPart  = parts[parts.length - 1];
                hashPart = hashPart.substring(0, hashPart.length() - 4);
                return hashPart;
            case VERSION_NUM:
                String[] parts2 = file.getName().split("-");
                String verPart = parts2[parts2.length - 1];
                verPart = verPart.substring(0, verPart.length() - 4).replace("v", "");
                return verPart;
            case NONE:
            default:
                return null;
        }
    
        HashCode code = Files.hash(file, hasher);
        return code.toString();
    }
    
    @Nullable
    public static ModUpdater getUpdater(File folder, Config.AutoUpdate update) {
        switch (update.source) {
            case GITHUB_ACTION:
                return new GithubActionUpdater(folder, update);
            case GITHUB_RELEASE:
                return new GithubReleaseUpdater(folder, update);
            case GITLAB_CI:
                return new GitlabCIUpdater(folder, update);
            case MAVEN_LATEST:
                return new MavenLatestUpdater(folder, update);
            case MAVEN_XML:
                break;
            default:
                FolderMod.LOGGER.warn("[" + FolderMod.class.getSimpleName() + "] unknown auto-update source for " + update);
        }
        return null;
    }
    
}
