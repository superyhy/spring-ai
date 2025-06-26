package com.yhy.springai.handler;

import com.yhy.springai.dto.MessageDTO;
import com.yhy.springai.service.DeepSeekChatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Yhy
 * @create 2025/6/26 11:29
 * @describe
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    @Autowired
    DeepSeekChatService deepSeekChatService;

    // 多用户聊天历史：key = sessionId（也可以替换成 userId）
    private final Map<String, List<MessageDTO>> userChatHistories = new ConcurrentHashMap<>();
    //最大消息数量
    private static final int MAX_HISTORY_SIZE = 50;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        //获取用户id
        String userId = getUserId(session);
        // 初始化对话历史
        userChatHistories.putIfAbsent(userId, Collections.synchronizedList(new ArrayList<>()));
        System.out.println("连接建立：" + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String userInput = message.getPayload();
        String userId = getUserId(session);

        List<MessageDTO> history = userChatHistories.getOrDefault(userId, new ArrayList<>());

        if (history == null) {
            session.sendMessage(new TextMessage("会话未建立成功，请稍后重试"));
            return;
        }

        try {
            // 调用 Spring AI，多轮对话
            String reply = deepSeekChatService.multipleChatWithPrompt(history, userInput);

            //线程安全
            synchronized (history) {
                // 添加 AI 回复
                history.add(new MessageDTO(userInput, reply));
                if (history.size() > MAX_HISTORY_SIZE) {
                    history.remove(0);
                }
            }
            // 回复客户端
            session.sendMessage(new TextMessage(reply));
        } catch (Exception e) {
            session.sendMessage(new TextMessage("AI回复消息失败，请稍后重试。"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        userChatHistories.remove(userId);
        System.out.println("连接关闭：" + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
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
