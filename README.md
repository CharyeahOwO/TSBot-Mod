# TSBot Mod — Minecraft × TeamSpeak 3 音乐点歌

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-brightgreen?style=flat-square&logo=minecraft" />
  <img src="https://img.shields.io/badge/Forge-47.1.0-orange?style=flat-square" />
  <img src="https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square" />
</p>

> **TSBot Mod** 是一个 Minecraft Forge 服务端 Mod，让玩家在游戏内通过聊天指令搜索、播放和控制 TeamSpeak 3 频道中的音乐机器人，实现 **MC 点歌 → TS3 放歌** 的跨平台联动体验。
  **注意**：该项目架构非常复杂，本人是有自己的需求才开发的，功能非常不完善，没有详细文档，该md文件由Cluade Opus 4.6 生成

---

## 💡 项目背景

许多游戏社区同时使用 **Minecraft 服务器** 和 **TeamSpeak 3 语音频道**。借助 [TS3AudioBot](https://github.com/Splamy/TS3AudioBot) 及其优秀的音乐插件 [**TS3AudioBot-Plugin-Netease-QQ**](https://github.com/RayQuantum/TS3AudioBot-Plugin-Netease-QQ)，TS3 频道已经拥有了强大的点歌能力（支持网易云、QQ 音乐搜索/播放/歌单/FM 等）。

但问题是：**玩家必须切换到 TS3 客户端才能点歌**，打断游戏体验。

**TSBot Mod 解决了这个问题** — 它作为 Minecraft 与 TS3 音乐机器人之间的桥梁，让玩家 **无需离开游戏界面**，直接在 MC 聊天栏搜索音乐、点击播放。指令通过 TS3 ServerQuery 协议透传给 TS3AudioBot 的 Netease-QQ 插件执行。

```
┌──────────────┐  聊天指令   ┌────────────┐  ServerQuery  ┌─────────────────────────┐
│  MC 玩家     │ ─────────▶ │  TSBot Mod │ ────────────▶ │  TS3AudioBot            │
│  /ts wyy     │            │  (本 Mod)  │               │  + Plugin-Netease-QQ    │
│  search 晴天  │            │            │               │  (实际播放音乐)          │
└──────────────┘            └─────┬──────┘               └─────────────────────────┘
                                  │ HTTP
                            ┌─────▼──────┐
                            │ 音乐 API    │
                            │ 网易云/QQ   │
                            │ (搜索歌曲)  │
                            └────────────┘
```

---

## ⚠️ 核心前置依赖

> [!IMPORTANT]
> 本 Mod **必须** 配合以下项目使用，请先完成它们的部署。

### 🎵 [TS3AudioBot-Plugin-Netease-QQ](https://github.com/RayQuantum/TS3AudioBot-Plugin-Netease-QQ) （最重要）

由 [RayQuantum](https://github.com/RayQuantum) 开发的 TS3AudioBot 音乐插件，是本 Mod 的**播放核心**。TSBot Mod 发送的所有播放指令（`!wyy play`、`!qq play`、`!next`、`!pause` 等）最终都由该插件执行。

该插件功能非常丰富：
- ✅ 网易云音乐 / QQ 音乐 双平台播放
- ✅ 歌曲点播、添加队列、下一首播放
- ✅ 歌单、专辑批量播放
- ✅ 6 种播放模式、FM 模式
- ✅ 歌词显示、进度调节、音量控制
- ✅ Cookie 登录支持 VIP 歌曲
- ✅ 频道无人自动暂停

**部署方式**: 参照其 [README](https://github.com/RayQuantum/TS3AudioBot-Plugin-Netease-QQ#安装方法)，推荐使用 Docker 部署。

### 🤖 [TS3AudioBot](https://github.com/Splamy/TS3AudioBot)

TeamSpeak 3 音频机器人框架，是上述插件的运行环境。

### 🎶 音乐 API 服务

| API | 项目 | 默认端口 | 用途 |
|-----|------|----------|------|
| 网易云音乐 | [Binaryify/NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi) | 3000 | MC 端搜索 + TS3 端播放 |
| QQ 音乐 | [jsososo/QQMusicApi](https://github.com/jsososo/QQMusicApi) | 3300 | MC 端搜索 + TS3 端播放 |

这两个 API 同时被 **TSBot Mod**（用于搜索展示结果）和 **Plugin-Netease-QQ**（用于获取播放链接）使用。

---

## ✨ 功能

| 功能 | 说明 |
|------|------|
| 🔍 **双平台搜索** | 网易云 + QQ 音乐关键词搜索，结果在游戏内展示 |
| ▶️ **可点击播放** | 搜索结果带 **[播放]** / **[入队]** 交互按钮 |
| 📋 **播放队列** | 支持"立即播放"和"加入队列"两种模式 |
| ⏭ **切歌 / 暂停** | `/ts next` 切歌、`/ts pause` 暂停/继续 |
| 📢 **全服广播** | 播放/入队/切歌时全服通知，显示歌名和操作者 |
| ⚙️ **自动配置** | 首次启动自动生成 `tsbot-config.toml` 并提醒填写 |
| 🔓 **全员可用** | 无需 OP 权限，所有玩家均可使用 |

---

## 🚀 快速开始

### 前置要求

- [x] Minecraft Forge 1.20.1 服务端
- [x] 运行中的 TeamSpeak 3 服务器
- [x] 已部署 [TS3AudioBot](https://github.com/Splamy/TS3AudioBot) + [Plugin-Netease-QQ](https://github.com/RayQuantum/TS3AudioBot-Plugin-Netease-QQ)
- [x] 运行中的网易云音乐 API 和/或 QQ 音乐 API
- [x] JDK 17（用于构建）

### 1. 安装

从 [Releases](https://github.com/CharyeahOwO/TSBot-Mod/releases) 下载最新的 `tsbotmod-1.0.0.jar`，放入 MC 服务端的 `mods/` 目录。

### 2. 配置

首次启动后自动生成 `config/tsbot-config.toml`，按需修改：

```toml
[General]
# TS3 ServerQuery 连接
host = "your-ts3-server.com"      # TS3 服务器地址
port = 10011                       # ServerQuery 端口
user = "serveradmin"
password = "YOUR_SERVERQUERY_PASSWORD"

# 默认音乐源
default_source = "wyy"

# 音乐搜索 API 地址
netease_api = "http://your-host:3000"
qq_api = "http://your-host:3300"
```

> [!WARNING]
> **ServerQuery 密码**不是 TS3 服务器连接密码！它是 TS3 服务端首次启动时自动生成的管理接口密码，可在 TS3 服务端日志中找到。

### 3. 启动验证

重启 MC 服务端，控制台应输出：

```
[TSBotMod] TSBotMod V2.0 已加载，等待服务器指令。
[TSBotMod]   TS3 服务器: your-ts3-server.com:10011
[TSBotMod]   网易云 API: http://your-host:3000
[TSBotMod]   QQ音乐 API: http://your-host:3300
```

### 4. 从源码构建

```bash
git clone https://github.com/CharyeahOwO/TSBot-Mod.git
cd TSBot-Mod
JAVA_HOME=/path/to/jdk17 ./gradlew build
# 产物: build/libs/tsbotmod-1.0.0.jar
```

> [!NOTE]
> 必须使用 JDK 17 构建，JDK 21 会报 `Unsupported class file major version 65` 错误。

---

## 📖 指令参考

| 指令 | 说明 | 示例 |
|------|------|------|
| `/ts wyy search <关键词>` | 搜索网易云音乐 | `/ts wyy search 晴天` |
| `/ts qq search <关键词>` | 搜索 QQ 音乐 | `/ts qq search 七里香` |
| `/ts wyy play <ID>` | 播放网易云歌曲 | 点击搜索结果的 **[播放]** |
| `/ts wyy add <ID>` | 加入播放队列 | 点击搜索结果的 **[入队]** |
| `/ts qq play <ID>` | 播放 QQ 歌曲 | 点击搜索结果的 **[播放]** |
| `/ts qq add <ID>` | 加入播放队列 | 点击搜索结果的 **[入队]** |
| `/ts next` | 切换下一首 | `/ts next` |
| `/ts pause` | 暂停 / 继续 | `/ts pause` |

> 搜索结果以可交互消息展示，带有可点击的 [播放] 和 [入队] 按钮，鼠标悬停显示歌曲信息。   
> 上述指令最终通过 ServerQuery 转为 `!wyy play`、`!qq play` 等命令发送给 [Plugin-Netease-QQ](https://github.com/RayQuantum/TS3AudioBot-Plugin-Netease-QQ) 执行。

---

## 🏗️ 技术架构

### 模块概览

| 类名 | 职责 |
|------|------|
| `TSBotMod` | Forge Mod 入口，Brigadier 命令树注册，搜索/播放/控制逻辑 |
| `MusicSearchService` | 异步 HTTP 搜索（`CompletableFuture`），调用网易云 / QQ 音乐 API |
| `MusicSearchResult` | 搜索结果数据类（ID、歌名、歌手、展示名） |
| `PlayQueue` | 播放队列管理，区分"立即播放"与"入队"，全服广播通知 |
| `TS3QueryClient` | TS3 ServerQuery 协议实现（Banner 消耗、键值对认证、TS3 转义） |
| `TSBotConfigLoader` | 配置加载 & 自动生成，空值保护，URL 清理 |
| `TSBotConfig` | 配置数据类 |

### 工作流程

1. 玩家在 MC 中执行 `/ts wyy search 晴天`
2. `MusicSearchService` 异步调用网易云 API 搜索，解析 JSON 结果
3. 搜索结果以可交互消息展示给玩家（带 [播放] / [入队] 按钮）
4. 玩家点击按钮 → 触发 `/ts wyy play <ID> <歌名>` 命令
5. `TS3QueryClient` 通过 ServerQuery 协议连接 TS3 服务器
6. 发送 `!wyy play <ID>` 指令给 TS3AudioBot（由 Plugin-Netease-QQ 执行播放）
7. `PlayQueue` 向全服广播播放通知

### 设计要点

- **完全异步**: 所有网络操作使用 `CompletableFuture`，不阻塞 MC 主线程
- **TS3 协议兼容**: 完整实现 ServerQuery 转义规则、Welcome Banner 消耗、键值对认证
- **健壮的错误处理**: 连接超时 / 认证失败 / 空配置等场景均有明确的用户反馈

---

## 📁 项目结构

```
TSBot-Mod/
├── build.gradle                          # Gradle 构建配置
├── gradle.properties                     # Mod 元数据 & 版本
├── config/
│   └── tsbot-config.toml                 # 配置模板
└── src/main/java/com/example/tsbotmod/
    ├── TSBotMod.java                     # Mod 入口 & 命令注册
    ├── TSBotConfig.java                  # 配置数据类
    ├── TSBotConfigLoader.java            # 配置加载与自动生成
    ├── MusicSearchService.java           # 音乐 API 搜索客户端
    ├── MusicSearchResult.java            # 搜索结果数据类
    ├── PlayQueue.java                    # 播放队列 & 全服广播
    └── TS3QueryClient.java              # TS3 ServerQuery 客户端
```

---

## 🐛 常见问题

| 问题 | 解决方案 |
|------|----------|
| 构建报错 `Unsupported class file major version 65` | 使用 JDK 17，不支持 JDK 21 |
| TS3 报 `invalid loginname or password` | 检查 `password` 是否为 ServerQuery 密码 |
| 搜索成功但播放无声 | 确认 TS3AudioBot + Plugin-Netease-QQ 已正常运行 |
| QQ 音乐搜索返回空 | 检查 QQ 音乐 API 容器是否正常（`curl http://host:3300/search?key=周杰伦`） |

---

## 🙏 致谢

本项目的实现离不开以下优秀的开源项目：

- ⭐ [**TS3AudioBot-Plugin-Netease-QQ**](https://github.com/RayQuantum/TS3AudioBot-Plugin-Netease-QQ) by [RayQuantum](https://github.com/RayQuantum) — 核心前置插件，为 TS3AudioBot 提供网易云 / QQ 音乐播放能力
- [Splamy/TS3AudioBot](https://github.com/Splamy/TS3AudioBot) — TeamSpeak 3 音频机器人框架
- [Binaryify/NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi) — 网易云音乐 API
- [jsososo/QQMusicApi](https://github.com/jsososo/QQMusicApi) — QQ 音乐 API
- [Minecraft Forge](https://minecraftforge.net/) — Mod 加载框架
- [Google Gson](https://github.com/google/gson) — JSON 解析库
- Cluade Code Opus 4.6 生成的AI文档，有问题提交ISSUS

---

## 📄 License

All Rights Reserved. See [LICENSE.txt](LICENSE.txt).
