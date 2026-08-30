# Tiamo AI 系统 - GitHub 部署完整指南

## 部署架构

```
用户浏览器
    │
    ▼
GitHub Pages（前端静态页面）  ◄──  自动部署（GitHub Actions）
    │
    │  API 请求（HTTPS）
    ▼
Render（后端 Spring Boot）    ◄──  Docker 自动部署
    │
    ▼
免费 MySQL 数据库（Aiven/Clever Cloud）
```

---

## 第一步：创建 GitHub 仓库并上传代码

### 1.1 注册/登录 GitHub

访问 https://github.com，注册账号（已有则跳过）。

### 1.2 创建新仓库

1. 点击右上角 `+` → `New repository`
2. 仓库名填：`tiamo-fullstack`
3. 选择 `Public`（公开，GitHub Pages 免费）
4. **不要**勾选 "Initialize this repository with"（避免冲突）
5. 点击 `Create repository`

### 1.3 上传代码

在电脑上打开终端（Windows 用 Git Bash，Mac 用终端），执行：

```bash
# 进入解压后的项目目录
cd tiamo-fullstack

# 初始化 Git
git init
git add .
git commit -m "Initial commit: Tiamo AI fullstack project"

# 关联远程仓库（把 你的用户名 改成你的 GitHub 用户名）
git branch -M main
git remote add origin https://github.com/你的用户名/tiamo-fullstack.git

# 推送
git push -u origin main
```

> 如果提示输入用户名密码，密码处填 **Personal Access Token**（不是登录密码）。
> 获取 Token：GitHub 右上角头像 → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token，勾选 `repo` 权限。

---

## 第二步：部署前端到 GitHub Pages

### 2.1 开启 GitHub Pages

1. 进入仓库页面 → 顶部 `Settings`
2. 左侧菜单 → `Pages`
3. `Source` 选择 `GitHub Actions`
4. 保存

### 2.2 修改前端 API 地址

部署前需要把前端的 API 地址改成你后端的地址（后端还没部署，先记下这一步，等后端部署完再回来改）。

编辑 `tiamo-auth/js/config.js`：

```javascript
window.APP_CONFIG = {
    // 改成你 Render 后端的地址（第三步部署后会得到）
    API_BASE: 'https://tiamo-backend.onrender.com',
    ...
};
```

改完后提交推送：

```bash
git add tiamo-auth/js/config.js
git commit -m "chore: update API base URL for production"
git push
```

### 2.3 自动部署

代码推送到 `main` 分支后，GitHub Actions 会自动部署前端（`.github/workflows/deploy-pages.yml` 已配置好）。

查看部署状态：仓库页面 → 顶部 `Actions` → 看到绿色对勾就是成功。

部署成功后，前端访问地址：
```
https://你的用户名.github.io/tiamo-fullstack/
```

---

## 第三步：部署后端到 Render（免费）

Render 提供免费的 Web Service 托管，每月 750 小时（个人用足够）。

### 3.1 注册 Render

访问 https://render.com，用 GitHub 账号登录（授权访问你的仓库）。

### 3.2 准备免费 MySQL 数据库

后端需要 MySQL，推荐用 **Aiven** 免费版：

1. 访问 https://aiven.io，注册账号
2. 创建服务 → 选择 `MySQL`
3. 计划选 `Free`（免费，30天试用期，之后可继续免费或升级）
4. 云服务商选 `Google Cloud`，区域选离你近的（如 `asia-east1` 台湾）
5. 创建后，在服务详情页找到连接信息：
   - Host
   - Port
   - User
   - Password
   - Database name（默认是 `defaultdb`，可以改成 `tiamo_db`）

6. 用 Navicat/DBeaver 连接这个数据库，执行 `tiamo-backend/src/main/resources/sql/schema.sql` 建表。

### 3.3 在 Render 部署后端

1. Render 控制台 → 右上角 `New +` → `Web Service`
2. 选择你刚才的 GitHub 仓库 `tiamo-fullstack`
3. 配置：
   - **Name**: `tiamo-backend`（这个会成为你的域名一部分）
   - **Region**: 选离你数据库近的（如 `Singapore`）
   - **Branch**: `main`
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `./tiamo-backend/Dockerfile`
   - **Docker Build Context Directory**: `./tiamo-backend`
   - **Instance Type**: `Free`
4. 点击 `Advanced` → 添加环境变量（Environment Variables）：

| Key | Value | 说明 |
|-----|-------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://你的Host:你的Port/tiamo_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true` | Aiven 的数据库连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 你的数据库用户名 | Aiven 的 User |
| `SPRING_DATASOURCE_PASSWORD` | 你的数据库密码 | Aiven 的 Password |
| `JWT_SECRET` | 随便写一串至少32位的字符 | 如 `my-secret-key-for-tiamo-ai-2024-abcdef` |
| `APP_LOG_EMAIL_ENABLED` | `false` | 先关闭邮件推送，后面再开 |

5. 点击 `Create Web Service`
6. 等待部署完成（第一次约 3-5 分钟，看到 `Your service is live` 就是成功）

部署成功后，后端地址：
```
https://tiamo-backend.onrender.com
```

> 注意：Render 免费版 15 分钟无请求会休眠，第一次访问可能需要等 30 秒唤醒。

### 3.4 验证后端

浏览器访问：
```
https://tiamo-backend.onrender.com/api/auth/verify
```
返回 `{"code":401,...}` 就是正常的（因为没带 Token）。

---

## 第四步：联调前后端

### 4.1 修改前端 API 地址

回到第二步 2.2，把 `config.js` 中的 `API_BASE` 改成：
```javascript
API_BASE: 'https://tiamo-backend.onrender.com',
```

提交推送：
```bash
git add tiamo-auth/js/config.js
git commit -m "chore: update API base URL to Render"
git push
```

GitHub Actions 会自动重新部署前端。

### 4.2 测试完整流程

1. 访问前端：`https://你的用户名.github.io/tiamo-fullstack/`
2. 用默认账号登录：`admin` / `Admin123`
3. 登录成功跳转到控制台
4. 点击「操作日志」查看日志
5. 试试新增/修改/删除商品数据

---

## 第五步（可选）：开启操作日志邮件推送

### 5.1 获取邮箱授权码

以 QQ 邮箱为例：
1. 登录 QQ 邮箱 → 设置 → 账户
2. 开启「IMAP/SMTP服务」
3. 生成授权码（16位）

### 5.2 在 Render 添加环境变量

Render 后台 → 你的服务 → `Environment` → 添加：

| Key | Value |
|-----|-------|
| `APP_LOG_EMAIL_ENABLED` | `true` |
| `SPRING_MAIL_HOST` | `smtp.qq.com` |
| `SPRING_MAIL_PORT` | `465` |
| `SPRING_MAIL_USERNAME` | `你的邮箱@qq.com` |
| `SPRING_MAIL_PASSWORD` | `你的16位授权码` |
| `APP_LOG_EMAIL_TO` | `接收日志的邮箱@qq.com` |

保存后 Render 会自动重新部署。之后每次有人操作，你的邮箱就会收到通知。

---

## 常见问题

### Q: GitHub Pages 访问 404？
A: 检查仓库 Settings → Pages 是否选了 GitHub Actions，以及 Actions 部署是否成功。

### Q: 后端部署失败？
A: Render 后台 → 你的服务 → `Logs` 查看报错。常见原因：
- 数据库连接信息错误
- 数据库没建表
- 环境变量没配全

### Q: 前端请求后端跨域？
A: 后端已配置全局 CORS（`WebConfig.java`），允许所有来源。如果仍有问题，检查 Render 服务是否正常运行。

### Q: Render 免费版休眠导致首次访问慢？
A: 正常现象，15 分钟无请求会休眠，唤醒约 30 秒。可以用免费监控服务（如 UptimeRobot）每 5 分钟 ping 一次保持唤醒。

### Q: 想绑定自己的域名？
A: GitHub Pages：仓库 Settings → Pages → Custom domain 填你的域名，然后在域名服务商添加 CNAME 记录指向 `你的用户名.github.io`。
Render：后台 → 你的服务 → `Custom Domains` → 添加域名，按提示配置 DNS。

---

## 部署文件清单

```
tiamo-fullstack/
├── .github/
│   └── workflows/
│       └── deploy-pages.yml      # GitHub Actions 自动部署前端
├── tiamo-backend/
│   ├── Dockerfile                 # 后端 Docker 镜像构建
│   ├── .dockerignore
│   └── src/main/resources/sql/schema.sql  # 数据库建表脚本
├── tiamo-auth/
│   ├── .nojekyll                  # GitHub Pages 不处理 Jekyll
│   └── js/config.js               # 前端 API 地址配置
├── render.yaml                    # Render 部署配置（参考）
├── .gitignore
└── DEPLOY.md                      # 本文档
```

---

## 成本估算

| 服务 | 费用 | 说明 |
|------|------|------|
| GitHub Pages | 免费 | 静态页面托管 |
| GitHub Actions | 免费 | 公开仓库无限分钟数 |
| Render Web Service | 免费 | 每月750小时，15分钟休眠 |
| Aiven MySQL | 免费/试用 | 免费版30天，之后$0/月（有限额） |
| 域名（可选） | 约 ¥50/年 | 不绑域名用免费子域名即可 |

**总计：0 元即可跑起来！**
