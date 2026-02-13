package com.example.tsbotmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod(TSBotMod.MODID)
public class TSBotMod {
    public static final String MODID = "tsbotmod";
    private static final Logger LOGGER = LoggerFactory.getLogger("TSBotMod");

    private final TS3QueryClient ts3Client = new TS3QueryClient();
    private final MusicSearchService searchService = new MusicSearchService();
    private final PlayQueue playQueue;

    public TSBotMod() {
        playQueue = new PlayQueue(ts3Client);
        MinecraftForge.EVENT_BUS.register(this);

        TSBotConfig config = TSBotConfigLoader.getConfig();
        LOGGER.info("TSBotMod V2.0 已加载，等待服务器指令。");
        LOGGER.info("  TS3 服务器: {}:{}", config.host, config.port);
        LOGGER.info("  网易云 API: {}", config.neteaseApiUrl);
        LOGGER.info("  QQ音乐 API: {}", config.qqApiUrl);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(buildRootCommand());
        LOGGER.info("/ts 指令已注册 (V2.0)。");
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildRootCommand() {
        return Commands.literal("ts")
                .then(Commands.literal("wyy")
                        .then(Commands.literal("search")
                                .then(Commands.argument("keyword", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String keyword = StringArgumentType.getString(ctx, "keyword");
                                            handleSearch(ctx.getSource(), "wyy", keyword);
                                            return 1;
                                        })))
                        .then(Commands.literal("play")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            handlePlay(ctx.getSource(), "wyy", id);
                                            return 1;
                                        })))
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            handleAdd(ctx.getSource(), "wyy", id);
                                            return 1;
                                        }))))

                .then(Commands.literal("qq")
                        .then(Commands.literal("search")
                                .then(Commands.argument("keyword", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String keyword = StringArgumentType.getString(ctx, "keyword");
                                            handleSearch(ctx.getSource(), "qq", keyword);
                                            return 1;
                                        })))
                        .then(Commands.literal("play")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            handlePlay(ctx.getSource(), "qq", id);
                                            return 1;
                                        })))
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            handleAdd(ctx.getSource(), "qq", id);
                                            return 1;
                                        }))))

                .then(Commands.literal("next")
                        .executes(ctx -> {
                            handleNext(ctx.getSource());
                            return 1;
                        }))

                .then(Commands.literal("pause")
                        .executes(ctx -> {
                            handlePause(ctx.getSource());
                            return 1;
                        }));
    }

    private void handleSearch(CommandSourceStack source, String platform, String keyword) {
        String platformName = "wyy".equals(platform) ? "网易云" : "QQ";
        source.sendSuccess(() -> Component.literal(
                "§7[TSBot] 正在搜索" + platformName + "：" + keyword + " ..."), false);

        CompletableFuture<List<MusicSearchResult>> future =
                "wyy".equals(platform)
                        ? searchService.searchNetease(keyword)
                        : searchService.searchQQ(keyword);

        future.thenAccept(results -> {
            if (results.isEmpty()) {
                source.sendFailure(Component.literal(
                        "§c[TSBot] 未找到任何结果，请尝试其他关键词。"));
                return;
            }

            source.sendSuccess(() -> Component.literal(
                    "§a[TSBot] §f" + platformName + "搜索结果（点击 §b[播放]§f 或 §e[入队]§f）："), false);

            for (int i = 0; i < results.size(); i++) {
                MusicSearchResult r = results.get(i);
                int index = i + 1;

                MutableComponent line = Component.literal("§7[" + index + "] §f" + r.getDisplayName() + " ");

                MutableComponent playBtn = Component.literal("§b[播放]");
                playBtn.setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/ts " + platform + " play " + r.getId() + " " + r.getDisplayName()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("§a点击立即播放：" + r.getDisplayName()))));

                MutableComponent separator = Component.literal(" ");

                MutableComponent addBtn = Component.literal("§e[入队]");
                addBtn.setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/ts " + platform + " add " + r.getId() + " " + r.getDisplayName()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("§e点击加入播放队列：" + r.getDisplayName()))));

                line.append(playBtn).append(separator).append(addBtn);

                source.sendSuccess(() -> line, false);
            }
        }).exceptionally(ex -> {
            LOGGER.error("搜索异常", ex);
            source.sendFailure(Component.literal(
                    "§c[TSBot] 搜索失败: " + ex.getMessage()));
            return null;
        });
    }

    private void handlePlay(CommandSourceStack source, String platform, String idAndName) {
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();

        String songId;
        String displayName;
        int spaceIdx = idAndName.indexOf(' ');
        if (spaceIdx > 0) {
            songId = idAndName.substring(0, spaceIdx);
            displayName = idAndName.substring(spaceIdx + 1);
        } else {
            songId = idAndName;
            displayName = idAndName;
        }

        source.sendSuccess(() -> Component.literal(
                "§7[TSBot] 正在播放..."), false);

        playQueue.playSong(server, player, platform, songId, displayName)
                .exceptionally(ex -> {
                    LOGGER.error("播放失败", ex);
                    source.sendFailure(Component.literal(
                            "§c[TSBot] 播放失败: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleAdd(CommandSourceStack source, String platform, String idAndName) {
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();

        String songId;
        String displayName;
        int spaceIdx = idAndName.indexOf(' ');
        if (spaceIdx > 0) {
            songId = idAndName.substring(0, spaceIdx);
            displayName = idAndName.substring(spaceIdx + 1);
        } else {
            songId = idAndName;
            displayName = idAndName;
        }

        source.sendSuccess(() -> Component.literal(
                "§7[TSBot] 正在加入队列..."), false);

        playQueue.addToQueue(server, player, platform, songId, displayName)
                .exceptionally(ex -> {
                    LOGGER.error("入队失败", ex);
                    source.sendFailure(Component.literal(
                            "§c[TSBot] 入队失败: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleNext(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();
        String playerName = player != null ? player.getName().getString() : "控制台";

        source.sendSuccess(() -> Component.literal(
                "§7[TSBot] 正在切歌..."), false);

        CompletableFuture.runAsync(() -> {
            try {
                ts3Client.sendBotCommand("!next");
                if (server != null) {
                    server.execute(() -> {
                        Component msg = Component.literal("§a[TSBot] §e" + playerName + " §f切了一首歌");
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(msg);
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("切歌失败", e);
                source.sendFailure(Component.literal(
                        "§c[TSBot] 切歌失败: " + e.getMessage()));
            }
        });
    }

    private void handlePause(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();
        String playerName = player != null ? player.getName().getString() : "控制台";

        source.sendSuccess(() -> Component.literal(
                "§7[TSBot] 正在切换暂停/继续..."), false);

        CompletableFuture.runAsync(() -> {
            try {
                ts3Client.sendBotCommand("!pause");
                if (server != null) {
                    server.execute(() -> {
                        Component msg = Component.literal("§a[TSBot] §e" + playerName + " §f切换了暂停/继续");
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(msg);
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("暂停/继续失败", e);
                source.sendFailure(Component.literal(
                        "§c[TSBot] 暂停/继续失败: " + e.getMessage()));
            }
        });
    }
}
