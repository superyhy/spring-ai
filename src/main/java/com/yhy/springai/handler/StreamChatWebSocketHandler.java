package com.yhy.springai.handler;

import com.yhy.springai.dto.MessageDTO;
import com.yhy.springai.service.DeepSeekChatService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话流式接口
 *
 * @Author Yhy
 * @create 2025/6/27 8:31
 * @describe
 */
@Component
public class StreamChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    @Autowired
    private DeepSeekChatService deepSeekChatService;
    @Autowired
    private OpenAiChatModel openAiChatModel;

    // 多用户聊天历史：key = sessionId（也可以替换成 userId）
    private final Map<String, List<MessageDTO>> userChatHistories = new ConcurrentHashMap<>();
    //最大消息数量
    private static final int MAX_HISTORY_SIZE = 50;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //获取用户id
        String userId = getUserId(session);
        // 初始化对话历史
        userChatHistories.putIfAbsent(userId, Collections.synchronizedList(new ArrayList<>()));
        System.out.println("连接建立：" + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userInput = message.getPayload();
        String userId = getUserId(session);

        List<MessageDTO> history = userChatHistories.getOrDefault(userId, new ArrayList<>());

        if (history == null) {
            session.sendMessage(new TextMessage("会话未建立成功，请稍后重试"));
            return;
        }

        // 调用 Spring AI 流式接口，多轮对话
        StringBuilder aiReplyBuilder = new StringBuilder();
        openAiChatModel.stream(deepSeekChatService.getMultipleMessages(history, userInput).toArray(new Message[0]))
                .subscribe(response -> {
                    try {
                        String reply = new String(response.getBytes(), StandardCharsets.UTF_8);
                        aiReplyBuilder.append(reply);
                        session.sendMessage(new TextMessage(reply));
                    } catch (Exception e) {
                        log.error("发送流式消息失败", e);
                    }
                }, error -> {
                    try {
                        session.sendMessage(new TextMessage("发送AI响应消息失败"));
                    } catch (IOException e) {
                        log.error("发送失败消息异常", e);
                    }
                }, () -> {
                    List<MessageDTO> messageDTOS = userChatHistories.computeIfAbsent(userId, id -> Collections.synchronizedList(new ArrayList<>()));
                    synchronized (messageDTOS) {
                        history.add(new MessageDTO(userInput, aiReplyBuilder.toString()));
                        if (history.size() > MAX_HISTORY_SIZE) {
                            history.remove(0);
                        }
                    }
                });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserId(session);
        userChatHistories.remove(userId);
        System.out.println("连接关闭：" + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("连接异常：{}", exception.getMessage(), exception);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.error("关闭连接失败", e);
        }
        String userId = getUserId(session);
        userChatHistories.remove(userId);
    }

    /**
     * 从连接参数中提取 userId，例如 ws://host/chat?userId=abc123
     */
    private String getUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (StringUtils.isNotBlank(query)) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals("userId")) {
                    return pair[1];
                }
            }
        }
        return session.getId(); // fallback：用 sessionId 替代
    }
}
