# 知识库系统

基于 Spring Boot 3 + Vue 3 的全栈知识库管理系统，支持文章管理、分类标签、评论收藏、JWT 鉴权等功能。

## 技术栈

### 后端
- **框架**：Spring Boot 3.2.5
- **ORM**：MyBatis-Plus 3.5.6
- **数据库**：MySQL 8
- **缓存**：Redis
- **安全**：Spring Security + JWT（jjwt 0.12.5）
- **校验**：Spring Validation
- **工具**：Lombok

### 前端
- **框架**：Vue 3.4 + Vite 5
- **UI 组件**：Element Plus 2.6
- **状态管理**：Pinia 2.1
- **路由**：Vue Router 4
- **HTTP 请求**：Axios
- **Markdown 渲染**：marked

---

## 环境要求

| 工具 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

---

## 快速开始

### 第一步：初始化数据库

1. 确保 MySQL 服务已启动
2. 执行初始化脚本：

```bash
mysql -u root -p < knowledge-backend/src/main/resources/sql/init.sql
```

或者手动在 MySQL 客户端中打开并执行 `knowledge-backend/src/main/resources/sql/init.sql`。

> 脚本会自动创建 `knowledge_base` 数据库，并初始化表结构和示例数据。
> 默认超级管理员账号：**admin / admin123**

### 第二步：配置后端

打开 `knowledge-backend/src/main/resources/application.yml`，修改数据库和 Redis 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_base?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root          # 改为你的 MySQL 用户名
    password: root          # 改为你的 MySQL 密码
  data:
    redis:
      host: localhost       # 改为你的 Redis 地址
      port: 6379
      password:             # 改为你的 Redis 密码（无密码留空）
```

### 第三步：启动后端

```bash
cd knowledge-backend
mvn spring-boot:run
```

后端启动后访问：http://localhost:8080

### 第四步：启动前端

```bash
cd knowledge-frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:5173

> 前端已配置代理，`/api` 请求会自动转发到后端 `http://localhost:8080`。

---

## 项目结构

```
D:\projects\mywork\
── knowledge-backend/              # 后端 Spring Boot 项目
│   ├── pom.xml                     # Maven 依赖配置
│   └── src/main/
│       ├── java/com/knowledge/base/
│       │   ├── KnowledgeApplication.java   # 启动类
│       │   ├── common/                     # 通用类（Result 统一响应）
│       │   ├── config/                     # 配置类（Security、CORS、MyBatis-Plus）
│       │   ├── controller/                 # 控制器层
│       │   ├── dto/                        # 数据传输对象
│       │   ├── entity/                     # 实体类
│       │   ├── mapper/                     # MyBatis-Plus Mapper
│       │   ├── security/                   # JWT 工具 + 认证过滤器
│       │   ├── service/                    # 服务层接口
│       │   │   └── impl/                   # 服务层实现
│       │   └── vo/                         # 视图对象
│       └── resources/
│           ├── application.yml             # 应用配置
│           └── sql/init.sql                # 数据库初始化脚本
│
├── knowledge-frontend/             # 前端 Vue 3 项目
│   ├── package.json
│   ├── vite.config.js              # Vite 配置（含代理）
│   ├── index.html
│   └── src/
│       ├── main.js                 # 入口文件
│       ├── App.vue                 # 根组件
│       ├── api/index.js            # API 请求封装
│       ├── assets/styles/          # 全局样式
│       ├── router/index.js         # 路由配置（含导航守卫）
│       ├── store/user.js           # Pinia 用户状态管理
│       ├── utils/request.js        # Axios 封装（拦截器）
│       └── views/                  # 页面组件
│           ├── Login.vue           # 登录页
│           ├── Register.vue        # 注册页
│           ├── Home.vue            # 首页（文章列表 + 搜索 + 分类 + 标签）
│           ├── ArticleDetail.vue   # 文章详情页（评论 + 收藏 + 点赞）
│           ├── ArticleCreate.vue   # 写文章页
│           ├── ArticleEdit.vue     # 编辑文章页
│           ├── Profile.vue         # 个人中心
│           ── CategoryManage.vue  # 分类管理（仅管理员）
│
└── README.md                       # 本文件
```

---

## 功能模块

### 用户模块
- 注册 / 登录（JWT Token 鉴权）
- 角色权限：超级管理员（SUPER_ADMIN）/ 管理员（ADMIN）/ 普通用户（USER）
- 邮箱验证码注册（一个邮箱最多注册 5 个账号）
- 邮箱验证码重置密码
- 用户管理（超级管理员专属）：角色调整、拉黑 / 解除拉黑
- Token 自动注入请求头，过期自动跳转登录

### 文章模块
- 文章列表（分页、搜索、分类筛选、标签筛选）
- 文章详情（Markdown 渲染、阅读量统计）
- 创建 / 编辑 / 删除文章
- 点赞功能

### 分类与标签
- 多级分类管理（管理员专属）
- 标签管理
- 文章支持多标签

### 评论模块
- 文章评论
- 删除评论
- 评论数量统计

### 收藏模块
- 收藏 / 取消收藏文章
- 个人中心查看收藏列表

### 个人中心
- 查看收藏列表
- 查看账号信息

---

## API 接口一览

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 否 |
| POST | `/api/auth/register` | 注册 | 否 |
| GET | `/api/articles` | 文章列表（分页） | 否 |
| GET | `/api/articles/{id}` | 文章详情 | 否 |
| POST | `/api/articles` | 创建文章 | 是 |
| PUT | `/api/articles/{id}` | 更新文章 | 是 |
| DELETE | `/api/articles/{id}` | 删除文章 | 是 |
| POST | `/api/articles/{id}/like` | 点赞文章 | 是 |
| GET | `/api/categories` | 分类列表 | 否 |
| POST | `/api/categories` | 创建分类 | 是 |
| PUT | `/api/categories/{id}` | 更新分类 | 是 |
| DELETE | `/api/categories/{id}` | 删除分类 | 是 |
| GET | `/api/tags` | 标签列表 | 否 |
| POST | `/api/tags` | 创建标签 | 是 |
| DELETE | `/api/tags/{id}` | 删除标签 | 是 |
| GET | `/api/comments/article/{articleId}` | 文章评论列表 | 否 |
| POST | `/api/comments` | 发表评论 | 是 |
| DELETE | `/api/comments/{id}` | 删除评论 | 是 |
| POST | `/api/favorites/toggle/{articleId}` | 收藏/取消收藏 | 是 |
| GET | `/api/favorites/check/{articleId}` | 检查是否收藏 | 是 |
| GET | `/api/favorites/my` | 我的收藏列表 | 是 |

---

## 常见问题

### 1. 后端启动报错：无法连接 MySQL
- 检查 MySQL 服务是否启动
- 检查 `application.yml` 中的用户名和密码是否正确
- 确保已执行 `init.sql` 初始化数据库

### 2. 前端请求 401 错误
- Token 过期，重新登录即可
- 检查后端 JWT 配置中的 `secret` 是否一致

### 3. Redis 连接失败
- 检查 Redis 服务是否启动
- 如果不需要 Redis 缓存，可以暂时注释掉 `spring-boot-starter-data-redis` 依赖

### 4. 前端页面空白
- 检查 Vite 代理配置是否正确（`vite.config.js`）
- 打开浏览器控制台查看具体报错信息

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |
