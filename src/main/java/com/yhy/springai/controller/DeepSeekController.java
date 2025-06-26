package com.yhy.springai.controller;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Yhy
 * @create 2025/6/25 11:14
 * @describe
 */
@RestController
@RequestMapping("/api/v1/deep_seek")
public class DeepSeekController {
    @Autowired
    OpenAiChatModel openAiChatModel;

    @GetMapping("/chat")
    public String ask(@RequestParam String message) {
        return openAiChatModel.call(message);
    }

}
