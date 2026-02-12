package com.example.tsbotmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 播放队列管理与全服广播。
 * 负责通过 TS3 ServerQuery 发送播放/入队指令，并向全服玩家广播通知。
 */
public class PlayQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayQueue");
    private final TS3QueryClient ts3Client;

    public PlayQueue(TS3QueryClient ts3Client) {
        this.ts3Client = ts3Client;
    }

    /**
     * 直接播放歌曲（立即插入播放）。
     * 映射为 !wyy play <id> 或 !qq play <id>
     *
     * @param server   MC 服务器实例（用于广播）
     * @param player   触发的玩家（可能为 null）
     * @param platform "wyy" 或 "qq"
     * @param songId   歌曲 ID
     * @param displayName 歌曲显示名（歌曲名 - 歌手），用于广播
     */
    public CompletableFuture<Void> playSong(MinecraftServer server, ServerPlayer player,
                                             String platform, String songId, String displayName) {
        String botCommand = "!" + platform + " play " + songId;
        String playerName = player != null ? player.getName().getString() : "控制台";

        return CompletableFuture.runAsync(() -> {
            try {
                ts3Client.sendBotCommand(botCommand);
                broadcast(server, "§a[TSBot] §f玩家 §e" + playerName + " §f播放了：§b" + displayName);
                LOGGER.info("播放指令已发送: {} -> {}", playerName, botCommand);
            } catch (Exception e) {
                LOGGER.error("播放指令失败", e);
                broadcast(server, "§c[TSBot] 播放失败: " + e.getMessage());
            }
        });
    }

    /**
     * 将歌曲加入播放队列。
     * 映射为 !wyy add <id> 或 !qq add <id>
     *
     * @param server   MC 服务器实例（用于广播）
     * @param player   触发的玩家（可能为 null）
     * @param platform "wyy" 或 "qq"
     * @param songId   歌曲 ID
     * @param displayName 歌曲显示名（歌曲名 - 歌手），用于广播
     */
    public CompletableFuture<Void> addToQueue(MinecraftServer server, ServerPlayer player,
                                               String platform, String songId, String displayName) {
        String botCommand = "!" + platform + " add " + songId;
        String playerName = player != null ? player.getName().getString() : "控制台";

        return CompletableFuture.runAsync(() -> {
            try {
                ts3Client.sendBotCommand(botCommand);
                broadcast(server, "§a[TSBot] §f玩家 §e" + playerName + " §f点播了：§b" + displayName + " §7(已入队)");
                LOGGER.info("入队指令已发送: {} -> {}", playerName, botCommand);
            } catch (Exception e) {
                LOGGER.error("入队指令失败", e);
                broadcast(server, "§c[TSBot] 入队失败: " + e.getMessage());
            }
        });
    }

    /**
     * 全服广播消息。
     */
    private void broadcast(MinecraftServer server, String message) {
        if (server == null) return;
        server.execute(() -> {
            Component component = Component.literal(message);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(component);
            }
        });
    }
}
