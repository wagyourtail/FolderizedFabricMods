package xyz.wagyourtail.modfolders;

import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class IOLib {
    public static InputStream urlPost(URL url, Map<String, String> headers, JsonElement formData) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        boolean hasForm = formData != null;
        
        conn.setDoOutput(true);
        if (hasForm) {
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
        }
        
        conn.connect();
        
        if (hasForm) {
            String form = FolderMod.gson.toJson(formData);
            conn.getOutputStream().write(form.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().close();
        }
        if (conn.getResponseCode() == 200)
            return conn.getInputStream();
        return null;
    }
    
    public static InputStream urlGet(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.connect();
        if (conn.getResponseCode() == 200)
            return conn.getInputStream();
        return null;
    }
    
    public static String toString(InputStream stream) throws IOException {
        try {
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        } finally {
            stream.close();
        }
    }
}
