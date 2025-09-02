# 数据库架构设计 - Kong AI Books

## 数据库选型

### 主数据库 - PostgreSQL 15+
- **ACID事务支持**: 确保数据一致性
- **JSON/JSONB支持**: 灵活的元数据存储
- **全文搜索**: 内置的文本搜索能力
- **扩展性**: 支持分区、复制、集群
- **向量扩展**: pgvector支持向量存储和搜索

### 缓存数据库 - Redis 7+
- **高性能缓存**: 亚毫秒级响应
- **数据结构丰富**: String、Hash、List、Set、ZSet
- **持久化**: RDB + AOF双重保障
- **集群支持**: Redis Cluster高可用

### 向量数据库 - Chroma/Pinecone
- **语义搜索**: 高效的向量相似性搜索
- **可扩展性**: 支持大规模向量数据
- **多模态**: 支持文本、图像等多种向量

## 核心表结构设计

### 1. 用户管理模块

```sql
-- 用户基本信息表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    avatar_url VARCHAR(500),
    display_name VARCHAR(100),
    bio TEXT,
    preferences JSONB DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- 用户角色表
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

-- 用户会话表
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    refresh_token VARCHAR(255),
    device_info JSONB,
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户偏好设置表
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    reading_preferences JSONB DEFAULT '{}', -- 字体大小、主题等
    notification_preferences JSONB DEFAULT '{}',
    privacy_preferences JSONB DEFAULT '{}',
    ai_preferences JSONB DEFAULT '{}', -- AI交互偏好
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. 书籍管理模块

```sql
-- 书籍分类表
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id UUID REFERENCES categories(id),
    icon_url VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 标签表
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(7), -- HEX颜色代码
    description TEXT,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 作者表
CREATE TABLE authors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    bio TEXT,
    avatar_url VARCHAR(500),
    birth_date DATE,
    nationality VARCHAR(50),
    website_url VARCHAR(500),
    social_links JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 出版社表
CREATE TABLE publishers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    website_url VARCHAR(500),
    logo_url VARCHAR(500),
    contact_info JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 书籍基本信息表
CREATE TABLE books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    subtitle VARCHAR(500),
    isbn VARCHAR(20) UNIQUE,
    isbn13 VARCHAR(17) UNIQUE,
    category_id UUID REFERENCES categories(id),
    publisher_id UUID REFERENCES publishers(id),
    language VARCHAR(10) DEFAULT 'zh-CN',
    description TEXT,
    abstract TEXT, -- 书籍摘要
    cover_url VARCHAR(500),
    file_path VARCHAR(500),
    file_format VARCHAR(10), -- PDF, EPUB, TXT
    file_size BIGINT,
    page_count INTEGER,
    word_count INTEGER,
    publication_date DATE,
    edition VARCHAR(50),
    price DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    view_count INTEGER DEFAULT 0,
    download_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3,2) DEFAULT 0.00,
    rating_count INTEGER DEFAULT 0,
    metadata JSONB DEFAULT '{}', -- 扩展元数据
    search_vector tsvector, -- PostgreSQL全文搜索
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT books_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT books_rating_check CHECK (rating_average >= 0 AND rating_average <= 5)
);

-- 书籍作者关联表
CREATE TABLE book_authors (
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    author_id UUID REFERENCES authors(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'AUTHOR', -- AUTHOR, EDITOR, TRANSLATOR
    sort_order INTEGER DEFAULT 0,
    PRIMARY KEY (book_id, author_id, role)
);

-- 书籍标签关联表
CREATE TABLE book_tags (
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, tag_id)
);

-- 书籍内容表（分页存储）
CREATE TABLE book_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    chapter_id UUID, -- 关联到章节表
    page_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) DEFAULT 'TEXT', -- TEXT, IMAGE, TABLE
    word_count INTEGER,
    char_count INTEGER,
    content_hash VARCHAR(64), -- 内容哈希，用于去重和版本控制
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(book_id, page_number)
);

-- 书籍章节表
CREATE TABLE book_chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    parent_chapter_id UUID REFERENCES book_chapters(id),
    title VARCHAR(300) NOT NULL,
    start_page INTEGER NOT NULL,
    end_page INTEGER NOT NULL,
    level INTEGER DEFAULT 1, -- 章节层级
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chapters_page_order_check CHECK (start_page <= end_page)
);

-- 书籍评分表
CREATE TABLE book_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    review TEXT,
    is_public BOOLEAN DEFAULT TRUE,
    helpful_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(book_id, user_id),
    CONSTRAINT rating_value_check CHECK (rating >= 1 AND rating <= 5)
);
```

### 3. 阅读管理模块

```sql
-- 阅读记录表
CREATE TABLE reading_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    current_page INTEGER DEFAULT 1,
    total_pages INTEGER,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    reading_time_minutes INTEGER DEFAULT 0,
    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'READING', -- READING, COMPLETED, PAUSED, ABANDONED
    
    UNIQUE(user_id, book_id),
    CONSTRAINT progress_check CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- 书签表
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    position_offset INTEGER DEFAULT 0, -- 页面内位置偏移
    title VARCHAR(200),
    note TEXT,
    color VARCHAR(7) DEFAULT '#FFD700', -- 书签颜色
    is_private BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 阅读笔记表
CREATE TABLE reading_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    start_position INTEGER,
    end_position INTEGER,
    selected_text TEXT,
    note_content TEXT NOT NULL,
    note_type VARCHAR(20) DEFAULT 'NOTE', -- NOTE, HIGHLIGHT, QUESTION
    color VARCHAR(7),
    is_shared BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 阅读统计表
CREATE TABLE reading_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    books_read INTEGER DEFAULT 0,
    pages_read INTEGER DEFAULT 0,
    reading_time_minutes INTEGER DEFAULT 0,
    ai_interactions INTEGER DEFAULT 0,
    searches_performed INTEGER DEFAULT 0,
    notes_created INTEGER DEFAULT 0,
    
    UNIQUE(user_id, date)
);
```

### 4. AI和搜索模块

```sql
-- AI对话记录表
CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id), -- 可选，特定书籍的对话
    title VARCHAR(200),
    summary TEXT, -- 对话摘要
    context_type VARCHAR(20) DEFAULT 'GENERAL', -- GENERAL, BOOK_SPECIFIC, SEARCH
    total_messages INTEGER DEFAULT 0,
    total_tokens_used INTEGER DEFAULT 0,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI消息表
CREATE TABLE ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    content TEXT NOT NULL,
    content_type VARCHAR(20) DEFAULT 'TEXT', -- TEXT, IMAGE, FILE
    tokens_used INTEGER,
    model_used VARCHAR(50),
    response_time_ms INTEGER,
    sources JSONB DEFAULT '[]', -- 引用来源
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT message_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

-- 搜索日志表
CREATE TABLE search_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    session_id VARCHAR(100),
    query TEXT NOT NULL,
    search_type VARCHAR(20) NOT NULL, -- TRADITIONAL, AI, HYBRID
    filters JSONB DEFAULT '{}',
    results_count INTEGER DEFAULT 0,
    clicked_results JSONB DEFAULT '[]',
    response_time_ms INTEGER,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT search_type_check CHECK (search_type IN ('TRADITIONAL', 'AI', 'HYBRID'))
);

-- AI搜索结果缓存表
CREATE TABLE ai_search_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query_hash VARCHAR(64) NOT NULL UNIQUE, -- 查询的哈希值
    query_text TEXT NOT NULL,
    response JSONB NOT NULL,
    hit_count INTEGER DEFAULT 1,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 推荐记录表
CREATE TABLE recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    recommendation_type VARCHAR(30) NOT NULL, -- CONTENT_BASED, COLLABORATIVE, AI_ENHANCED
    score DECIMAL(5,4) NOT NULL, -- 推荐分数 0-1
    reason TEXT, -- 推荐理由
    algorithm_version VARCHAR(20),
    context JSONB DEFAULT '{}', -- 推荐上下文
    shown_at TIMESTAMP,
    clicked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, book_id, recommendation_type),
    CONSTRAINT score_check CHECK (score >= 0 AND score <= 1)
);
```

### 5. 向量和嵌入模块

```sql
-- 文档向量表（如果使用PostgreSQL + pgvector）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID REFERENCES books(id) ON DELETE CASCADE,
    content_id UUID REFERENCES book_contents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI embedding维度
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(content_id, chunk_index)
);

-- 向量索引
CREATE INDEX ON document_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 查询向量缓存表
CREATE TABLE query_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query_text TEXT NOT NULL,
    query_hash VARCHAR(64) NOT NULL UNIQUE,
    embedding vector(1536),
    usage_count INTEGER DEFAULT 1,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 向量搜索日志表
CREATE TABLE vector_search_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    query_text TEXT NOT NULL,
    query_embedding_id UUID REFERENCES query_embeddings(id),
    top_k INTEGER DEFAULT 10,
    similarity_threshold DECIMAL(4,3) DEFAULT 0.7,
    results_count INTEGER,
    search_time_ms INTEGER,
    filters JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6. 系统管理模块

```sql
-- 系统配置表
CREATE TABLE system_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    config_type VARCHAR(20) DEFAULT 'STRING', -- STRING, INTEGER, BOOLEAN, JSON
    description TEXT,
    is_sensitive BOOLEAN DEFAULT FALSE, -- 是否为敏感配置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 审计日志表
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL, -- BOOK, USER, CONVERSATION
    resource_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(100),
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志表
CREATE TABLE operation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type VARCHAR(30) NOT NULL,
    operation_name VARCHAR(100) NOT NULL,
    user_id UUID REFERENCES users(id),
    parameters JSONB DEFAULT '{}',
    result JSONB DEFAULT '{}',
    duration_ms INTEGER,
    status VARCHAR(20) DEFAULT 'SUCCESS', -- SUCCESS, FAILURE, TIMEOUT
    error_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT operation_status_check CHECK (status IN ('SUCCESS', 'FAILURE', 'TIMEOUT'))
);

-- 文件管理表
CREATE TABLE file_storage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    file_hash VARCHAR(64) NOT NULL, -- 文件哈希，用于去重
    storage_type VARCHAR(20) DEFAULT 'LOCAL', -- LOCAL, S3, MINIO
    bucket_name VARCHAR(100),
    uploaded_by UUID REFERENCES users(id),
    is_processed BOOLEAN DEFAULT FALSE,
    processing_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    processing_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    CONSTRAINT storage_type_check CHECK (storage_type IN ('LOCAL', 'S3', 'MINIO')),
    CONSTRAINT processing_status_check CHECK (processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);
```

### 7. 通知和消息模块

```sql
-- 通知模板表
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_key VARCHAR(100) NOT NULL UNIQUE,
    template_name VARCHAR(200) NOT NULL,
    title_template TEXT NOT NULL,
    content_template TEXT NOT NULL,
    template_type VARCHAR(20) DEFAULT 'EMAIL', -- EMAIL, SMS, PUSH, IN_APP
    variables JSONB DEFAULT '[]', -- 模板变量定义
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户通知表
CREATE TABLE user_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    template_id UUID REFERENCES notification_templates(id),
    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    priority VARCHAR(10) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    status VARCHAR(20) DEFAULT 'UNREAD', -- UNREAD, READ, ARCHIVED
    delivery_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    scheduled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT priority_check CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT status_check CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED')),
    CONSTRAINT delivery_status_check CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'))
);
```

## 索引设计

### 1. 性能优化索引

```sql
-- 用户表索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 书籍表索引
CREATE INDEX idx_books_title ON books USING gin(to_tsvector('chinese', title));
CREATE INDEX idx_books_author ON books USING gin(to_tsvector('chinese', (SELECT string_agg(a.name, ' ') FROM authors a JOIN book_authors ba ON a.id = ba.author_id WHERE ba.book_id = books.id)));
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_books_publication_date ON books(publication_date);
CREATE INDEX idx_books_rating_average ON books(rating_average DESC);
CREATE INDEX idx_books_view_count ON books(view_count DESC);
CREATE INDEX idx_books_created_at ON books(created_at DESC);

-- 复合索引
CREATE INDEX idx_books_category_status ON books(category_id, status);
CREATE INDEX idx_books_status_rating ON books(status, rating_average DESC);

-- 书籍内容表索引
CREATE INDEX idx_book_contents_book_page ON book_contents(book_id, page_number);
CREATE INDEX idx_book_contents_content ON book_contents USING gin(to_tsvector('chinese', content));

-- 阅读记录索引
CREATE INDEX idx_reading_records_user_id ON reading_records(user_id);
CREATE INDEX idx_reading_records_book_id ON reading_records(book_id);
CREATE INDEX idx_reading_records_last_read ON reading_records(last_read_at DESC);
CREATE INDEX idx_reading_records_status ON reading_records(status);
CREATE INDEX idx_reading_records_user_status ON reading_records(user_id, status);

-- AI相关索引
CREATE INDEX idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_book_id ON ai_conversations(book_id);
CREATE INDEX idx_ai_conversations_created_at ON ai_conversations(created_at DESC);

CREATE INDEX idx_ai_messages_conversation_id ON ai_messages(conversation_id);
CREATE INDEX idx_ai_messages_created_at ON ai_messages(created_at);

CREATE INDEX idx_search_logs_user_id ON search_logs(user_id);
CREATE INDEX idx_search_logs_created_at ON search_logs(created_at DESC);
CREATE INDEX idx_search_logs_search_type ON search_logs(search_type);

-- 向量相关索引（如果使用pgvector）
CREATE INDEX idx_document_embeddings_book_id ON document_embeddings(book_id);
CREATE INDEX idx_document_embeddings_content_id ON document_embeddings(content_id);
```

### 2. 全文搜索索引

```sql
-- 创建中文全文搜索配置
CREATE TEXT SEARCH CONFIGURATION chinese (COPY = simple);
CREATE TEXT SEARCH DICTIONARY chinese_dict (
    TEMPLATE = simple,
    STOPWORDS = chinese
);
ALTER TEXT SEARCH CONFIGURATION chinese
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart, word, hword, hword_part
    WITH chinese_dict;

-- 更新书籍搜索向量
UPDATE books SET search_vector = 
    setweight(to_tsvector('chinese', COALESCE(title,'')), 'A') ||
    setweight(to_tsvector('chinese', COALESCE(description,'')), 'B') ||
    setweight(to_tsvector('chinese', (
        SELECT string_agg(a.name, ' ') 
        FROM authors a 
        JOIN book_authors ba ON a.id = ba.author_id 
        WHERE ba.book_id = books.id
    )), 'C');

-- 创建搜索向量索引
CREATE INDEX idx_books_search_vector ON books USING gin(search_vector);

-- 创建触发器自动更新搜索向量
CREATE OR REPLACE FUNCTION update_book_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('chinese', COALESCE(NEW.title,'')), 'A') ||
        setweight(to_tsvector('chinese', COALESCE(NEW.description,'')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_book_search_vector
    BEFORE INSERT OR UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION update_book_search_vector();
```

## 数据分区策略

### 1. 时间分区

```sql
-- 对大表进行时间分区
CREATE TABLE search_logs_partitioned (
    LIKE search_logs INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- 创建月度分区
CREATE TABLE search_logs_2024_01 PARTITION OF search_logs_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE search_logs_2024_02 PARTITION OF search_logs_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- 自动创建分区的函数
CREATE OR REPLACE FUNCTION create_monthly_partition(table_name TEXT, start_date DATE)
RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';
    
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF %I
                    FOR VALUES FROM (%L) TO (%L)',
                   partition_name, table_name, start_date, end_date);
END;
$$ LANGUAGE plpgsql;

-- 定期创建新分区的任务
SELECT cron.schedule('create-partitions', '0 0 1 * *', 
    'SELECT create_monthly_partition(''search_logs_partitioned'', date_trunc(''month'', CURRENT_DATE + INTERVAL ''1 month''));'
);
```

### 2. 哈希分区

```sql
-- 对用户相关数据进行哈希分区
CREATE TABLE user_activities_partitioned (
    id UUID DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_data JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, user_id)
) PARTITION BY HASH (user_id);

-- 创建哈希分区
CREATE TABLE user_activities_part_0 PARTITION OF user_activities_partitioned
    FOR VALUES WITH (modulus 4, remainder 0);

CREATE TABLE user_activities_part_1 PARTITION OF user_activities_partitioned
    FOR VALUES WITH (modulus 4, remainder 1);

CREATE TABLE user_activities_part_2 PARTITION OF user_activities_partitioned
    FOR VALUES WITH (modulus 4, remainder 2);

CREATE TABLE user_activities_part_3 PARTITION OF user_activities_partitioned
    FOR VALUES WITH (modulus 4, remainder 3);
```

## 数据迁移脚本

### 1. Flyway迁移脚本

```sql
-- V1__Initial_schema.sql
-- 创建基础表结构

-- V2__Add_ai_tables.sql
-- 添加AI相关表

-- V3__Add_vector_support.sql
-- 添加向量支持

-- V4__Add_partitioning.sql
-- 添加分区支持

-- V5__Add_indexes.sql
-- 添加性能索引

-- V6__Add_triggers.sql
-- 添加触发器和函数
```

### 2. 数据初始化脚本

```sql
-- data/init-data.sql
-- 插入默认角色
INSERT INTO roles (id, name, description, permissions) VALUES 
    (gen_random_uuid(), 'ADMIN', '系统管理员', '["ALL"]'),
    (gen_random_uuid(), 'USER', '普通用户', '["READ", "WRITE_OWN"]'),
    (gen_random_uuid(), 'PREMIUM_USER', '高级用户', '["READ", "WRITE_OWN", "AI_UNLIMITED"]');

-- 插入默认分类
INSERT INTO categories (id, name, slug, description) VALUES 
    (gen_random_uuid(), '计算机科学', 'computer-science', '计算机相关书籍'),
    (gen_random_uuid(), '文学', 'literature', '文学作品'),
    (gen_random_uuid(), '历史', 'history', '历史类书籍'),
    (gen_random_uuid(), '科学', 'science', '科学类书籍'),
    (gen_random_uuid(), '艺术', 'art', '艺术类书籍');

-- 插入系统配置
INSERT INTO system_configs (config_key, config_value, config_type, description) VALUES 
    ('ai.enabled', 'true', 'BOOLEAN', '是否启用AI功能'),
    ('ai.daily_limit', '100', 'INTEGER', '用户每日AI使用限制'),
    ('search.max_results', '50', 'INTEGER', '搜索结果最大数量'),
    ('upload.max_file_size', '104857600', 'INTEGER', '文件上传最大大小（字节）'),
    ('cache.ttl.books', '3600', 'INTEGER', '书籍缓存时间（秒）');

-- 插入通知模板
INSERT INTO notification_templates (template_key, template_name, title_template, content_template, template_type) VALUES 
    ('welcome', '欢迎消息', '欢迎加入Kong AI Books！', '亲爱的{{username}}，欢迎您加入我们的智能阅读平台！', 'EMAIL'),
    ('book_recommendation', '书籍推荐', '为您推荐新书籍', '根据您的阅读偏好，我们为您推荐了《{{book_title}}》', 'IN_APP'),
    ('reading_milestone', '阅读成就', '恭喜完成阅读目标！', '您已完成《{{book_title}}》的阅读，继续保持！', 'IN_APP');
```

## 数据库性能优化

### 1. 查询优化

```sql
-- 创建物化视图提高查询性能
CREATE MATERIALIZED VIEW book_statistics AS
SELECT 
    b.id,
    b.title,
    b.author,
    b.category_id,
    COUNT(DISTINCT rr.user_id) as reader_count,
    AVG(br.rating) as avg_rating,
    COUNT(br.id) as rating_count,
    b.view_count,
    b.created_at
FROM books b
LEFT JOIN reading_records rr ON b.id = rr.book_id
LEFT JOIN book_ratings br ON b.id = br.book_id
WHERE b.status = 'PUBLISHED'
GROUP BY b.id, b.title, b.author, b.category_id, b.view_count, b.created_at;

-- 创建索引
CREATE INDEX idx_book_statistics_reader_count ON book_statistics(reader_count DESC);
CREATE INDEX idx_book_statistics_avg_rating ON book_statistics(avg_rating DESC);
CREATE INDEX idx_book_statistics_category ON book_statistics(category_id);

-- 定期刷新物化视图
SELECT cron.schedule('refresh-book-stats', '0 */6 * * *', 
    'REFRESH MATERIALIZED VIEW CONCURRENTLY book_statistics;');

-- 热门搜索查询视图
CREATE MATERIALIZED VIEW popular_searches AS
SELECT 
    query,
    COUNT(*) as search_count,
    COUNT(DISTINCT user_id) as unique_users,
    AVG(results_count) as avg_results,
    MAX(created_at) as last_searched
FROM search_logs 
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
    AND success = TRUE
GROUP BY query
HAVING COUNT(*) >= 5
ORDER BY search_count DESC
LIMIT 1000;
```

### 2. 连接池配置

```yaml
# application-database.yml
spring:
  datasource:
    hikari:
      # 连接池配置
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
      # 性能优化
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false

  jpa:
    properties:
      hibernate:
        # 查询优化
        jdbc:
          batch_size: 25
          order_inserts: true
          order_updates: true
        
        # 缓存配置
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.redis.RedisRegionFactory
        
        # 统计信息
        generate_statistics: true
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 1000
```

## 数据备份和恢复

### 1. 备份策略

```bash
#!/bin/bash
# scripts/backup-database.sh

# 数据库备份脚本
BACKUP_DIR="/backup/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="kong_ai_books"

# 全量备份
pg_dump -h localhost -U postgres -d $DB_NAME \
    --format=custom \
    --compress=9 \
    --file="$BACKUP_DIR/full_backup_$DATE.dump"

# 增量备份（WAL文件）
pg_receivewal -h localhost -U postgres \
    --directory="$BACKUP_DIR/wal" \
    --compress=9

# 清理旧备份（保留30天）
find $BACKUP_DIR -name "*.dump" -mtime +30 -delete

echo "Database backup completed: full_backup_$DATE.dump"
```

### 2. 恢复脚本

```bash
#!/bin/bash
# scripts/restore-database.sh

BACKUP_FILE=$1
DB_NAME="kong_ai_books"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

# 停止应用服务
systemctl stop kong-ai-books

# 删除现有数据库
dropdb -h localhost -U postgres $DB_NAME

# 创建新数据库
createdb -h localhost -U postgres $DB_NAME

# 恢复数据
pg_restore -h localhost -U postgres \
    --dbname=$DB_NAME \
    --format=custom \
    --jobs=4 \
    --verbose \
    $BACKUP_FILE

# 重建索引
psql -h localhost -U postgres -d $DB_NAME -c "REINDEX DATABASE $DB_NAME;"

# 更新统计信息
psql -h localhost -U postgres -d $DB_NAME -c "ANALYZE;"

# 启动应用服务
systemctl start kong-ai-books

echo "Database restore completed from: $BACKUP_FILE"
```

## 监控和维护

### 1. 数据库监控

```sql
-- 创建监控视图
CREATE VIEW db_performance_stats AS
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation,
    most_common_vals,
    most_common_freqs
FROM pg_stats 
WHERE schemaname = 'public';

-- 慢查询监控
CREATE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements 
WHERE mean_time > 1000 -- 超过1秒的查询
ORDER BY mean_time DESC;

-- 表大小监控
CREATE VIEW table_sizes AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY size_bytes DESC;
```

### 2. 自动维护任务

```sql
-- 自动清理过期数据
CREATE OR REPLACE FUNCTION cleanup_expired_data()
RETURNS VOID AS $$
BEGIN
    -- 清理过期的搜索缓存
    DELETE FROM ai_search_cache WHERE expires_at < CURRENT_TIMESTAMP;
    
    -- 清理过期的会话
    DELETE FROM user_sessions WHERE expires_at < CURRENT_TIMESTAMP;
    
    -- 清理过期的查询嵌入缓存
    DELETE FROM query_embeddings WHERE expires_at < CURRENT_TIMESTAMP;
    
    -- 清理旧的操作日志（保留90天）
    DELETE FROM operation_logs WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    -- 更新表统计信息
    ANALYZE;
    
    RAISE NOTICE 'Expired data cleanup completed';
END;
$$ LANGUAGE plpgsql;

-- 定期执行清理任务
SELECT cron.schedule('cleanup-expired-data', '0 2 * * *', 'SELECT cleanup_expired_data();');

-- 自动更新表统计信息
SELECT cron.schedule('update-statistics', '0 3 * * 0', 'ANALYZE;');

-- 自动重建索引（每月一次）
SELECT cron.schedule('reindex-tables', '0 4 1 * *', 'REINDEX DATABASE kong_ai_books;');
```

## 数据安全

### 1. 敏感数据加密

```sql
-- 创建加密函数
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 敏感数据加密存储
CREATE OR REPLACE FUNCTION encrypt_sensitive_data(data TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN encode(pgp_sym_encrypt(data, current_setting('app.encryption_key')), 'base64');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION decrypt_sensitive_data(encrypted_data TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN pgp_sym_decrypt(decode(encrypted_data, 'base64'), current_setting('app.encryption_key'));
END;
$$ LANGUAGE plpgsql;

-- 审计触发器
CREATE OR REPLACE FUNCTION audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_logs (user_id, action, resource_type, resource_id, old_values)
        VALUES (current_setting('app.current_user_id')::UUID, 'DELETE', TG_TABLE_NAME, OLD.id, row_to_json(OLD));
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_logs (user_id, action, resource_type, resource_id, old_values, new_values)
        VALUES (current_setting('app.current_user_id')::UUID, 'UPDATE', TG_TABLE_NAME, NEW.id, row_to_json(OLD), row_to_json(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit_logs (user_id, action, resource_type, resource_id, new_values)
        VALUES (current_setting('app.current_user_id')::UUID, 'INSERT', TG_TABLE_NAME, NEW.id, row_to_json(NEW));
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 为敏感表添加审计触发器
CREATE TRIGGER audit_users AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

CREATE TRIGGER audit_books AFTER INSERT OR UPDATE OR DELETE ON books
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();
```

### 2. 数据脱敏

```sql
-- 数据脱敏函数（用于开发/测试环境）
CREATE OR REPLACE FUNCTION anonymize_user_data()
RETURNS VOID AS $$
BEGIN
    -- 脱敏用户邮箱
    UPDATE users SET 
        email = 'user' || id || '@example.com',
        username = 'user' || substring(id::text, 1, 8);
    
    -- 脱敏IP地址
    UPDATE user_sessions SET ip_address = '127.0.0.1';
    
    -- 清除敏感的用户代理信息
    UPDATE user_sessions SET user_agent = 'Anonymous Browser';
    
    RAISE NOTICE 'User data anonymization completed';
END;
$$ LANGUAGE plpgsql;
```

## Redis数据结构设计

### 1. 缓存键命名规范

```
# 用户相关
user:profile:{user_id}                 # 用户基本信息
user:session:{session_id}              # 用户会话
user:preferences:{user_id}             # 用户偏好设置
user:reading_progress:{user_id}        # 用户阅读进度

# 书籍相关  
book:detail:{book_id}                  # 书籍详情
book:content:{book_id}:{page}          # 书籍内容页
book:statistics:{book_id}              # 书籍统计信息
book:popular                           # 热门书籍列表

# AI相关
ai:search:{query_hash}                 # AI搜索结果缓存
ai:conversation:{conversation_id}      # 对话上下文
ai:embedding:{text_hash}               # 文本嵌入缓存
ai:model:stats:{model_name}            # 模型性能统计

# 搜索相关
search:suggestions                     # 搜索建议
search:trending                        # 热门搜索
search:history:{user_id}               # 用户搜索历史

# 系统相关
system:config                          # 系统配置
system:stats:daily:{date}              # 每日统计数据
rate_limit:{user_id}:{endpoint}        # 用户接口限流
```

### 2. Redis数据结构应用

```java
// service/RedisDataService.java
@Service
@Slf4j
public class RedisDataService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 用户阅读进度管理
    public void updateUserReadingProgress(String userId, String bookId, ReadingProgress progress) {
        String key = "user:reading_progress:" + userId;
        redisTemplate.opsForHash().put(key, bookId, progress);
        redisTemplate.expire(key, Duration.ofDays(30));
    }
    
    public ReadingProgress getUserReadingProgress(String userId, String bookId) {
        String key = "user:reading_progress:" + userId;
        return (ReadingProgress) redisTemplate.opsForHash().get(key, bookId);
    }
    
    // 热门书籍排行榜
    public void updateBookPopularity(String bookId, double score) {
        String key = "book:popular";
        redisTemplate.opsForZSet().add(key, bookId, score);
        
        // 只保留前100本热门书籍
        redisTemplate.opsForZSet().removeRange(key, 0, -101);
    }
    
    public List<String> getPopularBooks(int count) {
        String key = "book:popular";
        Set<Object> bookIds = redisTemplate.opsForZSet().reverseRange(key, 0, count - 1);
        return bookIds.stream()
            .map(Object::toString)
            .collect(Collectors.toList());
    }
    
    // 搜索建议管理
    public void addSearchSuggestion(String query) {
        String key = "search:suggestions";
        redisTemplate.opsForZSet().incrementScore(key, query, 1);
        
        // 保留前1000个建议
        redisTemplate.opsForZSet().removeRange(key, 0, -1001);
    }
    
    public List<String> getSearchSuggestions(String prefix, int limit) {
        String key = "search:suggestions";
        
        // 使用Lua脚本进行前缀匹配
        String luaScript = """
            local suggestions = {}
            local members = redis.call('ZREVRANGE', KEYS[1], 0, -1)
            local count = 0
            
            for i = 1, #members do
                if string.find(members[i], ARGV[1], 1, true) == 1 then
                    table.insert(suggestions, members[i])
                    count = count + 1
                    if count >= tonumber(ARGV[2]) then
                        break
                    end
                end
            end
            
            return suggestions
            """;
        
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, List.class),
            Collections.singletonList(key),
            prefix, String.valueOf(limit)
        );
        
        return suggestions != null ? suggestions : Collections.emptyList();
    }
    
    // 用户限流
    public boolean isRateLimited(String userId, String endpoint, int maxRequests, Duration window) {
        String key = "rate_limit:" + userId + ":" + endpoint;
        
        String luaScript = """
            local current = redis.call('GET', KEYS[1])
            if current == false then
                redis.call('SET', KEYS[1], 1)
                redis.call('EXPIRE', KEYS[1], ARGV[2])
                return 1
            else
                local count = tonumber(current)
                if count < tonumber(ARGV[1]) then
                    redis.call('INCR', KEYS[1])
                    return count + 1
                else
                    return -1
                end
            end
            """;
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(key),
            String.valueOf(maxRequests),
            String.valueOf(window.getSeconds())
        );
        
        return result != null && result > 0;
    }
}
```

## 数据分析和报表

### 1. 业务指标统计

```sql
-- 创建业务指标统计视图
CREATE VIEW daily_metrics AS
SELECT 
    date_trunc('day', created_at) as date,
    COUNT(DISTINCT user_id) as active_users,
    COUNT(*) as total_searches,
    COUNT(*) FILTER (WHERE search_type = 'AI') as ai_searches,
    AVG(response_time_ms) as avg_response_time
FROM search_logs 
GROUP BY date_trunc('day', created_at)
ORDER BY date DESC;

-- 用户参与度分析
CREATE VIEW user_engagement AS
SELECT 
    u.id,
    u.username,
    COUNT(DISTINCT rr.book_id) as books_read,
    SUM(rr.reading_time_minutes) as total_reading_time,
    COUNT(DISTINCT ac.id) as ai_conversations,
    COUNT(DISTINCT sl.id) as searches_performed,
    MAX(u.last_login_at) as last_active
FROM users u
LEFT JOIN reading_records rr ON u.id = rr.user_id
LEFT JOIN ai_conversations ac ON u.id = ac.user_id
LEFT JOIN search_logs sl ON u.id = sl.user_id
GROUP BY u.id, u.username;

-- 书籍热度分析
CREATE VIEW book_popularity AS
SELECT 
    b.id,
    b.title,
    b.author,
    COUNT(DISTINCT rr.user_id) as unique_readers,
    AVG(br.rating) as avg_rating,
    COUNT(br.id) as total_ratings,
    SUM(rr.reading_time_minutes) as total_reading_time,
    b.view_count,
    b.download_count
FROM books b
LEFT JOIN reading_records rr ON b.id = rr.book_id
LEFT JOIN book_ratings br ON b.id = br.book_id
WHERE b.status = 'PUBLISHED'
GROUP BY b.id, b.title, b.author, b.view_count, b.download_count
ORDER BY unique_readers DESC, avg_rating DESC;
```

### 2. 定期数据分析任务

```sql
-- 创建数据分析存储过程
CREATE OR REPLACE FUNCTION generate_daily_report(report_date DATE DEFAULT CURRENT_DATE)
RETURNS TABLE(
    metric_name TEXT,
    metric_value NUMERIC,
    metric_change_percent NUMERIC
) AS $$
DECLARE
    prev_date DATE := report_date - INTERVAL '1 day';
BEGIN
    -- 活跃用户数
    RETURN QUERY
    WITH current_metrics AS (
        SELECT COUNT(DISTINCT user_id) as active_users
        FROM search_logs 
        WHERE DATE(created_at) = report_date
    ),
    previous_metrics AS (
        SELECT COUNT(DISTINCT user_id) as active_users
        FROM search_logs 
        WHERE DATE(created_at) = prev_date
    )
    SELECT 
        'active_users'::TEXT,
        cm.active_users::NUMERIC,
        CASE 
            WHEN pm.active_users > 0 THEN 
                ((cm.active_users - pm.active_users) * 100.0 / pm.active_users)
            ELSE 0 
        END
    FROM current_metrics cm, previous_metrics pm;
    
    -- 新增书籍数
    RETURN QUERY
    SELECT 
        'new_books'::TEXT,
        COUNT(*)::NUMERIC,
        0::NUMERIC
    FROM books 
    WHERE DATE(created_at) = report_date;
    
    -- AI使用量
    RETURN QUERY
    WITH ai_usage AS (
        SELECT COUNT(*) as ai_requests
        FROM search_logs 
        WHERE DATE(created_at) = report_date 
            AND search_type = 'AI'
    )
    SELECT 
        'ai_requests'::TEXT,
        au.ai_requests::NUMERIC,
        0::NUMERIC
    FROM ai_usage au;
END;
$$ LANGUAGE plpgsql;
```

这个数据库架构设计提供了:

1. **完整的数据模型**: 涵盖用户、书籍、AI、搜索等所有业务领域
2. **高性能设计**: 合理的索引、分区、物化视图
3. **数据安全**: 加密存储、审计日志、权限控制
4. **可扩展性**: 分区表、读写分离、缓存策略
5. **运维友好**: 自动备份、监控视图、维护任务
6. **AI支持**: 向量存储、嵌入缓存、智能分析

该数据库设计能够支持大规模的在线书籍平台运营需求。