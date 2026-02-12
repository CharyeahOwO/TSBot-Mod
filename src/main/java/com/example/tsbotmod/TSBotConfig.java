package com.example.tsbotmod;

/**
 * TSBot 配置数据类，对应 tsbot-config.toml 中的字段。
 */
public class TSBotConfig {
    // TS3 ServerQuery 连接设置
    public String host = "localhost";
    public int port = 10011;
    public String user = "serveradmin";
    public String password = "";

    /** 默认音乐源：wyy 或 qq */
    public String defaultSource = "wyy";

    /** 网易云音乐 API 地址 */
    public String neteaseApiUrl = "http://localhost:3000";

    /** QQ 音乐 API 地址 */
    public String qqApiUrl = "http://localhost:3300";
}
