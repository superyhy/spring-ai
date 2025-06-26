package com.yhy.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author Yhy
 * @create 2025/6/26 11:33
 * @describe
 */
@Data
public class MessageDTO {

    private String user;
    private String assistant;

    public MessageDTO(String user, String assistant) {
        this.user = user;
        this.assistant = assistant;
    }

    public String getUser() {
        return user;
    }

    public String getAssistant() {
        return assistant;
    }
}
