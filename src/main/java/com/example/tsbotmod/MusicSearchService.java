package com.example.tsbotmod;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MusicSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicSearchService");
    private static final Gson GSON = new Gson();
    private static final int TIMEOUT_MS = 5000;

    public CompletableFuture<List<MusicSearchResult>> searchNetease(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TSBotConfig config = TSBotConfigLoader.getConfig();
                String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
                String baseUrl = stripTrailingSlash(config.neteaseApiUrl);
                String url = baseUrl + "/search?keywords=" + encoded + "&limit=5";
                String json = httpGet(url);
                return parseNeteaseResults(json);
            } catch (Exception e) {
                LOGGER.error("网易云搜索失败", e);
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<MusicSearchResult>> searchQQ(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TSBotConfig config = TSBotConfigLoader.getConfig();
                String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
                String baseUrl = stripTrailingSlash(config.qqApiUrl);
                String url = baseUrl + "/search?key=" + encoded + "&pageSize=5";
                String json = httpGet(url);
                return parseQQResults(json);
            } catch (Exception e) {
                LOGGER.error("QQ 音乐搜索失败", e);
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private String httpGet(String urlStr) throws Exception {
        LOGGER.info("正在连接音乐 API: {}", urlStr);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "TSBotMod/2.0");

        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP 请求失败，状态码: " + code + " (连接地址: " + urlStr + ")");
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            LOGGER.info("音乐 API 响应成功: {} ({}字节)", urlStr, sb.length());
            return sb.toString();
        } catch (ConnectException e) {
            String msg = "无法连接到音乐 API，请检查配置文件中的 URL 设置！(连接地址: " + urlStr + ")";
            LOGGER.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (SocketTimeoutException e) {
            String msg = "连接音乐 API 超时 (" + TIMEOUT_MS + "ms)，请检查网络或 API 服务状态！(连接地址: " + urlStr + ")";
            LOGGER.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private String stripTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private List<MusicSearchResult> parseNeteaseResults(String json) {
        List<MusicSearchResult> results = new ArrayList<>();
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonObject result = root.getAsJsonObject("result");
            if (result == null) return results;

            JsonArray songs = result.getAsJsonArray("songs");
            if (songs == null) return results;

            for (JsonElement el : songs) {
                JsonObject song = el.getAsJsonObject();
                String id = song.get("id").getAsString();
                String name = song.get("name").getAsString();

                String artist = "未知";
                JsonArray artists = song.getAsJsonArray("artists");
                if (artists != null && artists.size() > 0) {
                    artist = artists.get(0).getAsJsonObject().get("name").getAsString();
                }
                results.add(new MusicSearchResult(id, name, artist));
            }
        } catch (Exception e) {
            LOGGER.error("解析网易云搜索结果失败", e);
        }
        return results;
    }

    private List<MusicSearchResult> parseQQResults(String json) {
        List<MusicSearchResult> results = new ArrayList<>();
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            JsonArray list = null;

            if (root.has("response")) {
                JsonObject response = root.getAsJsonObject("response");
                if (response.has("data")) {
                    JsonObject data = response.getAsJsonObject("data");
                    if (data.has("song")) {
                        JsonObject song = data.getAsJsonObject("song");
                        list = song.getAsJsonArray("list");
                    }
                }
            }

            if (list == null && root.has("data")) {
                JsonObject data = root.getAsJsonObject("data");
                if (data.has("song")) {
                    JsonObject song = data.getAsJsonObject("song");
                    list = song.getAsJsonArray("list");
                } else if (data.has("list")) {
                    list = data.getAsJsonArray("list");
                }
            }

            if (list == null) return results;

            for (JsonElement el : list) {
                JsonObject song = el.getAsJsonObject();

                String id;
                if (song.has("songmid")) {
                    id = song.get("songmid").getAsString();
                } else if (song.has("songid")) {
                    id = song.get("songid").getAsString();
                } else {
                    continue;
                }

                String name = song.has("songname") ? song.get("songname").getAsString() : "未知";

                String artist = "未知";
                JsonArray singers = song.getAsJsonArray("singer");
                if (singers != null && singers.size() > 0) {
                    artist = singers.get(0).getAsJsonObject().get("name").getAsString();
                }
                results.add(new MusicSearchResult(id, name, artist));
            }
        } catch (Exception e) {
            LOGGER.error("解析 QQ 音乐搜索结果失败", e);
        }
        return results;
    }
}
