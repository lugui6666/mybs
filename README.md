# 云蜜罐管理系统 (Cloud Honeypot Management System)

基于 Spring Cloud 微服务架构的云蜜罐管理平台，支持多类型蜜罐实例的部署、监控与攻击日志分析。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | |
| Spring Boot | 3.3.6 | |
| Spring Cloud | 2023.0.3 | |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 服务注册与配置中心 |
| Spring Cloud Gateway | — | API 网关 |
| MyBatis-Plus | 3.5.5 | ORM |
| MySQL | 8.0+ | 数据持久化 |
| Redis | 6.0+ | Session 管理 / Token 存储 |
| JWT (jjwt) | 0.12.6 | 无状态认证 |
| Lombok | — | 代码简化 |
| Maven | 3.9+ | 构建工具 |

## 项目结构

```
mybs/
├── pom.xml                              # 父 POM，依赖版本管理
├── mybs-common/                         # 公共模块（共享实体、工具、配置）
├── mybs-gateway/                        # API 网关 :3000
├── mybs-user-service/                   # 用户管理服务 :8081
├── mybs-honeypot-type-service/          # 蜜罐类型管理服务 :8082
├── mybs-honeypot-instance-service/      # 蜜罐实例管理服务 :8083
├── mybs-log-service/                    # 日志管理服务 :8084
└── doc/sql/                             # 数据库初始化脚本
```

## 架构设计

```
                     ┌─────────────────────┐
                     │   Spring Cloud       │
                     │   Gateway :3000      │
                     │   (JWT 鉴权 / 路由)   │
                     └──────┬──────────────┘
                            │
              ┌─────────────┼─────────────┬──────────────┐
              │             │             │              │
     ┌────────▼──┐  ┌───────▼───┐ ┌───────▼─────┐ ┌──────▼───┐
     │ User      │  │ Honeypot  │ │ Honeypot    │ │ Log      │
     │ Service   │  │ Type      │ │ Instance    │ │ Service  │
     │ :8081     │  │ Service   │ │ Service     │ │ :8084    │
     │           │  │ :8082     │ │ :8083       │ │          │
     └─────┬─────┘  └─────┬─────┘ └──────┬──────┘ └────┬─────┘
           │              │              │             │
     ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼──────┐  ┌───▼──────┐
     │ db_user   │  │ db_hp_type│  │ db_hp_inst │  │ db_log   │
     │ _service  │  │ _service  │  │ _service   │  │ _service │
     └───────────┘  └───────────┘  └────────────┘  └──────────┘

                         ┌──────────────┐
                         │    Redis     │
                         │   Session    │
                         │   Storage    │
                         └──────────────┘
```

**设计要点：**
- **独立数据库**：每个微服务拥有独立数据库，通过 Feign 进行跨服务数据查询
- **JWT + Redis 单设备登录**：Gateway 统一校验 JWT，Redis 存储 Token 实现后登踢前登
- **Gateway @Import 模式**：网关不扫描 common 全包，仅按需导入所需 Bean，避免 WebFlux 与 WebMVC 类路径冲突

## 开发状态

| 模块 | 状态 | 说明 |
|------|------|------|
| mybs-common | ✅ 已完成 | 公共实体、DTO、JWT 工具、SessionService |
| mybs-gateway | ✅ 已完成 | 路由转发、JWT 鉴权、CORS、Token 校验 |
| mybs-user-service | ✅ 已完成 | 登录/注册、用户信息管理、单设备登录 |
| mybs-honeypot-type-service | 🚧 开发中 | 蜜罐类型 CRUD |
| mybs-honeypot-instance-service | 🚧 开发中 | 蜜罐实例生命周期管理 |
| mybs-log-service | 🚧 开发中 | 攻击日志采集与统计 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x

### 1. 启动基础服务

```bash
# 启动 Nacos（单机模式）
cd nacos/bin
startup.cmd -m standalone        # Windows
sh startup.sh -m standalone      # Linux/Mac

# 启动 Redis
redis-server

# 启动 MySQL（确保服务已运行）
```

### 2. 初始化数据库

```bash
# 依次执行以下 SQL 脚本
mysql -u root -p < doc/sql/user_service.sql
# 其他服务的 SQL 脚本待对应模块完成后执行
```

### 3. 修改配置

编辑各服务 `src/main/resources/application.yml`：

```yaml
# 数据库密码、Redis 密码、Nacos 地址等
spring:
  datasource:
    password: your_mysql_password
  data:
    redis:
      password: your_redis_password
```

### 4. 编译项目

```bash
mvn clean compile
```

### 5. 启动服务（按顺序）

```bash
# 1. Gateway
mvn -pl mybs-gateway spring-boot:run

# 2. User Service
mvn -pl mybs-user-service spring-boot:run

# 其他服务待开发完成后启动
```

### 6. 验证

```bash
# 注册
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","email":"test@example.com"}'

# 登录
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 查询当前用户（替换 YOUR_TOKEN）
curl -X GET http://localhost:3000/api/auth/current \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## API 文档

### 认证接口（白名单，无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录，返回 JWT Token |
| POST | `/api/auth/register` | 注册 |

### 用户管理接口（需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/current` | 获取当前登录用户信息 |
| GET | `/api/user/{id}` | 查询用户详情 |
| PUT | `/api/user/{id}` | 更新用户信息 |
| DELETE | `/api/user/{id}` | 删除用户（同时清除 Session） |

> 请求需携带 Header: `Authorization: Bearer <token>`

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1720000000000
}
```

## 认证流程

```
1. POST /api/auth/login → UserService 验证密码
   └── 生成 JWT → 存入 Redis (user:token:{userId})

2. 后续请求 → Gateway AuthGlobalFilter
   ├── 解析 JWT
   ├── 对比 Redis 中的 Token（后登踢前登）
   ├── 写入 X-User-Id / X-User-Name 请求头
   └── 转发到下游服务

3. DELETE /api/user/{id} → 清除 Redis Session
```

## License

待定
