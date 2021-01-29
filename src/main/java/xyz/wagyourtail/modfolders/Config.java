package xyz.wagyourtail.modfolders;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Config {
    /**
     * only needed for action runs downloads
     */
    public String github_api_key;
    public Map<String, AutoUpdate[]> updaters = new HashMap<>();
    
    public static class AutoUpdate {
    
        /**
         * file name as regex matcher, this should also (exclusively) match the downloaded file's name if there are multiple latest downloads.
         *
         * case sensitive
         *
         * if {@link #hashType} is GIT, the short git hash should be at the end of the file name after a "-", it will be appended
         *      on updates if it's not already present.
         * if {@link #hashType} is VERSION_NUM, this file should end with a semver after last "-", if it doesn't it will be appended.
         */
        public String modfilename;
        
        /**
         * where is the file updated from
         * MAVEN_LATEST uses a static maven directory that always gets the updated mod.
         * MAVEN_POM uses the latest from the maven-metadata.xml
         */
        public OverrideType source;
    
        /**
         * type of hash that should be used for comparison,
         *  should be set to GIT or NONE for gitlab/github,
         *  VERSION_NUM for github releases or any but GIT for maven.
         */
        public HashType hashType;
        
        /**
         * update url,
         *
         * GITHUB_ACTION url is {@code https://api.github.com/repos/{owner}/{repo}/actions/runs?branch=bname&status=completed}
         * GITHUB_RELEASE url is {@code https://api.github.com/repos/{owner}/{repo}/releases}
         * GITLAB_CI url is {@code https://api.gitlab.com/projects/{id}/pipelines?ref={branch}&status=success}
         * MAVEN_LATEST should point directly at the jar
         * MAVEN_XML should point at the {@code maven.metadata.xml}
         */
        public URL url;
    
        public AutoUpdate(String modfilename, OverrideType source, HashType hashType, URL url) {
            this.modfilename = modfilename;
            this.source = source;
            this.hashType = hashType;
            this.url = url;
        }
    
        public String toString() {
            return String.format("{\"modfilename\":\"%s\", \"source\":\"%s\", \"url\":\"%s\"}", modfilename, source, url);
        }
    }
    
    public enum OverrideType {
        GITHUB_ACTION, GITHUB_RELEASE, GITLAB_CI, MAVEN_LATEST, MAVEN_XML
    }
    
    public enum HashType {
        NONE, SHA1, SHA256, MD5, GIT, VERSION_NUM
    }
}
