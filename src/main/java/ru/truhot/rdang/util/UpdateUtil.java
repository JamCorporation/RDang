package ru.truhot.rdang.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.util.logger.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateUtil {
    private final RDang plugin;
    private final String url = "https://api.github.com/repos/Truhott/RDang/releases/latest";
    private boolean updateAvailable = false;
    private String latestVersion = null;

    public UpdateUtil(RDang plugin) {
        this.plugin = plugin;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java 11 HttpClient (RDang)")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(response.body()).getAsJsonObject();
                    latestVersion = jsonObject.has("tag_name") ? jsonObject.get("tag_name").getAsString().replace("v", "") : "Unknown";
                    String currentVersion = plugin.getDescription().getVersion().replace("v", "");
                    updateAvailable = needsUpdate(currentVersion, latestVersion);
                    if (updateAvailable) {
                        Logger.warn("Вы используете устаревшую версию плагина (&c" + currentVersion + "&e)");
                        Logger.warn("Скачать новую версию можно тут - &nhttps://github.com/Truhott/RDang/releases");
                        Logger.warn("Или используйте &a/rdang update &fдля обновления.");
                    } else {
                        Logger.info("Актуальная версия: " + currentVersion);
                    }
                }
            } catch (Exception e) {
                Logger.error("Ошибка проверки обновления: " + e.getMessage());
            }
        });
    }

    public void downloadUpdate(String ver, CommandSender sender) {
        try {
            File currentJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            String downloadUrl = "https://github.com/Truhott/RDang/releases/download/" + ver + "/RDang.jar";
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            int fileSize = connection.getContentLength();
            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(currentJar)) {
                byte[] data = new byte[1024];
                int bytesRead;
                int totalBytesRead = 0;
                int lastPercentage = 0;
                while ((bytesRead = in.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    if (fileSize > 0) {
                        int progress = (int) ((double) totalBytesRead / fileSize * 100);
                        if (progress >= lastPercentage + 20) {
                            lastPercentage = progress;
                            Logger.info("Загрузка обновления: " + progress + "% (" + (totalBytesRead / 1024) + "/" + (fileSize / 1024) + " KB)");
                        }
                    }
                }
                Logger.info("Обновление до версии " + ver + " успешно загружено.");
            }
        } catch (Exception e) {
            Logger.error("Ошибка при загрузке обновления: " + e.getMessage());
        }
    }

    private boolean needsUpdate(String current, String latest) {
        try {
            String[] cP = current.split("\\.");
            String[] lP = latest.split("\\.");
            int length = Math.max(cP.length, lP.length);
            for (int i = 0; i < length; i++) {
                int cV = i < cP.length ? Integer.parseInt(cP[i]) : 0;
                int lV = i < lP.length ? Integer.parseInt(lP[i]) : 0;
                if (cV < lV) return true;
                if (cV > lV) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }
}