# PolarBear 项目文档

本目录为仓库根级文档，汇总 **springboot-server-api**（后端 API）与 **vue-web-server**（前端站点）的说明与运维要点。各子项目内仍有更细的专题文档，可按需对照。

## 文档索引

| 文档 | 说明 |
|------|------|
| [项目文档.md](./项目文档.md) | 整体架构、技术栈、功能与模块、接口概览、本地开发 |
| [部署文档.md](./部署文档.md) | 前后端生产部署、Nginx、数据库、常见问题与排查 |

## 子项目内文档（原始出处）

**后端**（`springboot-server-api/docs/`）

- [API.md](../springboot-server-api/docs/API.md) — 接口说明与示例
- [宝塔部署指南.md](../springboot-server-api/docs/宝塔部署指南.md) — 宝塔 / systemd / Nginx 细节
- [MySQL配置说明.md](../springboot-server-api/docs/MySQL配置说明.md) — 数据库与生产启动说明

**前端**（`vue-web-server/`）

- [README.md](../vue-web-server/README.md) — 功能与本地命令
- [DEPLOY.md](../vue-web-server/DEPLOY.md) — 静态站点部署
- [doc/](../vue-web-server/doc/) — 80 端口放行、502 排查等运维笔记
