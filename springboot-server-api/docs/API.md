# API 接口文档

在线文档（Swagger UI）：`http://服务器地址:8082/api/swagger-ui.html`  
OpenAPI JSON：`http://服务器地址:8082/api/v3/api-docs`

---

## 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- `code`: 0 表示成功，非 0 表示失败
- `message`: 提示信息
- `data`: 业务数据

---

## 接口列表

### 1. Hello

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/hello | Hello World 示例 |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": "Hello World"
}
```

---

### 2. 注册

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册（支持设备 UUID 自动注册） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceUuid | string | 二选一 | 设备唯一标识，用于自动注册 |
| username | string | 二选一 | 用户名，3-50 位 |
| password | string | 二选一 | 密码，6-32 位 |
| mobile | string | 否 | 手机号 |

> 至少提供 `deviceUuid` 或 `(username + password)`

**方式一：设备 UUID 自动注册**
```json
{
  "deviceUuid": "246a520aae6b7a3c"
}
```

**方式二：账号密码注册**
```json
{
  "username": "testuser",
  "password": "123456",
  "mobile": "13800138000"
}
```

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "1",
    "mobile": "",
    "is_vip": false,
    "vip": "normal",
    "vipFormat": "普通用户",
    "vipExpireTime": 0,
    "token": "xxx",
    "registerTime": 1773508098214,
    "responseTime": 1773508098217
  }
}
```

---

### 3. 登录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户名密码登录 |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应示例**
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
    "registerTime": 1773508098214,
    "responseTime": 1773508098260
  }
}
```

---

### 4. 管理后台 - 个人信息（需 X-Auth-Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/profile | 获取当前用户（用于左上角 "hello xxxx"） |
| PUT | /api/admin/profile/password | 修改密码 |

**请求头**：`X-Auth-Token: <登录后返回的 token>`

**GET /admin/profile 响应**
```json
{
  "code": 0,
  "message": "success",
  "data": { "username": "admin" }
}
```

**PUT /admin/profile/password 请求体**
```json
{
  "oldPassword": "Admin@123",
  "newPassword": "YourNewSecurePassword8"
}
```
新密码需 8-32 位。
