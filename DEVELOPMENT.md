# Kong AI Books 开发指南

## 🛠️ 开发环境搭建

### 前置条件

确保你的开发环境安装了以下工具：

- **Java 17+** (推荐使用 OpenJDK 17)
- **Node.js 18+** (推荐使用 LTS 版本)
- **Docker & Docker Compose** (用于本地服务)
- **Git** (版本控制)
- **IDE**: IntelliJ IDEA / VS Code

### 环境变量配置

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
# 必须设置的变量:
# - OPENAI_API_KEY: OpenAI API密钥
# - JWT_SECRET: JWT签名密钥 (生产环境必须更改)
```

## 🏃‍♂️ 快速启动

### 方式一: Docker一键启动 (推荐)

```bash
# 启动所有服务
docker-compose up -d

# 等待服务启动完成
docker-compose logs -f backend | grep "Started KongAiBooksApplication"

# 访问应用
# 前端: http://localhost:3000
# 后端API: http://localhost:8080
# 数据库: localhost:5432
```

### 方式二: 本地开发模式

#### 1. 启动基础服务

```bash
# 仅启动数据库和缓存服务
docker-compose up -d postgres redis chroma
```

#### 2. 启动后端

```bash
cd backend

# 首次启动需要运行数据库迁移
./gradlew flywayMigrate

# 启动Spring Boot应用
./gradlew bootRun

# 或者在IDE中运行 KongAiBooksApplication.main()
```

#### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 应用将在 http://localhost:3000 启动
```

## 📁 项目结构详解

### 后端模块结构

```
backend/src/main/java/com/kong/aibooks/
├── api/                        # API层
│   ├── controller/            # REST控制器
│   ├── dto/                   # 数据传输对象
│   └── config/                # API配置
├── core/                      # 核心业务层
│   ├── book/                  # 书籍管理
│   │   ├── BookService.java
│   │   ├── dto/
│   │   └── mapper/
│   ├── user/                  # 用户管理
│   ├── ai/                    # AI服务
│   └── search/                # 搜索服务
├── data/                      # 数据访问层
│   ├── entity/                # JPA实体
│   ├── repository/            # 数据仓库
│   └── migration/             # 数据库迁移
├── common/                    # 公共模块
│   ├── config/                # 配置类
│   ├── security/              # 安全配置
│   ├── exception/             # 异常处理
│   └── utils/                 # 工具类
└── integration/               # 外部集成
    ├── ai/                    # AI客户端
    ├── storage/               # 文件存储
    └── notification/          # 通知服务
```

### 前端模块结构

```
frontend/src/
├── components/                # 组件库
│   ├── ui/                   # 基础UI组件
│   ├── layout/               # 布局组件
│   ├── book/                 # 书籍相关组件
│   └── ai/                   # AI相关组件
├── pages/                    # 页面组件
├── hooks/                    # 自定义Hooks
├── services/                 # API服务
├── store/                    # 状态管理
├── types/                    # TypeScript类型
├── utils/                    # 工具函数
└── styles/                   # 样式文件
```

## 🔧 开发工作流

### 1. 功能开发流程

```bash
# 1. 创建功能分支
git checkout -b feature/book-reader-enhancement

# 2. 开发功能
# - 编写代码
# - 添加测试
# - 更新文档

# 3. 代码质量检查
cd frontend && npm run lint && npm run test
cd backend && ./gradlew check && ./gradlew test

# 4. 提交代码
git add .
git commit -m "feat: enhance book reader with bookmark functionality"

# 5. 推送并创建PR
git push origin feature/book-reader-enhancement
```

### 2. 数据库迁移

```bash
# 创建新的迁移文件
cd backend/src/main/resources/db/migration
# 命名格式: V{version}__{description}.sql
# 例如: V002__add_bookmark_table.sql

# 运行迁移
./gradlew flywayMigrate

# 查看迁移状态
./gradlew flywayInfo

# 回滚迁移 (谨慎使用)
./gradlew flywayUndo
```

### 3. API开发

#### 创建新的REST端点

```java
// 1. 定义DTO
public class CreateBookRequest {
    @NotBlank
    private String title;
    // ...
}

// 2. 实现Service方法
@Service
public class BookService {
    public BookDTO createBook(CreateBookRequest request) {
        // 业务逻辑
    }
}

// 3. 添加Controller端点
@RestController
public class BookController {
    @PostMapping("/api/v1/books")
    public ResponseEntity<BookDTO> createBook(@RequestBody @Valid CreateBookRequest request) {
        BookDTO book = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }
}
```

### 4. 前端组件开发

#### 创建新组件

```typescript
// 1. 定义组件接口
interface BookCardProps {
  book: Book;
  onSelect?: (book: Book) => void;
}

// 2. 实现组件
export const BookCard: React.FC<BookCardProps> = ({ book, onSelect }) => {
  // 组件逻辑
};

// 3. 添加测试
describe('BookCard', () => {
  it('should render book information', () => {
    // 测试逻辑
  });
});
```

## 🧪 测试策略

### 后端测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests BookServiceTest

# 生成测试报告
./gradlew test jacocoTestReport
```

### 前端测试

```bash
# 单元测试
npm run test

# E2E测试
npm run test:e2e

# 测试覆盖率
npm run test:coverage
```

### 集成测试

```bash
# 启动测试环境
docker-compose -f docker-compose.test.yml up -d

# 运行集成测试
./gradlew integrationTest

# 清理测试环境
docker-compose -f docker-compose.test.yml down
```

## 🐛 调试指南

### 后端调试

#### 1. 应用日志

```bash
# 查看应用日志
tail -f backend/logs/kong-ai-books.log

# 或者通过Docker查看
docker-compose logs -f backend
```

#### 2. 数据库调试

```bash
# 连接数据库
docker-compose exec postgres psql -U postgres -d kong_ai_books

# 查看表结构
\dt
\d books
```

#### 3. Redis调试

```bash
# 连接Redis
docker-compose exec redis redis-cli

# 查看缓存键
KEYS *
```

### 前端调试

#### 1. 浏览器开发工具

- 使用React Developer Tools
- 检查Network请求
- 查看Console错误

#### 2. Vite调试

```bash
# 详细输出模式
npm run dev -- --debug

# 构建分析
npm run build -- --analyze
```

## 🔍 代码规范

### Java代码规范

- 使用Google Java Style Guide
- 类名使用PascalCase
- 方法名使用camelCase
- 常量使用UPPER_SNAKE_CASE
- 包名全小写

### TypeScript代码规范

- 使用ESLint + Prettier
- 接口名使用PascalCase
- 变量名使用camelCase
- 常量使用UPPER_SNAKE_CASE
- 文件名使用kebab-case

### Git提交规范

使用Conventional Commits规范：

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建工具或辅助工具的变动
```

## 📊 性能优化

### 后端性能优化

1. **数据库优化**
   - 添加适当的索引
   - 使用连接池
   - 查询优化

2. **缓存策略**
   - Redis缓存热点数据
   - Spring Cache注解
   - HTTP缓存头

3. **异步处理**
   - 使用@Async处理耗时操作
   - CompletableFuture异步编程

### 前端性能优化

1. **代码分割**
   - React.lazy懒加载
   - 路由级别分割

2. **缓存优化**
   - React Query缓存
   - Service Worker
   - HTTP缓存

3. **资源优化**
   - 图片懒加载
   - WebP格式
   - Bundle分析

## 🚀 部署流程

### 开发环境部署

```bash
# 构建并启动
docker-compose up --build -d
```

### 生产环境部署

```bash
# 使用生产配置
docker-compose -f docker-compose.prod.yml up -d

# 或使用CI/CD流水线自动部署
```

### 健康检查

```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查前端状态
curl http://localhost:3000

# 检查数据库连接
docker-compose exec postgres pg_isready
```

## 🔧 故障排除

### 常见问题

#### 1. 后端启动失败

```bash
# 检查Java版本
java -version

# 检查数据库连接
docker-compose exec postgres psql -U postgres -c "SELECT 1"

# 查看详细错误日志
./gradlew bootRun --debug
```

#### 2. 前端构建失败

```bash
# 清理node_modules
rm -rf node_modules package-lock.json
npm install

# 检查Node版本
node --version
npm --version
```

#### 3. AI服务不可用

```bash
# 检查OpenAI API Key
echo $OPENAI_API_KEY

# 检查Chroma服务
curl http://localhost:8000/api/v1/heartbeat

# 查看AI服务日志
docker-compose logs -f backend | grep "AI"
```

## 📚 学习资源

### 技术文档

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Spring AI文档](https://docs.spring.io/spring-ai/reference/)
- [React官方文档](https://react.dev/)
- [Vite官方文档](https://vitejs.dev/)

### 项目文档

- [系统架构设计](ARCHITECTURE.md)
- [AI/RAG系统架构](ai-rag-architecture.md)
- [数据库设计](database-schema.md)
- [部署指南](deployment-guide.md)

## 🤝 贡献指南

### 提交代码前检查清单

- [ ] 代码符合项目规范
- [ ] 添加了必要的测试
- [ ] 更新了相关文档
- [ ] 通过了所有测试
- [ ] 没有引入新的安全漏洞

### Code Review要点

- 代码逻辑正确性
- 性能影响评估
- 安全性检查
- 可维护性
- 测试覆盖率

## 📞 获取帮助

- **技术问题**: 在GitHub Issues中提问
- **功能建议**: 创建Feature Request
- **安全问题**: 发送邮件到 security@kong-ai-books.com
- **其他问题**: 联系开发团队

---

Happy Coding! 🎉