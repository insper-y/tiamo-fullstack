# 分销商品数据管理系统 - 后台服务

为 [https://tiamo-zeng.github.io](https://tiamo-zeng.github.io) 前端页面配套的完整后端服务，包含数据库设计。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Spring Boot 3 要求 |
| Spring Boot | 3.2.5 | Web 框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架（MyBatis 增强版） |
| MySQL | 8.0+ | 数据库 |
| Lombok | - | 简化实体类 |

## 项目结构

```
tiamo-backend/
├── pom.xml
├── src/main/java/com/tiamo/
│   ├── TiamoApplication.java          # 启动类
│   ├── config/
│   │   └── CorsConfig.java            # 跨域配置
│   ├── controller/
│   │   └── BooksController.java       # 数据接口控制器
│   ├── entity/
│   │   └── Books.java                 # 分销商品数据实体
│   ├── mapper/
│   │   └── BooksMapper.java           # 数据访问层
│   ├── service/
│   │   ├── BooksService.java          # 业务接口
│   │   └── impl/
│   │       └── BooksServiceImpl.java  # 业务实现
│   └── common/
│       ├── Result.java                # 统一返回结果
│       ├── Code.java                  # 状态码常量
│       ├── BusinessException.java     # 业务异常
│       └── GlobalExceptionHandler.java # 全局异常处理
└── src/main/resources/
    ├── application.yml                 # 应用配置
    └── sql/
        └── schema.sql                  # 数据库建表脚本
```

## 快速开始

### 1. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行建表脚本
source /path/to/tiamo-backend/src/main/resources/sql/schema.sql
```

或直接在 Navicat / DBeaver 等工具中打开 `schema.sql` 执行。

### 2. 修改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tiamo_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root        # 改成你的 MySQL 用户名
    password: root        # 改成你的 MySQL 密码
```

### 3. 启动服务

```bash
# 方式一：Maven 启动
cd tiamo-backend
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/tiamo-backend-1.0.0.jar

# 方式三：IDEA 中直接运行 TiamoApplication.main()
```

启动成功后控制台会显示：

```
==================================================
  分销商品数据管理系统 启动成功!
  接口地址: http://localhost:8080/maven/books
  数据库:   MySQL (tiamo_db)
==================================================
```

## 接口文档

所有接口基础路径：`http://localhost:8080/maven/books`

### 查询全部数据

```
GET /maven/books
```

**响应示例：**
```json
{
  "code": 20041,
  "data": [
    {
      "id": 1,
      "name": "分销助手A",
      "type": "wx_account_001",
      "description": "soft_acc_001",
      "aa": "客户张三",
      "bd": "10001",
      "ac": "https://example.com/product/10001",
      "ab": "https://example.com/img/10001.jpg",
      "ax": "示例商品标题1"
    }
  ],
  "msg": "查询成功"
}
```

### 根据ID查询

```
GET /maven/books/{id}
```

### 新增数据

```
POST /maven/books/
Content-Type: application/json

{
  "name": "分销软件名称",
  "type": "微信账号",
  "description": "软件账号",
  "aa": "微信备注名",
  "bd": "商品ID",
  "ac": "商品链接",
  "ab": "商品主图URL",
  "ax": "商品标题"
}
```

### 修改数据

```
PUT /maven/books/
Content-Type: application/json

{
  "id": 1,
  "name": "修改后的名称",
  ...
}
```

### 删除数据

```
DELETE /maven/books/{id}
```

## 状态码说明

| 状态码 | 含义 |
|--------|------|
| 20011 | 新增成功 |
| 20010 | 新增失败 |
| 20021 | 删除成功 |
| 20020 | 删除失败 |
| 20031 | 修改成功 |
| 20030 | 修改失败 |
| 20041 | 查询成功 |
| 20040 | 查询失败 |
| 50000 | 系统异常 |

## 前端对接说明

前端 `login.html` 中当前 API 地址为 `https://te.zssmh.asia/maven/books`，替换为你的后端地址即可：

```javascript
// 替换前
axios.get("https://te.zssmh.asia/maven/books")

// 替换后（本地开发）
axios.get("http://localhost:8080/maven/books")

// 替换后（线上部署，改成你的域名）
axios.get("https://your-domain.com/maven/books")
```

> 全局搜索 `https://te.zssmh.asia/maven` 一次性替换所有出现的地方。

## 数据库表结构

### books（分销商品数据表）

| 字段 | 类型 | 说明 | 对应前端字段 |
|------|------|------|-------------|
| id | INT | 主键，自增 | id |
| name | VARCHAR(255) | 分销软件 | name |
| type | VARCHAR(255) | 微信账号 | type |
| description | TEXT | 软件账号 | description |
| aa | VARCHAR(255) | 微信备注名 | aa |
| bd | VARCHAR(255) | 商品ID | bd |
| ac | VARCHAR(500) | 商品链接 | ac |
| ab | VARCHAR(500) | 商品主图 | ab |
| ax | VARCHAR(500) | 商品标题 | ax |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |
| deleted | TINYINT | 逻辑删除标记 | - |

### sys_user（系统用户表，预留）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(100) | 用户名（唯一） |
| password | VARCHAR(255) | BCrypt 加密密码 |
| nickname | VARCHAR(100) | 昵称 |
| status | TINYINT | 状态 0-禁用 1-启用 |

## 生产部署建议

1. **Nginx 反向代理**：将后端接口通过 Nginx 代理，统一域名和 HTTPS
2. **数据库密码**：生产环境不要用 root，创建专用账号并限制权限
3. **跨域配置**：`CorsConfig` 中 `allowedOriginPatterns("*")` 改为具体前端域名
4. **日志**：生产环境关闭 SQL 日志（`mybatis-plus.configuration.log-impl`）
5. **JWT 认证**：后续可在 `sys_user` 表基础上扩展登录认证功能

## 许可证

MIT
