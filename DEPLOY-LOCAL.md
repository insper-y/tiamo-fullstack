# Tiamo AI 本地部署说明（2026-09-11）

> 由豆包完成的本机部署记录。当前系统已在本地完整跑通。

## 运行状态

| 组件 | 技术 | 端口 | 状态 |
|------|------|------|------|
| 前端 | 原生 HTML/JS 静态页面 | **8083** | ✅ 运行中 |
| 后端 | Spring Boot 3.2.5 (JDK 17) | **8082** | ✅ 运行中 |
| 数据库 | MySQL 8.0.46 (tiamo_db) | 3306 | ✅ 运行中 |
| 缓存 | Redis 6.0 | 6379 | ✅ 运行中 |

## 访问方式

- 前端入口：`http://127.0.0.1:8083/login.html`
- 后端接口：`http://127.0.0.1:8082`（如 `/api/auth/verify`）
- 默认账号：`admin` / `Admin123`

## 部署时做的调整（相对 GitHub 仓库）

1. **环境依赖**：安装了 JDK 17、Maven 3.6.3、MySQL 8.0.46、Redis 6.0（apt 安装，源已切阿里云镜像）
2. **数据库**：导入 `tiamo-backend/src/main/resources/sql/schema.sql`，创建 `tiamo_db` 库（sys_user / books / sys_operation_log 等表 + 2 条测试数据）
3. **端口**：环境自带 nginx 占用 8080/8081，后端改用 **8082**，前端静态服务用 **8083**
4. **后端连接**：通过环境变量覆盖 `DB_URL / DB_USERNAME / DB_PASSWORD` 指向本地 MySQL（root/root）
5. **前端 API 地址**：`tiamo-auth/js/config.js` 的 `API_BASE` 改为 `http://127.0.0.1:8082`
   - 原文件备份为 `tiamo-auth/js/config.js.codespaces.bak`
   - 线上（Codespaces）地址：`https://ubiquitous-space-happiness-r4xqx9vq57v62x459-8080.app.github.dev`
6. **Bug 修复**：`schema.sql` 的 `sys_user` 表补充了 `role` 列（与实体类 `SysUser.java` 一致，否则启动报 `Unknown column 'role'`）；线上已有数据库也需执行 `ALTER TABLE sys_user ADD COLUMN role TINYINT DEFAULT 0;`

## 重启命令

```bash
# 后端（8082）
cd /home/user/Doubao/chats/8596141988320514/tiamo-fullstack/tiamo-backend
DB_URL='jdbc:mysql://127.0.0.1:3306/tiamo_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' \
DB_USERNAME=root DB_PASSWORD=root \
nohup java -jar target/tiamo-backend-1.0.0.jar --server.port=8082 > /tmp/tiamo-backend.log 2>&1 &

# 前端（8083）
cd /home/user/Doubao/chats/8596141988320514/tiamo-fullstack/tiamo-auth
nohup python3 -m http.server 8083 --bind 0.0.0.0 > /tmp/tiamo-frontend.log 2>&1 &

# MySQL / Redis
sudo service mysql start
sudo service redis-server start
```

## 切换回线上（Codespaces）

把 `tiamo-auth/js/config.js` 中 `API_BASE` 改回：
```javascript
API_BASE: 'https://ubiquitous-space-happiness-r4xqx9vq57v62x459-8080.app.github.dev',
```
或直接 `cp tiamo-auth/js/config.js.codespaces.bak tiamo-auth/js/config.js`

## 已验证流程

- ✅ 后端启动成功（Tomcat 8082，默认管理员创建成功）
- ✅ `POST /api/auth/login`（admin/Admin123）返回 JWT
- ✅ 携带 Token 访问 `/maven/books` 返回 2 条商品数据
- ✅ 浏览器实际登录 → 跳转控制台 → 商品列表展示正常
