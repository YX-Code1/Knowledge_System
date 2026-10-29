# MySQL 8.0 + 强制 utf8mb4
# 为什么不用 bind mount：Windows 挂进容器的文件是 777，
# MySQL 会提示 "World-writable config file ... is ignored" 并跳过，
# 所以必须在镜像构建阶段 COPY 进来（COPY 保留 644 权限）。
FROM mysql:8.0

COPY charset.cnf /etc/mysql/conf.d/charset.cnf
RUN chmod 0644 /etc/mysql/conf.d/charset.cnf
