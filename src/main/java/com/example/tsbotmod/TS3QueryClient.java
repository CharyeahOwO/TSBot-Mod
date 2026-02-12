package com.example.tsbotmod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 使用原生 java.net.Socket 与 TS3 ServerQuery 通信的客户端。
 *
 * 协议流程：
 * 1. 连接 host:port（带超时）
 * 2. login serveradmin <password>
 * 3. use 1
 * 4. sendtextmessage targetmode=2 msg=<escapedCommand>
 *
 * 所有 Payload 严格遵守 TS3 转义规则。
 */
public class TS3QueryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("TS3QueryClient");
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    /**
     * 发送指令到 TS3 机器人。
     * @throws RuntimeException 如果连接/认证/发送失败
     */
    public void sendBotCommand(String rawCommand) {
        TSBotConfig config = TSBotConfigLoader.getConfig();

        String escapedCommand = ts3Escape(rawCommand);
        LOGGER.info("准备发送 TS3 指令: {}", escapedCommand);

        try (Socket socket = new Socket()) {
            // 带超时的连接
            socket.connect(new InetSocketAddress(config.host, config.port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // ===== 完整消耗 Welcome Banner =====
            // TS3 ServerQuery 连接后会发送多行 Banner，格式通常为：
            //   TS3
            //   Welcome to the TeamSpeak 3 ServerQuery interface, ...
            //   (可能还有更多行)
            // 必须全部读完，否则后续 login 响应会读到 Banner 内容
            consumeWelcomeBanner(reader);

            // 登录 — 使用键值对格式，兼容含特殊字符的密码
            if (config.password == null || config.password.isEmpty()) {
                LOGGER.warn("TS3 密码为空！请在 tsbot-config.toml 中设置 password");
            }
            String loginCmd = "login client_login_name=" + ts3Escape(config.user)
                    + " client_login_password=" + ts3Escape(config.password);
            writeLine(writer, loginCmd);
            String loginResponse = readResponse(reader);
            if (loginResponse == null || !loginResponse.startsWith("error id=0")) {
                throw new RuntimeException("TS3 ServerQuery 认证失败: " + loginResponse);
            }
            LOGGER.info("TS3 登录成功");

            // 选择虚拟服务器 1
            writeLine(writer, "use 1");
            String useResponse = readResponse(reader);
            if (useResponse == null || !useResponse.startsWith("error id=0")) {
                LOGGER.warn("use 1 响应: {}", useResponse);
            }

            // 发送文本消息到全服务器广播 (targetmode=3)
            String sendText = "sendtextmessage targetmode=3 msg=" + escapedCommand;
            writeLine(writer, sendText);
            String sendResponse = readResponse(reader);
            if (sendResponse == null || !sendResponse.startsWith("error id=0")) {
                LOGGER.warn("sendtextmessage 响应: {}", sendResponse);
            }

            // 退出登录
            writeLine(writer, "quit");

            LOGGER.info("TS3 指令发送完成");
        } catch (IOException e) {
            String msg = "TS3 连接失败 (" + config.host + ":" + config.port + "): " + e.getMessage();
            LOGGER.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.write("\n");
        writer.flush();
    }

    /**
     * TS3 ServerQuery 协议完整转义规则。
     * 参考: https://yat.qa/resources/server-query/
     */
    private String ts3Escape(String input) {
        return input
                .replace("\\", "\\\\")   // 反斜杠必须最先转义
                .replace("/", "\\/")      // 斜杠
                .replace(" ", "\\s")      // 空格
                .replace("|", "\\p")      // 管道符
                .replace("\n", "\\n")     // 换行
                .replace("\r", "\\r")     // 回车
                .replace("\t", "\\t")     // 制表符
                .replace("\u0007", "\\a") // 响铃
                .replace("\b", "\\b")     // 退格
                .replace("\f", "\\f");    // 换页
    }

    /**
     * 完整消耗 TS3 ServerQuery 的 Welcome Banner。
     * TS3 连接后发送的 Banner 固定为 2 行：
     *   第1行: "TS3"
     *   第2行: "Welcome to the TeamSpeak 3 ServerQuery interface, ..."
     * Banner 之后不会发送空行，直接进入等待指令状态。
     * 因此：读到包含 "Welcome" 的行后必须立即停止，不能再读。
     */
    private void consumeWelcomeBanner(BufferedReader reader) throws IOException {
        int linesRead = 0;
        final int MAX_BANNER_LINES = 10; // 安全上限

        while (linesRead < MAX_BANNER_LINES) {
            String line = reader.readLine();
            linesRead++;

            if (line == null) {
                LOGGER.warn("TS3 连接在读取 Banner 时关闭");
                break;
            }

            LOGGER.info("TS3 Banner [{}]: {}", linesRead, line);

            // Welcome 行是 Banner 的最后一行，读到就结束
            if (line.contains("Welcome")) {
                break;
            }
        }

        LOGGER.info("TS3 Welcome Banner 已消耗 ({} 行)", linesRead);
    }

    /**
     * 读取 TS3 ServerQuery 命令响应。
     * ServerQuery 的响应可能有数据行 + 以 "error" 开头的状态行。
     * 此方法跳过数据行，返回 "error ..." 状态行。
     */
    private String readResponse(BufferedReader reader) throws IOException {
        final int MAX_RESPONSE_LINES = 20;
        for (int i = 0; i < MAX_RESPONSE_LINES; i++) {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            LOGGER.debug("TS3 响应: {}", line);
            // TS3 命令响应以 "error id=" 开头
            if (line.startsWith("error ")) {
                return line;
            }
            // 其他行是数据行，继续读
        }
        LOGGER.warn("TS3 响应读取超过上限 ({} 行)", MAX_RESPONSE_LINES);
        return null;
    }
}
