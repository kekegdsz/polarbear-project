# MySQL 持久化配置

## 1. 在宝塔设置 MySQL 密码

1. 登录宝塔面板
2. 左侧 **数据库** → 点击 **root** 后的 **改密**
3. 将密码设为：`PA4HKdcDZ7jRkj99`（或与 `application-prod.yml` 中一致）

或命令行：`bt` → 选择 `(7) 强制修改MySQL密码`

## 2. 创建数据库

```bash
mysql -uroot -pPA4HKdcDZ7jRkj99 -e "CREATE DATABASE IF NOT EXISTS springboot_api DEFAULT CHARACTER SET utf8mb4;"
```

## 3. 启动应用（生产环境）

```bash
cd /opt/apps/springboot-server-api
export MYSQL_PASSWORD=PA4HKdcDZ7jRkj99
./start-prod.sh
```

或直接指定密码：

```bash
MYSQL_PASSWORD=PA4HKdcDZ7jRkj99 ./start-prod.sh
```

或使用 `application-prod.yml` 中配置的默认密码。
