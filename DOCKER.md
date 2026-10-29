# Docker 一键启动

> 目标：从「装 JDK + 装 MySQL + 建库 + 导 SQL + 装 Redis + npm install + 开两个终端」变成**一条命令**。

---

## 新增了哪些文件

| 文件 | 作用 |
|---|---|
| `docker-compose.yml` | 编排 4 个服务 + 依赖顺序 + 数据卷 |
| `.env` / `.env.example` | 本机真实配置（已加入 `.gitignore`，不入库） |
| `knowledge-backend/Dockerfile` | 多阶段构建：Maven 编译 → JRE 运行 |
| `knowledge-backend/.dockerignore` | 排除 target 和 `application-local.yml`（防止密码打进镜像） |
| `knowledge-frontend/Dockerfile` | Node 构建 → Nginx 托管静态文件 |
| `knowledge-frontend/nginx.conf` | 托管 SPA + 反向代理 `/api` + SSE 不缓冲 |
| `knowledge-frontend/.dockerignore` | 排除 node_modules / dist |
| `docker/mysql/Dockerfile` | 基于 mysql:8.0，把 `charset.cnf` 打进镜像 |
| `docker/mysql/charset.cnf` | 强制 utf8mb4（不然中文入库变乱码） |

**原有代码一行没动**，删掉这几个文件就回到原状态。

---

## 三步跑起来

1. **启动 Docker Desktop**（任务栏托盘图标变成绿色 / 鲸鱼不再转圈）
2. 在 `D:\projects\mywork\KnowledgeSystem` 目录打开终端：

```bash
docker compose up -d --build
```

3. 浏览器打开 **http://localhost:8000**，账号 `admin` / `admin123`

> 第一次会慢（拉镜像 + Maven 下依赖，10~20 分钟）。之后再启动是秒级。

---

## 端口对照

| 服务 | 容器内 | 宿主机 | 说明 |
|---|---|---|---|
| 前端 | 80 | **8000** | 你只需要记这个 |
| 后端 | 8080 | 8080 | 直接调接口 / 看日志时用 |
| MySQL | 3306 | **13306** | 避开本机的两个 MySQL 实例（3306 / 3307 都被占用） |
| Redis | 6379 | **6380** | 避开你本机的 Redis |

> 容器之间用**服务名**互访（`mysql:3306`、`redis:6379`、`backend:8080`），不是 `localhost`。
> 从宿主机（比如 Navicat）连进去才用 `127.0.0.1:13306`。

---

## 启动时自动发生了什么

1. `mysql` 容器初始化 → 执行挂载进去的两个脚本：
   `01_init.sql`（建库建表 + 超级管理员）→ `02_seed.sql`（演示文章数据）
2. 健康检查通过后，`backend` 才开始启动（`depends_on: condition: service_healthy`）
3. `backend` 用环境变量连 MySQL/Redis（`application-local.yml` 已被排除，不参与）
4. `frontend` 用 Nginx 托管打包后的静态文件，`/api/` 反代到 `backend:8080`

> **为什么没把整个 `sql/` 目录挂进去？** 因为 `init.sql` 已经是「最新完整表结构」，而
> `migration_*.sql` 里是 `ALTER TABLE ADD COLUMN`——在新库上会报 `Duplicate column name`，
> 导致 MySQL 初始化直接失败。migration 只用于升级**已有旧库**，全新容器不需要。
>
> 另：`seed_demo_data.sql` 只插了文章/评论/点赞，没插用户，所以演示文章的作者昵称可能是空的，
> 注册几个账号发几篇自己的文章就正常了。

---

## 常用命令

```bash
docker compose up -d --build     # 构建并后台启动
docker compose ps                # 看 4 个容器状态
docker compose logs -f backend   # 盯后端日志（排错第一现场）
docker compose restart backend   # 只重启后端
docker compose down              # 停止并删除容器（数据卷保留）
docker compose down -v           # 连数据一起清空（会重新跑一遍 init.sql）
```

---

## 常见问题

| 现象 | 原因 / 解法 |
|---|---|
| `dockerDesktopLinuxEngine` 连不上 | Docker Desktop 没启动，托盘图标变绿再试 |
| `Virtualization support not detected` | Windows 功能没开：管理员执行 `wsl --install --no-distribution` 后重启 |
| 拉镜像报 `403 Forbidden`（阿里云 mirror） | 个人加速器过期，删掉 `~/.docker/daemon.json` 里的 `registry-mirrors` |
| 构建时报 `auth.docker.io` 超时 | BuildKit 不走 Docker Desktop 的代理；先 `docker pull` 基础镜像再从本地构建 |
| `port is already allocated` | 13306 / 6380 / 8000 / 8080 被占用，改 `docker-compose.yml` 左边那个数字 |
| 数据库里中文变 `è‡ªåŠ¨` 乱码 | 客户端按 latin1 导入。见下方「踩坑记录」第 4 条 |
| 后端起来了但连不上数据库 | 看 `docker compose logs mysql`，多半是初始化脚本报错；`down -v` 重来一次 |
| 改了代码但页面没变 | 镜像不会自动重建，要 `docker compose up -d --build` |
| 前端刷新页面 404 | `nginx.conf` 里的 `try_files ... /index.html` 已经处理了 history 模式 |
| AI 问答不流式 / 卡住 | `nginx.conf` 里已关 `proxy_buffering`；另外要在 `.env` 填 `AI_DEEPSEEK_API_KEY` 和 `AI_SILICONFLOW_API_KEY` |
| 注册收不到验证码 | `.env` 里 `MAIL_*` 留空会失败，填 QQ 邮箱授权码即可 |

---

## 踩坑记录（2026-10-28 首次跑通）

1. **镜像源 403**：`daemon.json` 里配的阿里云个人加速器已失效（`iy2n45qp.mirror.aliyuncs.com` 返回 403）。
   → 删掉 `registry-mirrors`，改用 Docker Desktop 的 HTTP 代理（Settings → Resources → Proxies）直连 Docker Hub。
2. **BuildKit 不走代理**：`docker pull` 能成功，但 `compose build` 解析 `auth.docker.io` 超时。
   → 先 `docker pull` 四个基础镜像（maven / temurin / node / nginx），镜像在本地后构建直接命中缓存。
3. **3307 端口冲突**：本机跑了两个 mysqld（3306 和 3307）。→ 改用 13306。
4. **中文乱码（最容易漏的一个）**：`init.sql` 是 UTF-8，但官方镜像里的 mysql 客户端默认按 latin1 连接，
   导入后库里存的是 `C3A8E280A1…`（UTF-8 字节被当成 latin1 又编码了一次）。
   修法有两层：
   - 客户端/服务端都锁 utf8mb4 —— 但**不能用 bind mount**：Windows 挂进去的文件是 777，
     MySQL 会警告 `World-writable config file ... is ignored` 并跳过；
   - 所以改成 `docker/mysql/Dockerfile` 里 `COPY charset.cnf`（保留 644 权限）。
   改完必须 `docker compose down -v` 清库重导，否则旧乱码数据还在。
5. **校验方法**：`SELECT HEX(title)` 应该是 `E887AA…`（UTF-8），而不是 `C3A8…`（双重编码）。

---

## 它解决了什么、没解决什么

**解决了**：环境一致性（换台电脑也能一条命令跑起来）、演示成本（面试官不用配环境）、部署路径（同一套 compose 可以直接丢到云服务器）。

**没解决**：AI 功能仍需要外网 API Key；演示数据是 `seed_demo_data.sql` 里写死的那批。
