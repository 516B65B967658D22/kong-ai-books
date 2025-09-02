# Kong AI Books - 系统架构设计

## 项目概述

Kong AI Books 是一个基于现代技术栈的在线书籍平台，提供智能搜索、阅读和AI问答功能。

### 核心特性
- 在线书籍查询和阅读
- AI智能搜索和回复
- 专业简洁的用户界面
- RAG (检索增强生成) 技术支持

## 整体架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用       │    │   后端服务       │    │   AI服务层      │
│  React 18       │◄──►│  Spring Boot    │◄──►│  Spring AI      │
│  + Vite         │    │  + Spring AI    │    │  + RAG Engine   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CDN/静态资源   │    │   数据库层       │    │   向量数据库     │
│                │    │  PostgreSQL     │    │  Chroma/Pinecone│
└─────────────────┘    │  + Redis        │    │  + 知识库       │
                       └─────────────────┘    └─────────────────┘
```

## 前端架构 (React 18 + Vite)

### 技术栈
- **构建工具**: Vite 4+
- **框架**: React 18 (支持并发特性)
- **路由**: React Router v6
- **状态管理**: Zustand / Redux Toolkit
- **UI框架**: Ant Design / Material-UI
- **样式**: Tailwind CSS + CSS Modules
- **HTTP客户端**: Axios
- **类型检查**: TypeScript

### 目录结构
```
frontend/
├── src/
│   ├── components/          # 公共组件
│   │   ├── common/         # 通用UI组件
│   │   ├── book/           # 书籍相关组件
│   │   └── ai/             # AI交互组件
│   ├── pages/              # 页面组件
│   │   ├── Home/           # 首页
│   │   ├── BookList/       # 书籍列表
│   │   ├── BookReader/     # 书籍阅读器
│   │   └── AIChat/         # AI对话页面
│   ├── hooks/              # 自定义Hooks
│   ├── services/           # API服务层
│   ├── store/              # 状态管理
│   ├── utils/              # 工具函数
│   ├── types/              # TypeScript类型定义
│   └── assets/             # 静态资源
├── public/                 # 公共资源
└── dist/                   # 构建输出
```

### 核心组件设计

#### 1. 书籍阅读器组件
```typescript
interface BookReaderProps {
  bookId: string;
  initialPage?: number;
}

// 功能特性:
// - 分页阅读
// - 书签管理
// - 阅读进度追踪
// - 字体大小调节
// - 夜间模式
```

#### 2. AI搜索组件
```typescript
interface AISearchProps {
  onSearch: (query: string) => void;
  suggestions?: string[];
  isLoading?: boolean;
}

// 功能特性:
// - 智能搜索建议
// - 搜索历史
// - 语音输入支持
```

#### 3. AI对话组件
```typescript
interface AIChatProps {
  bookContext?: BookContext;
  conversationId?: string;
}

// 功能特性:
// - 流式响应显示
// - 上下文感知对话
// - 引用来源显示
```

## 后端架构 (Spring Boot + Spring AI)

### 技术栈
- **框架**: Spring Boot 3.2+
- **AI集成**: Spring AI
- **数据库**: PostgreSQL 15+
- **缓存**: Redis 7+
- **搜索引擎**: Elasticsearch (可选)
- **文件存储**: MinIO / AWS S3
- **消息队列**: RabbitMQ / Apache Kafka
- **监控**: Micrometer + Prometheus

### 模块设计

#### 1. 核心模块
```
backend/
├── kong-ai-books-api/           # API网关层
├── kong-ai-books-book/          # 书籍管理服务
├── kong-ai-books-user/          # 用户管理服务
├── kong-ai-books-ai/            # AI服务模块
├── kong-ai-books-search/        # 搜索服务
├── kong-ai-books-common/        # 公共模块
└── kong-ai-books-gateway/       # 服务网关
```

#### 2. API层设计
```java
@RestController
@RequestMapping("/api/v1")
public class BookController {
    
    @GetMapping("/books")
    public PageResult<BookDTO> getBooks(@RequestParam BookQuery query);
    
    @GetMapping("/books/{id}")
    public BookDetailDTO getBookDetail(@PathVariable String id);
    
    @GetMapping("/books/{id}/content")
    public BookContentDTO getBookContent(@PathVariable String id, @RequestParam int page);
}

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {
    
    @PostMapping("/search")
    public AISearchResponse intelligentSearch(@RequestBody AISearchRequest request);
    
    @PostMapping("/chat")
    public Flux<ChatResponse> chatWithAI(@RequestBody ChatRequest request);
    
    @PostMapping("/recommend")
    public List<BookRecommendation> getRecommendations(@RequestBody RecommendationRequest request);
}
```

## AI/RAG 系统架构

### 核心组件

#### 1. 文档处理流水线
```
原始书籍文档 → 文本提取 → 分块处理 → 向量化 → 存储到向量数据库
    ↓              ↓         ↓         ↓           ↓
  PDF/EPUB    文本清洗   语义分块   Embedding   Chroma/Pinecone
```

#### 2. RAG检索流程
```
用户查询 → 查询向量化 → 相似性搜索 → 上下文构建 → LLM生成 → 响应返回
    ↓          ↓           ↓           ↓         ↓         ↓
  自然语言   Embedding   Top-K检索   Prompt工程  GPT/Claude  结构化响应
```

#### 3. Spring AI集成
```java
@Service
public class BookAIService {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private EmbeddingClient embeddingClient;
    
    @Autowired
    private VectorStore vectorStore;
    
    public AIResponse searchBooks(String query) {
        // 1. 向量化查询
        List<Double> queryEmbedding = embeddingClient.embed(query);
        
        // 2. 相似性搜索
        List<Document> similarDocs = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(5)
        );
        
        // 3. 构建上下文
        String context = buildContext(similarDocs);
        
        // 4. 生成响应
        return chatClient.call(
            new Prompt(buildPrompt(query, context))
        );
    }
}
```

## 数据库设计

### 核心表结构

#### 1. 书籍相关表
```sql
-- 书籍基本信息表
CREATE TABLE books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    author VARCHAR(200) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    category_id UUID REFERENCES categories(id),
    description TEXT,
    cover_url VARCHAR(500),
    file_path VARCHAR(500),
    page_count INTEGER,
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 书籍内容表 (分页存储)
CREATE TABLE book_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    word_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(book_id, page_number)
);

-- 书籍分类表
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id UUID REFERENCES categories(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. 用户相关表
```sql
-- 用户表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    preferences JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 阅读记录表
CREATE TABLE reading_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    current_page INTEGER DEFAULT 1,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reading_time_minutes INTEGER DEFAULT 0,
    UNIQUE(user_id, book_id)
);

-- 书签表
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. AI相关表
```sql
-- AI对话记录表
CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id),
    title VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI消息表
CREATE TABLE ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- 'user' or 'assistant'
    content TEXT NOT NULL,
    tokens_used INTEGER,
    response_time_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 搜索日志表
CREATE TABLE search_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    query TEXT NOT NULL,
    search_type VARCHAR(20), -- 'traditional' or 'ai'
    results_count INTEGER,
    response_time_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 部署架构

### 容器化部署
```yaml
# docker-compose.yml
version: '3.8'
services:
  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      - VITE_API_BASE_URL=http://backend:8080
  
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=postgresql://postgres:5432/kong_ai_books
      - REDIS_URL=redis://redis:6379
    depends_on:
      - postgres
      - redis
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=kong_ai_books
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - frontend
      - backend

volumes:
  postgres_data:
```

### 微服务架构 (可选扩展)
```
┌─────────────────┐
│   API Gateway   │ ← Nginx/Kong
│   (Kong/Zuul)   │
└─────────┬───────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│ User    │ │ Book    │ │ Search  │ │ AI      │
│ Service │ │ Service │ │ Service │ │ Service │
└─────────┘ └─────────┘ └─────────┘ └─────────┘
    │           │           │           │
    ▼           ▼           ▼           ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│ User DB │ │ Book DB │ │Elastic  │ │Vector DB│
└─────────┘ └─────────┘ │Search   │ └─────────┘
                        └─────────┘
```

## 技术选型详解

### 前端技术栈
- **React 18**: 利用并发特性提升用户体验
- **Vite**: 快速开发和构建
- **TypeScript**: 类型安全
- **Tailwind CSS**: 快速样式开发
- **React Query**: 服务器状态管理
- **React Router**: 客户端路由

### 后端技术栈
- **Spring Boot 3.2+**: 现代Java开发框架
- **Spring AI**: AI能力集成
- **Spring Security**: 安全认证
- **Spring Data JPA**: 数据访问层
- **PostgreSQL**: 主数据库
- **Redis**: 缓存和会话存储

### AI技术栈
- **Spring AI**: 统一AI接口
- **OpenAI GPT-4** / **Claude**: 大语言模型
- **Chroma/Pinecone**: 向量数据库
- **LangChain4j**: RAG实现
- **Sentence Transformers**: 文本向量化

## 安全架构

### 认证授权
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
            )
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/ai/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### 数据安全
- JWT Token认证
- HTTPS强制
- SQL注入防护
- XSS防护
- CORS配置
- 敏感数据加密

## 性能优化策略

### 前端优化
- **代码分割**: React.lazy + Suspense
- **缓存策略**: Service Worker + HTTP缓存
- **图片优化**: WebP格式 + 懒加载
- **Bundle优化**: Tree shaking + 压缩

### 后端优化
- **数据库优化**: 索引优化 + 查询优化
- **缓存策略**: Redis多层缓存
- **连接池**: HikariCP配置优化
- **异步处理**: @Async + CompletableFuture

### AI服务优化
- **向量缓存**: 热门查询结果缓存
- **模型选择**: 根据查询复杂度选择模型
- **批处理**: 批量向量化处理
- **流式响应**: Server-Sent Events

## 监控和运维

### 监控指标
- **应用指标**: QPS, 响应时间, 错误率
- **业务指标**: 用户活跃度, 阅读时长, AI使用率
- **基础设施**: CPU, 内存, 磁盘, 网络

### 日志管理
```java
// 结构化日志
@Slf4j
@Component
public class AIServiceLogger {
    
    public void logAIRequest(String userId, String query, long responseTime) {
        log.info("AI_REQUEST userId={} query={} responseTime={}ms", 
                userId, query, responseTime);
    }
}
```

### 错误处理
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ErrorResponse> handleAIServiceException(AIServiceException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("AI_SERVICE_ERROR", e.getMessage()));
    }
}
```

## 扩展性考虑

### 水平扩展
- 无状态服务设计
- 数据库读写分离
- 缓存集群
- 负载均衡

### 功能扩展
- 多语言支持
- 移动端适配
- 离线阅读
- 社交功能
- 个性化推荐

## 开发流程

### 开发环境
```bash
# 前端开发
cd frontend
npm install
npm run dev

# 后端开发
cd backend
./mvnw spring-boot:run

# 数据库迁移
./mvnw flyway:migrate
```

### CI/CD流程
```yaml
# GitHub Actions示例
name: CI/CD Pipeline
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Tests
        run: |
          cd frontend && npm test
          cd backend && ./mvnw test
```

## 总结

这个架构设计提供了:
1. **模块化**: 清晰的前后端分离和模块划分
2. **可扩展性**: 支持微服务演进和水平扩展
3. **现代化**: 使用最新的技术栈和最佳实践
4. **AI集成**: 完整的RAG系统和智能功能
5. **高性能**: 多层缓存和优化策略
6. **安全性**: 完善的安全防护机制

该架构可以支持从MVP到大规模生产环境的演进需求。