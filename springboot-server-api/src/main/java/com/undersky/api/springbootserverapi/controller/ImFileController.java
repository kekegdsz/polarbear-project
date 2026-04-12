package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.ImUploadVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * IM 附件：需登录上传；下载为公开链接（文件名随机 UUID，便于客户端展示图片/语音）。
 */
@RestController
@RequestMapping("/im")
public class ImFileController {

    private static final Pattern SAFE_FILE_NAME = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9]{1,10}$");

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "mp4", "mov", "m4v", "3gp",
            "m4a", "aac", "mp3", "wav", "amr", "ogg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "txt", "csv");

    private final UserMapper userMapper;
    private final Path uploadRoot;

    public ImFileController(UserMapper userMapper,
                            @Value("${im.upload.dir:./data/im-upload}") String uploadDir) throws IOException {
        this.userMapper = userMapper;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadRoot);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImUploadVO> upload(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("请先登录");
        }
        if (userMapper.selectByToken(token.trim()) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("登录已失效");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("文件为空");
        }
        String original = file.getOriginalFilename();
        String ext = extensionOf(original);
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            return Result.error("不支持的文件类型");
        }
        String stored = UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + "." + ext.toLowerCase(Locale.ROOT);
        Path target = uploadRoot.resolve(stored).normalize();
        if (!target.startsWith(uploadRoot)) {
            return Result.error("非法路径");
        }
        file.transferTo(target.toFile());

        String ctx = request.getContextPath();
        if (ctx == null) {
            ctx = "";
        }
        String path = ctx + "/im/files/" + stored;
        String base = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? "" : ":" + request.getServerPort());
        String url = base + path;

        ImUploadVO vo = new ImUploadVO();
        vo.setPath(path);
        vo.setUrl(url);
        vo.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        vo.setSize(file.getSize());
        return Result.success(vo);
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        if (!SAFE_FILE_NAME.matcher(fileName).matches()) {
            return ResponseEntity.notFound().build();
        }
        String ext = extensionOf(fileName);
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.notFound().build();
        }
        Path file = uploadRoot.resolve(fileName).normalize();
        if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file.toFile());
        String probe;
        try {
            probe = Files.probeContentType(file);
        } catch (IOException e) {
            probe = null;
        }
        MediaType media = MediaType.APPLICATION_OCTET_STREAM;
        if (StringUtils.hasText(probe)) {
            try {
                media = MediaType.parseMediaType(probe);
            } catch (Exception ignored) {
                // keep octet-stream
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(media)
                .body(resource);
    }

    private static String extensionOf(String name) {
        if (name == null || !name.contains(".")) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1);
    }
}
