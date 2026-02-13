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

public class TS3QueryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("TS3QueryClient");
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    public void sendBotCommand(String rawCommand) {
        TSBotConfig config = TSBotConfigLoader.getConfig();

        String escapedCommand = ts3Escape(rawCommand);
        LOGGER.info("准备发送 TS3 指令: {}", escapedCommand);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(config.host, config.port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            consumeWelcomeBanner(reader);

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

            writeLine(writer, "use 1");
            String useResponse = readResponse(reader);
            if (useResponse == null || !useResponse.startsWith("error id=0")) {
                LOGGER.warn("use 1 响应: {}", useResponse);
            }

            String sendText = "sendtextmessage targetmode=3 msg=" + escapedCommand;
            writeLine(writer, sendText);
            String sendResponse = readResponse(reader);
            if (sendResponse == null || !sendResponse.startsWith("error id=0")) {
                LOGGER.warn("sendtextmessage 响应: {}", sendResponse);
            }

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

    private String ts3Escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("/", "\\/")
                .replace(" ", "\\s")
                .replace("|", "\\p")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\u0007", "\\a")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    private void consumeWelcomeBanner(BufferedReader reader) throws IOException {
        int linesRead = 0;
        final int MAX_BANNER_LINES = 10;

        while (linesRead < MAX_BANNER_LINES) {
            String line = reader.readLine();
            linesRead++;

            if (line == null) {
                LOGGER.warn("TS3 连接在读取 Banner 时关闭");
                break;
            }

            LOGGER.info("TS3 Banner [{}]: {}", linesRead, line);

            if (line.contains("Welcome")) {
                break;
            }
        }

        LOGGER.info("TS3 Welcome Banner 已消耗 ({} 行)", linesRead);
    }

    private String readResponse(BufferedReader reader) throws IOException {
        final int MAX_RESPONSE_LINES = 20;
        for (int i = 0; i < MAX_RESPONSE_LINES; i++) {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            LOGGER.debug("TS3 响应: {}", line);
            if (line.startsWith("error ")) {
                return line;
            }
        }
        LOGGER.warn("TS3 响应读取超过上限 ({} 行)", MAX_RESPONSE_LINES);
        return null;
    }
}
