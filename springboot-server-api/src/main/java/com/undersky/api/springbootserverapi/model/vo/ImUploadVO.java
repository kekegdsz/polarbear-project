package com.undersky.api.springbootserverapi.model.vo;

/**
 * IM 媒体上传结果：客户端将 path 或 url 写入消息 JSON body。
 */
public class ImUploadVO {

    /** 相对浏览器路径，如 /api/im/files/{uuid}.jpg */
    private String path;
    /** 绝对 URL（便于客户端直接加载；反向代理后可能与外网域名不一致时可只用 path） */
    private String url;
    private String contentType;
    private long size;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
