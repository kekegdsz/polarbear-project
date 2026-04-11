package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.model.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello 示例控制器
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public Result<String> hello() {
        return Result.success("Hello World");
    }
}
