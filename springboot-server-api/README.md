# springboot-server-api

Spring Boot REST API 项目，遵循分层架构规范，统一响应格式。

## 技术栈

- Java 17
- Spring Boot 4.0.3
- Maven

## 项目结构

```
src/main/java/com/undersky/api/springbootserverapi/
├── config/              # 配置类
├── controller/          # 控制器层
├── service/             # 业务逻辑层
├── repository/          # 数据访问层
├── model/
│   ├── dto/             # 数据传输对象
│   ├── entity/          # 实体类
│   └── vo/              # 视图对象
├── exception/           # 全局异常处理
└── constant/            # 常量定义
```

## 快速开始

### 本地运行

```bash
./mvnw spring-boot:run
```

默认端口 8081，访问：http://localhost:8081/api/hello

### 打包

```bash
./mvnw clean package -DskipTests
```

## API 说明

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/hello | Hello World 示例 |
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |

### 注册请求示例

**方式一：设备 UUID 自动注册**（无需账号密码）
```json
POST /api/auth/register
{
  "deviceUuid": "246a520aae6b7a3c"
}
```
可附带可选手机号：`{"deviceUuid": "xxx", "mobile": "13800138000"}`

**方式二：账号密码注册**
```json
POST /api/auth/register
{
  "username": "testuser",
  "password": "123456",
  "mobile": "13800138000"
}
```
可附带 deviceUuid 绑定设备

### 登录请求示例

```json
POST /api/auth/login
{
  "username": "testuser",
  "password": "123456"
}
```

### 登录/注册响应（参考 vlserver）

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "1",
    "mobile": "13800138000",
    "is_vip": false,
    "vip": "normal",
    "vipFormat": "普通用户",
    "vipExpireTime": 0,
    "token": "xxx",
    "registerTime": 1773507676705,
    "responseTime": 1773507676868
  }
}
```

### 统一响应格式（code 0 为成功）

```json
{
  "code": 0,
  "message": "success",
  "data": "Hello World"
}
```

- `code`: 0 表示成功，非 0 表示失败
- `message`: 提示信息
- `data`: 业务数据

## 部署

- **[宝塔面板部署指南](docs/宝塔部署指南.md)**：详细部署步骤
- **一键部署**：`./scripts/deploy.sh root@你的服务器IP`

## 文档

| 文档 | 说明 |
|------|------|
| [API 接口文档](docs/API.md) | 接口说明、请求示例 |
| [宝塔部署指南](docs/宝塔部署指南.md) | 宝塔面板部署、运维命令 |

**在线 Swagger UI**：`/api/swagger-ui.html`
