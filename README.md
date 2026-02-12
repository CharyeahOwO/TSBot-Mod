# TSBot Mod — Minecraft × TeamSpeak 3 音乐机器人桥接

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-brightgreen?style=flat-square&logo=minecraft" />
  <img src="https://img.shields.io/badge/Forge-47.1.0-orange?style=flat-square" />
  <img src="https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square" />
</p>

> **TSBot Mod** 让 Minecraft 服务器玩家通过游戏内聊天指令搜索、播放和控制在 TeamSpeak 3 频道中运行的音乐机器人（[TS3AudioBot](https://github.com/Splamy/TS3AudioBot)），实现 **MC 点歌 → TS3 放歌** 的跨平台联动。

---

## ✨ 功能亮点

| 功能 | 说明 |
|------|------|
| 🔍 **双平台搜索** | 支持 **网易云音乐** 和 **QQ 音乐** 关键词搜索 |
| ▶️ **一键播放** | 搜索结果带可点击按钮，点击即播放 |
| 📋 **播放队列** | 支持直接播放或加入播放队列 |
| ⏭ **切歌 / 暂停** | `/ts next` 切歌、`/ts pause` 暂停/继续 |
| 📢 **全服广播** | 播放/入队/切歌操作全服广播通知 |
| ⚙️ **自动生成配置** | 首次启动自动生成 `tsbot-config.toml`，控制台高亮提醒 |
| 🔓 **无权限限制** | 所有玩家均可使用，无需 OP |

---

## 🏗️ 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                    Minecraft Server (Forge)                  │
│                                                             │
│  ┌──────────┐    ┌──────────────────┐    ┌──────────────┐   │
│  │ TSBotMod │───▶│ MusicSearchService│───▶│ 网易云/QQ API │   │
│  │ (命令入口) │    │  (HTTP 搜索)      │    │ (Docker 容器) │   │
│  └────┬─────┘    └──────────────────┘    └──────────────┘   │
│       │                                                      │
│       ▼                                                      │
│  ┌──────────┐    ┌──────────────────┐    ┌──────────────┐   │
│  │PlayQueue │───▶│  TS3QueryClient   │───▶│ TS3AudioBot  │   │
│  │ (队列管理) │    │ (ServerQuery 协议) │    │ (TS3 音乐Bot)│   │
│  └──────────┘    └──────────────────┘    └──────────────┘   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TSBotConfigLoader → TSBotConfig  (配置管理，自动生成)  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 核心模块

| 类名 | 职责 |
|------|------|
| **`TSBotMod`** | Forge Mod 入口，注册 Brigadier 命令树，处理搜索/播放/切歌/暂停逻辑 |
| **`MusicSearchService`** | 异步 HTTP 请求封装，调用网易云 / QQ 音乐 API，解析 JSON 返回搜索结果 |
| **`MusicSearchResult`** | 搜索结果数据类，包含歌曲 ID、名称、歌手、展示名 |
| **`PlayQueue`** | 播放队列管理，区分"立即播放"与"加入队列"，负责全服广播通知 |
| **`TS3QueryClient`** | TS3 ServerQuery 协议客户端，处理连接、Banner 消耗、登录认证、指令发送 |
| **`TSBotConfig`** | 配置数据类 |
| **`TSBotConfigLoader`** | 配置文件加载器，支持自动生成默认配置、空值保护、URL 清理 |

---

## 📦 依赖项目

本 Mod 依赖以下开源项目，请提前部署：

### 1. TS3AudioBot （音乐播放核心）

- **项目**: [Splamy/TS3AudioBot](https://github.com/Splamy/TS3AudioBot)
- **作用**: 运行在 TeamSpeak 3 服务器中的音乐机器人，负责实际的音频播放
- **部署方式**: Docker（推荐 [ancieque/ts3audiobot](https://hub.docker.com/r/ancieque/ts3audiobot)）
- **备注**: 需先有一个运行中的 TeamSpeak 3 服务器

### 2. NeteaseCloudMusicApi （网易云音乐 API）

- **项目**: [Binaryify/NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi)
- **作用**: 提供网易云音乐搜索和歌曲信息接口
- **部署方式**: Docker
  ```bash
  docker run -d --name netease-api -p 3000:3000 binaryify/netease_cloud_music_api
  ```
- **API 路径**: `GET /search?keywords=<关键词>&limit=5`

### 3. QQMusicApi （QQ 音乐 API）

- **项目**: [jsososo/QQMusicApi](https://github.com/jsososo/QQMusicApi)
- **作用**: 提供 QQ 音乐搜索和歌曲信息接口
- **部署方式**: Docker
  ```bash
  docker run -d --name qq-api -p 3300:3300 <your-qqmusicapi-image>
  ```
- **API 路径**: `GET /search?key=<关键词>&pageSize=5`

### 4. Minecraft Forge

- **版本**: 1.20.1 (Forge 47.1.0)
- **下载**: [Forge 官网](https://files.minecraftforge.net/net/minecraftforge/forge/)

---

## 🚀 快速开始

### 前置条件

- JDK 17
- 运行中的 TeamSpeak 3 服务器
- 运行中的 TS3AudioBot（已连接到 TS3 服务器）
- 运行中的网易云音乐 API 和/或 QQ 音乐 API

### 1. 构建 Mod

```bash
git clone https://github.com/YOUR_USERNAME/TSBot-Mod.git
cd TSBot-Mod

# 使用 JDK 17 构建（JDK 21 会报版本错误）
JAVA_HOME=/path/to/jdk17 ./gradlew build
```

构建产物: `build/libs/tsbotmod-1.0.0.jar`

### 2. 安装 Mod

将 `tsbotmod-1.0.0.jar` 复制到 Minecraft 服务端的 `mods/` 目录。

### 3. 配置

首次启动服务端会自动在 `config/` 目录生成 `tsbot-config.toml`，并在控制台输出高亮提醒。

编辑 `config/tsbot-config.toml`：

```toml
[General]
# TS3 ServerQuery 连接信息
host = "your-ts3-server.com"
port = 10011
user = "serveradmin"
password = "YOUR_SERVERQUERY_PASSWORD"

# 默认音乐源：wyy（网易云）或 qq（QQ音乐）
default_source = "wyy"

# 音乐 API 地址
netease_api = "http://your-api-host:3000"
qq_api = "http://your-api-host:3300"
```

> ⚠️ **ServerQuery 密码** 不是 TS3 服务器密码！它是 TS3 服务端首次启动时自动生成的管理接口密码，可在 TS3 服务端日志中找到。

### 4. 启动

重启 Minecraft 服务端，控制台应输出：

```
[TSBotMod] TSBotMod V2.0 已加载，等待服务器指令。
[TSBotMod]   TS3 服务器: your-ts3-server.com:10011
[TSBotMod]   网易云 API: http://your-api-host:3000
[TSBotMod]   QQ音乐 API: http://your-api-host:3300
```

---

## 📖 指令参考

| 指令 | 说明 | 示例 |
|------|------|------|
| `/ts wyy search <关键词>` | 搜索网易云音乐 | `/ts wyy search 晴天` |
| `/ts qq search <关键词>` | 搜索 QQ 音乐 | `/ts qq search 七里香` |
| `/ts wyy play <ID>` | 播放网易云歌曲 | 搜索结果点击 **[播放]** |
| `/ts wyy add <ID>` | 加入播放队列 | 搜索结果点击 **[入队]** |
| `/ts qq play <ID>` | 播放 QQ 音乐歌曲 | 搜索结果点击 **[播放]** |
| `/ts qq add <ID>` | 加入播放队列 | 搜索结果点击 **[入队]** |
| `/ts next` | 切换到下一首 | `/ts next` |
| `/ts pause` | 暂停 / 继续播放 | `/ts pause` |

搜索结果以可交互消息形式显示，带有可点击的 **[播放]** 和 **[入队]** 按钮，鼠标悬停显示歌曲信息。

---

## 🔧 技术细节

### TS3 ServerQuery 协议

本 Mod 通过原生 `java.net.Socket` 实现 TS3 ServerQuery 协议通信：

1. **连接** → 带 5 秒超时的 TCP 连接
2. **消耗 Banner** → 循环读取 TS3 欢迎信息（`TS3` + `Welcome...`），避免与后续响应混淆
3. **认证** → 使用键值对格式 `login client_login_name=xx client_login_password=xx`
4. **选择虚拟服务器** → `use 1`
5. **发送指令** → `sendtextmessage targetmode=3 msg=<已转义的指令>`
6. **退出** → `quit`

所有参数严格遵守 TS3 转义规则（空格→`\s`、斜杠→`\/` 等）。

### 异步设计

所有网络操作（API 搜索、TS3 指令发送）均使用 `CompletableFuture` 异步执行，不阻塞 Minecraft 主线程。

### 错误处理

- `ConnectException` → 提示检查 API 地址配置
- `SocketTimeoutException` → 提示检查网络或 API 服务状态
- `TS3 认证失败` → 提示检查 ServerQuery 密码
- 空值保护 → 密码/API URL 为空时控制台警告

---

## 📁 项目结构

```
TSBot-Mod/
├── build.gradle                          # Gradle 构建脚本 (Forge MDK)
├── gradle.properties                     # Mod 元数据 & 版本号
├── config/
│   └── tsbot-config.toml                 # 配置文件模板
└── src/main/java/com/example/tsbotmod/
    ├── TSBotMod.java                     # Mod 入口，命令注册
    ├── TSBotConfig.java                  # 配置数据类
    ├── TSBotConfigLoader.java            # 配置加载 & 自动生成
    ├── MusicSearchService.java           # 音乐搜索 API 客户端
    ├── MusicSearchResult.java            # 搜索结果数据类
    ├── PlayQueue.java                    # 播放队列 & 全服广播
    └── TS3QueryClient.java              # TS3 ServerQuery 客户端
```

---

## 🐛 常见问题

### Q: 构建报错 `Unsupported class file major version 65`
**A**: 需要使用 JDK 17 构建，不支持 JDK 21。设置 `JAVA_HOME` 指向 JDK 17：
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew build
```

### Q: TS3 报 `invalid loginname or password`
**A**: 检查 `tsbot-config.toml` 中的 `password` 是否为 TS3 ServerQuery 密码（不是服务器连接密码）。

### Q: 搜索成功但播放失败
**A**: 确认 TS3AudioBot 已连接到 TS3 服务器且正常运行。检查 TS3AudioBot 是否安装了对应的音乐源插件。

### Q: QQ 音乐搜索返回空
**A**: 确认 QQ 音乐 API 容器正常运行（`curl http://localhost:3300/search?key=周杰伦`）。

### Q: 广播消息在 TS3 中显示乱码
**A**: TS3 ServerQuery 有严格的字符转义规则，已内置完整转义处理。如仍有问题请提 Issue。

---

## 📄 License

All Rights Reserved. See [LICENSE.txt](LICENSE.txt).

---

## 🙏 致谢

- [Minecraft Forge](https://minecraftforge.net/) — Mod 加载框架
- [Splamy/TS3AudioBot](https://github.com/Splamy/TS3AudioBot) — TeamSpeak 3 音乐机器人
- [Binaryify/NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi) — 网易云音乐 API
- [jsososo/QQMusicApi](https://github.com/jsososo/QQMusicApi) — QQ 音乐 API
- [Google Gson](https://github.com/google/gson) — JSON 解析库
