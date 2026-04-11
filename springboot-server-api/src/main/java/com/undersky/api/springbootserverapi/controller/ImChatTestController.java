package com.undersky.api.springbootserverapi.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * IM 测试页：显式返回 HTML，避免仅依赖默认静态映射时 404（尤其 WAR 外置 Tomcat / 反向代理场景）。
 */
@Controller
public class ImChatTestController {

    @GetMapping(value = {"/im/im-chat", "/im-chat.html"})
    public ResponseEntity<String> imChatPage() throws IOException {
        var res = new ClassPathResource("static/im-chat.html");
        if (!res.exists()) {
            return ResponseEntity.notFound().build();
        }
        String html;
        try (InputStream in = res.getInputStream()) {
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(html);
    }
}
