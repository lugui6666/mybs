# mybs — 云蜜罐管理系统

基于 Spring Cloud 微服务架构的云蜜罐管理平台，支持多类型蜜罐实例的部署、监控与攻击日志分析。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | |
| Spring Boot | 3.3.6 | |
| Spring Cloud | 2023.0.3 | |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 服务注册与配置中心、Sentinel 熔断 |
| Spring Cloud Gateway | — | API 网关 |
| MyBatis-Plus | 3.5.5 | ORM |
| MySQL | 8.0+ | 数据持久化 |
| Redis | 6.0+ | Session 管理 / Token 存储 |
| RabbitMQ | — | 异步任务（部署、启停、销毁、健康检查、日志） |
| Elasticsearch | 8.13.4 | 日志存储与检索 |
| JWT (jjwt) | 0.12.6 | 无状态认证 |
| Docker (WSL) | — | 蜜罐容器化部署 |
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
                     │  Spring Cloud       │
                     │  Gateway :3000      │
                     │  (JWT 鉴权 / 路由)   │
                     └──────────┬──────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
    ┌─────▼──────┐    ┌─────────▼──────┐    ┌─────────▼──────┐
    │ User       │    │ Honeypot       │    │ Honeypot       │
    │ Service    │    │ Type Service   │    │ Instance       │
    │ :8081      │    │ :8082          │    │ Service        │
    │            │    │                │    │ :8083          │
    └─────┬──────┘    └────────┬───────┘    └────────┬───────┘
          │                    │                     │
    ┌─────▼──────┐    ┌────────▼───────┐    ┌────────▼───────┐
    │ db_user_   │    │ db_hp_type_    │    │ db_hp_inst_    │
    │ _service   │    │ _service       │    │ _service       │
    └────────────┘    └────────────────┘    └────────────────┘

                          ┌─────────────┐
                          │   Redis     │
                          │  Session    │
                          │  Storage    │
                          └─────────────┘

                          ┌─────────────┐
                          │  RabbitMQ   │
                          │  异步消息    │
                          └─────────────┘

                          ┌─────────────┐
                          │   Nacos     │
                          │ 注册/配置中心│
                          └─────────────┘

                          ┌─────────────┐
                          │Elasticsearch│
                          │  日志存储    │
                          └─────────────┘
```

**架构要点：**
- **独立数据库**：每个微服务拥有独立数据库，通过 Feign 进行跨服务数据查询
- **JWT + Redis 单设备登录**：Gateway 统一校验 JWT，Redis 存储 Token 实现后登踢前登
- **Gateway @Import 模式**：网关不扫描 common 全包，仅按需导入所需 Bean，避免 WebFlux 与 WebMVC 类路径冲突
- **RabbitMQ 异步任务**：蜜罐实例的部署、启停、销毁、健康检查等耗时操作通过消息队列异步处理
- **健康检查延迟队列**：利用 RabbitMQ 死信队列实现延迟心跳检测
- **Sentinel 熔断降级**：跨服务调用通过 Sentinel 保护，防止级联故障
- **Elasticsearch 日志存储**：攻击日志通过 RabbitMQ 采集后存入 ES，支持高效全文检索

## 开发状态

| 模块 | 状态 | 说明 |
|------|------|------|
| mybs-common | ✅ 已完成 | 公共实体、DTO、JWT 工具、SessionService、全局异常处理 |
| mybs-gateway | ✅ 已完成 | 路由转发、JWT 鉴权、CORS、Token 校验 |
| mybs-user-service | ✅ 已完成 | 登录/注册、用户信息管理、单设备登录 |
| mybs-honeypot-type-service | ✅ 已完成 | 蜜罐类型 CRUD |
| mybs-honeypot-instance-service | ✅ 已完成 | 蜜罐实例部署、启停、销毁、健康检查 |
| mybs-log-service | ✅ 已完成 | 攻击日志采集（RabbitMQ → Elasticsearch）与查询 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x
- RabbitMQ 3.x（含延迟队列插件）
- Elasticsearch 8.x（日志服务需要）
- Docker（WSL 环境，用于蜜罐容器化部署）

### 1. 启动基础服务

```bash
# 启动 Nacos（单机模式）
cd nacos/bin
startup.cmd -m standalone        # Windows
sh startup.sh -m standalone      # Linux/Mac

# 启动 Redis
redis-server

# 启动 MySQL（确保服务已运行）
# 启动 RabbitMQ
rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# 启动 Elasticsearch
```

### 2. 初始化数据库

```bash
mysql -u root -p < doc/sql/user_service.sql
mysql -u root -p < doc/sql/hp_type_service.sql
mysql -u root -p < doc/sql/hp_instance_service.sql
```

### 3. 修改配置

编辑各服务 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    password: your_mysql_password
  data:
    redis:
      password: your_redis_password
  rabbitmq:
    password: your_rabbitmq_password
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

# 3. Honeypot Type Service
mvn -pl mybs-honeypot-type-service spring-boot:run

# 4. Honeypot Instance Service
mvn -pl mybs-honeypot-instance-service spring-boot:run

# 5. Log Service
mvn -pl mybs-log-service spring-boot:run
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

## 服务端口概览

| 服务 | 端口 | 说明 |
|------|------|------|
| mybs-gateway | 3000 | API 网关 |
| mybs-user-service | 8081 | 用户服务 |
| mybs-honeypot-type-service | 8082 | 蜜罐类型服务 |
| mybs-honeypot-instance-service | 8083 | 蜜罐实例服务 |
| mybs-log-service | 8084 | 日志服务 |
| Nacos | 8848 | 注册/配置中心 |
| Sentinel Dashboard | 8090 | 流量治理控制台 |

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

### 蜜罐类型接口（需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/hp-type/page` | 分页查询类型列表 |
| GET | `/api/hp-type/list` | 查询全部类型 |
| GET | `/api/hp-type/{id}` | 查询类型详情 |
| POST | `/api/hp-type` | 新增蜜罐类型 |
| PUT | `/api/hp-type/{id}` | 更新蜜罐类型 |
| DELETE | `/api/hp-type/{id}` | 删除蜜罐类型 |

### 蜜罐实例接口（需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/hp-instance/page` | 分页查询实例列表（支持 keyword/status/typeId 筛选） |
| GET | `/api/hp-instance/{id}` | 查询实例详情 |
| POST | `/api/hp-instance` | 部署新实例 |
| PUT | `/api/hp-instance/{id}` | 更新实例配置 |
| PUT | `/api/hp-instance/{id}/start` | 启动实例 |
| PUT | `/api/hp-instance/{id}/stop` | 停止实例 |
| DELETE | `/api/hp-instance/{id}` | 销毁实例 |

### 日志接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/logs/collect` | 采集日志（白名单，蜜罐直接上报） |
| GET | `/api/logs/search` | 检索日志（需 Token） |

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

## 蜜罐实例状态机

```
deploying → running → stopped → running  （启停循环）
               ↓
            error → running/stopped        （异常恢复）
               ↓
            destroyed                      （最终销毁）
```

实例状态包括：`deploying`（部署中）、`running`（运行中）、`stopped`（已停止）、`error`（异常）、`destroyed`（已销毁）。

## 消息队列架构

项目使用 RabbitMQ 处理以下异步任务：

| 交换机 | 路由键 | 队列 | 用途 |
|--------|--------|------|------|
| `deploy.exchange` | `deploy.routingkey` | `deploy.queue` | 蜜罐部署 |
| — | `stop.routingkey` | `stop.queue` | 停止实例 |
| — | `start.routingkey` | `start.queue` | 启动实例 |
| — | `destroy.routingkey` | `destroy.queue` | 销毁实例 |
| `health.exchange` | `health.delay.routingkey` | `health.delay.queue` | 健康检查延迟队列 |
| `health.exchange` | `health.work.routingkey` | `health.work.queue` | 健康检查工作队列 |
| `logs.exchange` | `logs.routingkey` | `logs.queue` | 攻击日志采集 |

## License

待定
