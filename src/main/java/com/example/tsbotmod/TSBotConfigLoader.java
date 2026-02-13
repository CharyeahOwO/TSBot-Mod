package com.example.tsbotmod;

import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TSBotConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("TSBotConfigLoader");
    private static TSBotConfig cachedConfig;

    private static final String DEFAULT_CONFIG_TEMPLATE = """
            # TSBot 配置文件
            # 由 TSBotMod V2.0 自动生成，请根据你的环境修改下列参数。

            [General]
            host = "localhost"
            port = 10011
            user = "serveradmin"
            password = "YOUR_SERVERQUERY_PASSWORD"

            default_source = "wyy"

            netease_api = "http://localhost:3000"
            qq_api = "http://localhost:3300"
            """;

    public static TSBotConfig getConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfig();
        }
        return cachedConfig;
    }

    public static void reloadConfig() {
        cachedConfig = loadConfig();
    }

    private static TSBotConfig loadConfig() {
        TSBotConfig config = new TSBotConfig();
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("tsbot-config.toml");

        if (!Files.exists(configPath)) {
            LOGGER.warn("========================================");
            LOGGER.warn("[TSBot] 配置文件已生成！请前往 config/tsbot-config.toml 填写密码和 IP！");
            LOGGER.warn("========================================");
            try {
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, DEFAULT_CONFIG_TEMPLATE, StandardCharsets.UTF_8);
                LOGGER.info("配置文件已写入: {}", configPath.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("自动生成配置文件失败!", e);
                return config;
            }
        }

        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                switch (key) {
                    case "host" -> config.host = value;
                    case "port" -> {
                        try {
                            config.port = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("配置 port 解析失败，使用默认值 {}: {}", config.port, value);
                        }
                    }
                    case "user" -> config.user = value;
                    case "password" -> config.password = value;
                    case "default_source" -> {
                        if ("qq".equalsIgnoreCase(value)) {
                            config.defaultSource = "qq";
                        } else {
                            config.defaultSource = "wyy";
                        }
                    }
                    case "netease_api", "netease_api_url" -> config.neteaseApiUrl = stripTrailingSlash(value);
                    case "qq_api", "qq_api_url" -> config.qqApiUrl = stripTrailingSlash(value);
                    default -> {}
                }
            }

            if (config.password == null || config.password.isEmpty()) {
                LOGGER.warn("[TSBot] ⚠ password 为空！ServerQuery 登录将会失败，请在 tsbot-config.toml 中填写密码！");
            }
            if (config.neteaseApiUrl == null || config.neteaseApiUrl.isEmpty()) {
                LOGGER.warn("[TSBot] ⚠ netease_api 未配置！网易云搜索功能将无法使用。");
                config.neteaseApiUrl = "";
            }
            if (config.qqApiUrl == null || config.qqApiUrl.isEmpty()) {
                LOGGER.warn("[TSBot] ⚠ qq_api 未配置！QQ 音乐搜索功能将无法使用。");
                config.qqApiUrl = "";
            }

            LOGGER.info("TSBot 配置已加载: host={}, port={}, user={}, default_source={}, netease_api={}, qq_api={}",
                    config.host, config.port, config.user, config.defaultSource,
                    config.neteaseApiUrl, config.qqApiUrl);
        } catch (IOException e) {
            LOGGER.error("读取 TSBot 配置失败，将使用默认配置", e);
        }

        return config;
    }

    private static String stripTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
