# 宝塔面板部署指南

## 一、本地构建

在项目目录执行：

```bash
npm install
npm run build
```

构建完成后会生成 `dist` 文件夹，里面是可直接部署的静态文件。

---

## 二、上传到服务器

### 方式 A：宝塔文件管理器

1. 登录宝塔面板 → **文件**
2. 进入网站根目录（如 `/www/wwwroot/你的域名/`）
3. 将本地 `dist` 文件夹**内的所有文件**上传（不是 dist 文件夹本身）

### 方式 B：FTP / SFTP

用 FileZilla、Cyberduck 等工具连接服务器，把 `dist` 里的内容上传到网站根目录。

### 方式 C：scp 命令（需要 SSH）

```bash
# 替换 USER、IP、/path/to/site 为你的实际信息
scp -r dist/* root@你的服务器IP:/www/wwwroot/你的站点目录/
```

---

## 三、宝塔添加站点

1. **网站** → **添加站点**
2. 域名：填写你的域名或 IP
3. 根目录：如 `/www/wwwroot/你的域名`
4. PHP 版本：选「纯静态」即可

---

## 四、Nginx 配置（重要）

Vue Router 使用 history 模式，需要 Nginx 把未匹配路由都转发到 `index.html`。

在宝塔：**网站** → 你的站点 → **设置** → **配置文件**，在 `server { }` 块内找到 `location /`，修改为：

```nginx
location / {
    root   /www/wwwroot/你的站点目录;   # 改成你的实际路径
    index  index.html;
    try_files $uri $uri/ /index.html;
}
```

保存后重载 Nginx。

---

## 五、访问验证

浏览器访问 `http://你的域名或IP`，应能看到 App 分发页面。

---

## 六、注意事项

- **不要**在聊天或文档里发送服务器密码
- 确保网站根目录下存在 `index.html`，否则无法访问
- 如遇 404，检查 `try_files $uri $uri/ /index.html;` 是否已添加
- 需 HTTPS 时，在宝塔为该站点申请 SSL 证书即可
