package com.yhy.springai.service;

import com.yhy.springai.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Yhy
 * @create 2025/6/25 12:57
 * @describe
 */
@Service
@Slf4j
public class DeepSeekChatService {
    @Autowired
    OpenAiChatModel openAiChatModel;


    public String singleChat(String message) {
        return openAiChatModel.call(message);
    }

    /**
     * 多轮对话
     */
    public String multipleChat(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("对话历史不能为空");
        }
        return openAiChatModel.call(messages.toArray(new Message[0]));
    }

    /**
     * 构建多轮消息调用（含 system prompt）
     */
    public String multipleChatWithPrompt(List<MessageDTO> history, String userInput) {
        List<Message> messages = new ArrayList<>();

        // 可选：添加角色提示
        messages.add(new SystemMessage("你是一个热情且专业的 AI 编程助手"));

        // 添加历史
        for (MessageDTO dto : history) {
            messages.add(new UserMessage(dto.getUser()));
            messages.add(new AssistantMessage(dto.getAssistant()));
        }

        // 添加当前提问
        messages.add(new UserMessage(userInput));

        // 调用模型
        return multipleChat(messages);
    }

}
