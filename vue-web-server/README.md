# App 分发官网

基于 Vue 3 + Vite 的 App 分发下载站，支持 iOS 与 Android 双平台。

## 功能

- 首页展示应用信息与下载入口
- 平台选择页（iOS / Android）
- 更新日志页
- 响应式布局，深色主题

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务
npm run dev

# 构建生产版本
npm run build
```

## 配置

在 `src/config/app.js` 中修改：

- `name` / `shortName`：应用名称
- `download.ios.url`：iOS 安装链接（itms-services 或直链）
- `download.android.url`：Android APK 下载链接

## 目录结构

```
src/
├── config/app.js    # 应用配置
├── components/      # 公共组件
├── views/           # 页面视图
├── router/          # 路由
└── style.css        # 全局样式
```
