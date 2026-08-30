# Tiamo AI 数据管理系统 - 前后端完整项目

## 项目概览

| 模块 | 技术栈 | 端口 |
|------|--------|------|
| 后端 | Spring Boot 3.2.5 + MyBatis-Plus + JWT + MySQL | 8080 |
| 前端 | 原生 HTML + CSS + JS（AI 科技风格） | 静态页面 |

## 目录结构

```
tiamo-fullstack/
├── tiamo-backend/          # 后端项目
│   ├── pom.xml
│   ├── src/main/java/com/tiamo/
│   │   ├── TiamoApplication.java       # 启动类
│   │   ├── controller/
│   │   │   ├── AuthController.java     # 认证接口（登录/注册/重置密码/验证码）
│   │   │   └── BooksController.java    # 商品数据CRUD接口
│   │   ├── service/ + impl/            # 业务逻辑层
│   │   ├── mapper/                      # 数据访问层
│   │   ├── entity/                      # 实体类（SysUser, Books）
│   │   ├── dto/                         # 数据传输对象
│   │   ├── security/                    # JWT工具、拦截器、验证码服务
│   │   ├── config/                      # Web配置、数据初始化
│   │   └── common/                      # 统一返回、异常处理
│   └── src/main/resources/
│       ├── application.yml              # 应用配置
│       └── sql/schema.sql               # 数据库建表脚本
│
└── tiamo-auth/             # 前端页面
    ├── index.html           # 入口（跳转登录）
    ├── login.html           # 登录页
    ├── register.html        # 注册页
    ├── forgot-password.html # 忘记密码页
    ├── dashboard.html       # 登录后控制台
    ├── css/auth.css         # AI风格样式
    └── js/auth.js           # 交互逻辑 + API封装 + Token管理
```

## 快速开始（三步启动）

### 第一步：初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行建表脚本
source /path/to/tiamo-backend/src/main/resources/sql/schema.sql
```

脚本会自动创建：
- `tiamo_db` 数据库
- `sys_user` 系统用户表
- `books` 商品数据表（含2条测试数据）

### 第二步：启动后端

```bash
cd tiamo-backend

# 修改数据库连接（如需要）
# 编辑 src/main/resources/application.yml 中的 username/password

# 启动
mvn spring-boot:run
```

启动成功后会自动创建默认管理员账号：
- **用户名**: `admin`
- **密码**: `Admin123`

后端接口地址: `http://localhost:8080`

### 第三步：打开前端

直接用浏览器打开 `tiamo-auth/login.html`，或用任意静态服务器托管：

```bash
# 方式一：直接双击打开 login.html
# 方式二：Python 静态服务器
cd tiamo-auth
python3 -m http.server 8081
# 访问 http://localhost:8081
```

使用默认账号 `admin` / `Admin123` 登录。

## API 接口文档

### 认证接口（无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/send-captcha` | 发送短信验证码 |
| POST | `/api/auth/reset-password` | 重置密码（忘记密码） |
| GET | `/api/auth/verify` | 验证 Token 有效性 |

### 商品数据接口（需 Token，请求头携带 `Authorization: Bearer {token}`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/maven/books` | 查询全部商品 |
| GET | `/maven/books/{id}` | 根据ID查询 |
| POST | `/maven/books/` | 新增商品 |
| PUT | `/maven/books/` | 修改商品 |
| DELETE | `/maven/books/{id}` | 删除商品 |

### 登录请求示例

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123"}'
```

响应：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "email": null
  },
  "msg": "登录成功"
}
```

### 带 Token 请求受保护接口

```bash
curl http://localhost:8080/maven/books \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## 认证流程说明

```
用户输入账号密码
       ↓
前端调用 POST /api/auth/login
       ↓
后端校验密码（BCrypt），生成 JWT Token
       ↓
前端保存 Token 到 localStorage
       ↓
后续请求自动在 Header 携带 Authorization: Bearer {token}
       ↓
后端 JwtInterceptor 拦截验证，无效返回 401
```

## 安全特性

- **密码加密**: BCrypt 算法存储，不可逆
- **JWT 认证**: 无状态 Token，默认 24 小时有效期
- **接口拦截**: `/maven/books/**` 全部需要认证
- **验证码**: 注册和重置密码需要短信验证码（5分钟有效，一次性）
- **跨域配置**: 已配置 CORS，支持前端独立部署
- **全局异常**: 统一异常处理，不暴露堆栈信息

## 配置说明

### 后端配置（application.yml）

```yaml
server:
  port: 8080                    # 后端端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tiamo_db  # 数据库地址
    username: root              # 数据库用户名
    password: root              # 数据库密码

jwt:
  secret: your-secret-key       # JWT 密钥（生产环境务必修改）
  expiration: 86400000          # Token 有效期（毫秒），默认24小时

app:
  default-admin:
    username: admin              # 默认管理员用户名
    password: Admin123           # 默认管理员密码
```

### 前端配置（js/auth.js）

```javascript
var API_BASE = 'http://localhost:8080';  // 后端接口地址
var TOKEN_KEY = 'tiamo_token';             // localStorage 中 Token 的 key
```

## 生产部署建议

1. **修改 JWT 密钥**：`application.yml` 中的 `jwt.secret` 改为复杂随机字符串
2. **修改默认管理员密码**：首次登录后立即修改
3. **验证码服务**：当前为内存存储+控制台打印，生产环境接入阿里云/腾讯云短信服务，改用 Redis 存储
4. **HTTPS**：生产环境务必使用 HTTPS，避免 Token 明文传输
5. **Nginx 反向代理**：前端静态文件和后端接口通过同一域名 + Nginx 代理，避免跨域
6. **数据库**：生产环境不要用 root，创建专用账号并限制权限
7. **关闭 SQL 日志**：`mybatis-plus.configuration.log-impl` 注释掉

## 常见问题

**Q: 前端打开后提示"网络请求失败"？**
A: 后端没启动，或者 `js/auth.js` 中的 `API_BASE` 地址不对。确认后端在 8080 端口运行。

**Q: 登录提示"用户名或密码错误"？**
A: 确认数据库已初始化，后端首次启动时会自动创建 admin/Admin123 账号。检查控制台是否有"默认管理员账号创建成功"的日志。

**Q: 验证码收不到？**
A: 开发环境下验证码会打印在后端控制台（【验证码】手机号: xxx 验证码: xxxxxx），同时接口响应中也会返回 captcha 字段，前端会自动填充。生产环境需要接入真实短信服务。

**Q: Token 过期了怎么办？**
A: Token 默认 24 小时有效。过期后调用受保护接口会返回 401，前端自动跳转登录页。可修改 `jwt.expiration` 调整有效期。

## 许可证

MIT
