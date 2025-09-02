# 前端架构设计 - Kong AI Books

## 技术栈选择

### 核心框架
- **React 18.2+**: 利用并发特性和自动批处理
- **TypeScript 5.0+**: 类型安全和开发体验
- **Vite 4.0+**: 快速开发服务器和优化构建

### 开发工具链
- **ESLint + Prettier**: 代码规范
- **Husky + lint-staged**: Git钩子
- **Vitest**: 单元测试
- **Playwright**: E2E测试

## 目录结构详解

```
frontend/
├── public/                     # 静态资源
│   ├── favicon.ico
│   ├── manifest.json
│   └── robots.txt
├── src/
│   ├── app/                    # 应用配置
│   │   ├── App.tsx            # 根组件
│   │   ├── store.ts           # 全局状态配置
│   │   └── router.tsx         # 路由配置
│   ├── components/            # 组件库
│   │   ├── ui/                # 基础UI组件
│   │   │   ├── Button/
│   │   │   ├── Input/
│   │   │   ├── Modal/
│   │   │   └── index.ts       # 统一导出
│   │   ├── layout/            # 布局组件
│   │   │   ├── Header/
│   │   │   ├── Sidebar/
│   │   │   ├── Footer/
│   │   │   └── MainLayout/
│   │   ├── book/              # 书籍相关组件
│   │   │   ├── BookCard/
│   │   │   ├── BookReader/
│   │   │   ├── BookSearch/
│   │   │   └── BookProgress/
│   │   └── ai/                # AI相关组件
│   │       ├── ChatInterface/
│   │       ├── AISearch/
│   │       └── SmartRecommend/
│   ├── pages/                 # 页面组件
│   │   ├── Home/
│   │   ├── Library/
│   │   ├── BookDetail/
│   │   ├── Reader/
│   │   ├── AIChat/
│   │   └── Profile/
│   ├── hooks/                 # 自定义Hooks
│   │   ├── useAuth.ts
│   │   ├── useBooks.ts
│   │   ├── useAI.ts
│   │   └── useLocalStorage.ts
│   ├── services/              # API服务层
│   │   ├── api.ts             # Axios配置
│   │   ├── auth.ts            # 认证服务
│   │   ├── books.ts           # 书籍API
│   │   ├── ai.ts              # AI API
│   │   └── types.ts           # API类型定义
│   ├── store/                 # 状态管理
│   │   ├── slices/
│   │   │   ├── authSlice.ts
│   │   │   ├── booksSlice.ts
│   │   │   └── aiSlice.ts
│   │   └── index.ts
│   ├── utils/                 # 工具函数
│   │   ├── constants.ts
│   │   ├── helpers.ts
│   │   ├── validators.ts
│   │   └── formatters.ts
│   ├── types/                 # 类型定义
│   │   ├── book.ts
│   │   ├── user.ts
│   │   ├── ai.ts
│   │   └── common.ts
│   ├── styles/                # 样式文件
│   │   ├── globals.css
│   │   ├── variables.css
│   │   └── components.css
│   └── assets/                # 静态资源
│       ├── images/
│       ├── icons/
│       └── fonts/
├── tests/                     # 测试文件
│   ├── components/
│   ├── pages/
│   ├── utils/
│   └── setup.ts
├── docs/                      # 文档
├── .env.example              # 环境变量示例
├── .eslintrc.js              # ESLint配置
├── .prettierrc               # Prettier配置
├── tailwind.config.js        # Tailwind配置
├── tsconfig.json             # TypeScript配置
├── vite.config.ts            # Vite配置
└── package.json              # 依赖管理
```

## 核心组件设计

### 1. 书籍阅读器组件 (BookReader)

```typescript
// types/book.ts
export interface Book {
  id: string;
  title: string;
  author: string;
  coverUrl: string;
  totalPages: number;
  category: string;
  description: string;
}

export interface BookContent {
  pageNumber: number;
  content: string;
  wordCount: number;
}

export interface ReadingProgress {
  bookId: string;
  currentPage: number;
  progressPercentage: number;
  lastReadAt: Date;
}

// components/book/BookReader/BookReader.tsx
interface BookReaderProps {
  bookId: string;
  initialPage?: number;
  onPageChange?: (page: number) => void;
  onProgressUpdate?: (progress: ReadingProgress) => void;
}

export const BookReader: React.FC<BookReaderProps> = ({
  bookId,
  initialPage = 1,
  onPageChange,
  onProgressUpdate
}) => {
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [fontSize, setFontSize] = useState(16);
  const [isDarkMode, setIsDarkMode] = useState(false);
  
  const { data: bookContent, isLoading } = useBookContent(bookId, currentPage);
  const { mutate: updateProgress } = useUpdateReadingProgress();
  
  // 阅读器核心功能实现
  const handlePageChange = useCallback((newPage: number) => {
    setCurrentPage(newPage);
    onPageChange?.(newPage);
    
    // 更新阅读进度
    const progress: ReadingProgress = {
      bookId,
      currentPage: newPage,
      progressPercentage: (newPage / bookContent?.totalPages) * 100,
      lastReadAt: new Date()
    };
    
    updateProgress(progress);
    onProgressUpdate?.(progress);
  }, [bookId, bookContent?.totalPages, onPageChange, onProgressUpdate, updateProgress]);
  
  return (
    <div className={`book-reader ${isDarkMode ? 'dark' : ''}`}>
      <ReaderToolbar 
        fontSize={fontSize}
        onFontSizeChange={setFontSize}
        isDarkMode={isDarkMode}
        onDarkModeToggle={setIsDarkMode}
      />
      <ReaderContent 
        content={bookContent?.content}
        fontSize={fontSize}
        isLoading={isLoading}
      />
      <ReaderNavigation 
        currentPage={currentPage}
        totalPages={bookContent?.totalPages}
        onPageChange={handlePageChange}
      />
    </div>
  );
};
```

### 2. AI搜索组件 (AISearch)

```typescript
// components/ai/AISearch/AISearch.tsx
interface AISearchProps {
  placeholder?: string;
  onSearch: (query: string, type: 'traditional' | 'ai') => void;
  suggestions?: string[];
  recentSearches?: string[];
}

export const AISearch: React.FC<AISearchProps> = ({
  placeholder = "智能搜索书籍内容...",
  onSearch,
  suggestions = [],
  recentSearches = []
}) => {
  const [query, setQuery] = useState('');
  const [searchType, setSearchType] = useState<'traditional' | 'ai'>('ai');
  const [isVoiceActive, setIsVoiceActive] = useState(false);
  
  const { mutate: searchBooks, isLoading } = useAISearch();
  
  const handleSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      onSearch(query.trim(), searchType);
      searchBooks({ query: query.trim(), type: searchType });
    }
  }, [query, searchType, onSearch, searchBooks]);
  
  const handleVoiceSearch = useCallback(() => {
    if ('webkitSpeechRecognition' in window) {
      const recognition = new webkitSpeechRecognition();
      recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setQuery(transcript);
      };
      recognition.start();
      setIsVoiceActive(true);
    }
  }, []);
  
  return (
    <div className="ai-search">
      <form onSubmit={handleSubmit} className="search-form">
        <div className="search-input-container">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={placeholder}
            className="search-input"
          />
          <button
            type="button"
            onClick={handleVoiceSearch}
            className={`voice-button ${isVoiceActive ? 'active' : ''}`}
          >
            🎤
          </button>
        </div>
        
        <div className="search-options">
          <label>
            <input
              type="radio"
              checked={searchType === 'traditional'}
              onChange={() => setSearchType('traditional')}
            />
            传统搜索
          </label>
          <label>
            <input
              type="radio"
              checked={searchType === 'ai'}
              onChange={() => setSearchType('ai')}
            />
            AI智能搜索
          </label>
        </div>
        
        <button type="submit" disabled={isLoading} className="search-button">
          {isLoading ? '搜索中...' : '搜索'}
        </button>
      </form>
      
      <SearchSuggestions 
        suggestions={suggestions}
        recentSearches={recentSearches}
        onSuggestionClick={setQuery}
      />
    </div>
  );
};
```

### 3. AI对话组件 (ChatInterface)

```typescript
// components/ai/ChatInterface/ChatInterface.tsx
interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  sources?: BookReference[];
}

interface ChatInterfaceProps {
  conversationId?: string;
  bookContext?: Book;
  onNewConversation?: () => void;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({
  conversationId,
  bookContext,
  onNewConversation
}) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  
  const { mutate: sendMessage } = useChatMutation();
  
  const handleSendMessage = useCallback(async () => {
    if (!inputMessage.trim()) return;
    
    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content: inputMessage.trim(),
      timestamp: new Date()
    };
    
    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsStreaming(true);
    
    // 流式响应处理
    const eventSource = new EventSource(`/api/v1/ai/chat/stream`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getAuthToken()}`
      }
    });
    
    let assistantMessage = '';
    
    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      
      if (data.type === 'content') {
        assistantMessage += data.content;
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMessage = newMessages[newMessages.length - 1];
          
          if (lastMessage?.role === 'assistant') {
            lastMessage.content = assistantMessage;
          } else {
            newMessages.push({
              id: generateId(),
              role: 'assistant',
              content: assistantMessage,
              timestamp: new Date(),
              sources: data.sources
            });
          }
          
          return newMessages;
        });
      } else if (data.type === 'done') {
        setIsStreaming(false);
        eventSource.close();
      }
    };
    
    eventSource.onerror = () => {
      setIsStreaming(false);
      eventSource.close();
    };
  }, [inputMessage, sendMessage]);
  
  return (
    <div className="chat-interface">
      <ChatHeader 
        bookContext={bookContext}
        onNewConversation={onNewConversation}
      />
      
      <MessageList 
        messages={messages}
        isStreaming={isStreaming}
      />
      
      <ChatInput
        value={inputMessage}
        onChange={setInputMessage}
        onSend={handleSendMessage}
        disabled={isStreaming}
        placeholder="询问关于书籍的任何问题..."
      />
    </div>
  );
};
```

## 状态管理架构

### Zustand Store设计

```typescript
// store/authStore.ts
interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: localStorage.getItem('auth_token'),
  isAuthenticated: false,
  
  login: async (credentials) => {
    const response = await authAPI.login(credentials);
    set({
      user: response.user,
      token: response.token,
      isAuthenticated: true
    });
    localStorage.setItem('auth_token', response.token);
  },
  
  logout: () => {
    set({ user: null, token: null, isAuthenticated: false });
    localStorage.removeItem('auth_token');
  },
  
  refreshToken: async () => {
    const token = get().token;
    if (token) {
      const response = await authAPI.refreshToken(token);
      set({ token: response.token });
      localStorage.setItem('auth_token', response.token);
    }
  }
}));

// store/booksStore.ts
interface BooksState {
  books: Book[];
  currentBook: Book | null;
  readingProgress: Record<string, ReadingProgress>;
  bookmarks: Record<string, Bookmark[]>;
  searchResults: SearchResult[];
  isLoading: boolean;
  
  // Actions
  fetchBooks: (params: BookQuery) => Promise<void>;
  setCurrentBook: (book: Book) => void;
  updateReadingProgress: (bookId: string, progress: ReadingProgress) => void;
  addBookmark: (bookmark: Bookmark) => void;
  searchBooks: (query: string) => Promise<void>;
}

export const useBooksStore = create<BooksState>((set, get) => ({
  books: [],
  currentBook: null,
  readingProgress: {},
  bookmarks: {},
  searchResults: [],
  isLoading: false,
  
  fetchBooks: async (params) => {
    set({ isLoading: true });
    try {
      const books = await booksAPI.getBooks(params);
      set({ books, isLoading: false });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },
  
  setCurrentBook: (book) => set({ currentBook: book }),
  
  updateReadingProgress: (bookId, progress) => {
    set(state => ({
      readingProgress: {
        ...state.readingProgress,
        [bookId]: progress
      }
    }));
  },
  
  addBookmark: (bookmark) => {
    set(state => ({
      bookmarks: {
        ...state.bookmarks,
        [bookmark.bookId]: [
          ...(state.bookmarks[bookmark.bookId] || []),
          bookmark
        ]
      }
    }));
  },
  
  searchBooks: async (query) => {
    set({ isLoading: true });
    try {
      const results = await booksAPI.searchBooks(query);
      set({ searchResults: results, isLoading: false });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  }
}));

// store/aiStore.ts
interface AIState {
  conversations: Conversation[];
  currentConversation: Conversation | null;
  aiSearchResults: AISearchResult[];
  isProcessing: boolean;
  
  // Actions
  createConversation: (bookId?: string) => void;
  sendMessage: (message: string) => Promise<void>;
  aiSearch: (query: string) => Promise<void>;
  getRecommendations: (userId: string) => Promise<void>;
}
```

## 自定义Hooks设计

### 1. 书籍相关Hooks

```typescript
// hooks/useBooks.ts
export const useBooks = (params?: BookQuery) => {
  return useQuery({
    queryKey: ['books', params],
    queryFn: () => booksAPI.getBooks(params),
    staleTime: 5 * 60 * 1000, // 5分钟
    cacheTime: 10 * 60 * 1000, // 10分钟
  });
};

export const useBookContent = (bookId: string, page: number) => {
  return useQuery({
    queryKey: ['book-content', bookId, page],
    queryFn: () => booksAPI.getBookContent(bookId, page),
    enabled: !!bookId && page > 0,
    staleTime: 30 * 60 * 1000, // 30分钟缓存
  });
};

export const useReadingProgress = (bookId: string) => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (progress: ReadingProgress) => 
      booksAPI.updateReadingProgress(bookId, progress),
    onSuccess: () => {
      queryClient.invalidateQueries(['reading-progress', bookId]);
    },
  });
};

// hooks/useBookReader.ts
export const useBookReader = (bookId: string) => {
  const [currentPage, setCurrentPage] = useState(1);
  const [readerSettings, setReaderSettings] = useLocalStorage('reader-settings', {
    fontSize: 16,
    isDarkMode: false,
    lineHeight: 1.6
  });
  
  const { data: book } = useBook(bookId);
  const { data: content } = useBookContent(bookId, currentPage);
  const { mutate: updateProgress } = useReadingProgress(bookId);
  
  const goToPage = useCallback((page: number) => {
    if (page >= 1 && page <= (book?.totalPages || 1)) {
      setCurrentPage(page);
      updateProgress({
        bookId,
        currentPage: page,
        progressPercentage: (page / (book?.totalPages || 1)) * 100,
        lastReadAt: new Date()
      });
    }
  }, [book?.totalPages, bookId, updateProgress]);
  
  const nextPage = useCallback(() => {
    goToPage(currentPage + 1);
  }, [currentPage, goToPage]);
  
  const previousPage = useCallback(() => {
    goToPage(currentPage - 1);
  }, [currentPage, goToPage]);
  
  return {
    book,
    content,
    currentPage,
    readerSettings,
    setReaderSettings,
    goToPage,
    nextPage,
    previousPage,
    canGoNext: currentPage < (book?.totalPages || 1),
    canGoPrevious: currentPage > 1
  };
};
```

### 2. AI相关Hooks

```typescript
// hooks/useAI.ts
export const useAISearch = () => {
  return useMutation({
    mutationFn: (request: AISearchRequest) => aiAPI.search(request),
    onSuccess: (data) => {
      // 缓存搜索结果
      queryClient.setQueryData(['ai-search', request.query], data);
    },
  });
};

export const useAIChat = (conversationId?: string) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  
  const sendMessage = useCallback(async (content: string) => {
    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content,
      timestamp: new Date()
    };
    
    setMessages(prev => [...prev, userMessage]);
    setIsStreaming(true);
    
    try {
      const response = await aiAPI.sendMessage({
        conversationId,
        message: content,
        stream: true
      });
      
      // 处理流式响应
      const reader = response.body?.getReader();
      let assistantMessage = '';
      
      while (reader) {
        const { done, value } = await reader.read();
        if (done) break;
        
        const chunk = new TextDecoder().decode(value);
        assistantMessage += chunk;
        
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMessage = newMessages[newMessages.length - 1];
          
          if (lastMessage?.role === 'assistant') {
            lastMessage.content = assistantMessage;
          } else {
            newMessages.push({
              id: generateId(),
              role: 'assistant',
              content: assistantMessage,
              timestamp: new Date()
            });
          }
          
          return newMessages;
        });
      }
    } finally {
      setIsStreaming(false);
    }
  }, [conversationId]);
  
  return {
    messages,
    isStreaming,
    sendMessage
  };
};
```

## 路由设计

```typescript
// app/router.tsx
import { createBrowserRouter } from 'react-router-dom';
import { lazy, Suspense } from 'react';

// 懒加载页面组件
const Home = lazy(() => import('../pages/Home'));
const Library = lazy(() => import('../pages/Library'));
const BookDetail = lazy(() => import('../pages/BookDetail'));
const Reader = lazy(() => import('../pages/Reader'));
const AIChat = lazy(() => import('../pages/AIChat'));
const Profile = lazy(() => import('../pages/Profile'));

const LoadingSpinner = () => (
  <div className="flex items-center justify-center h-64">
    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
  </div>
);

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<LoadingSpinner />}>
            <Home />
          </Suspense>
        )
      },
      {
        path: 'library',
        element: (
          <Suspense fallback={<LoadingSpinner />}>
            <Library />
          </Suspense>
        )
      },
      {
        path: 'books/:bookId',
        element: (
          <Suspense fallback={<LoadingSpinner />}>
            <BookDetail />
          </Suspense>
        )
      },
      {
        path: 'reader/:bookId',
        element: (
          <Suspense fallback={<LoadingSpinner />}>
            <Reader />
          </Suspense>
        )
      },
      {
        path: 'ai-chat',
        element: (
          <Suspense fallback={<LoadingSpinner />}>
            <AIChat />
          </Suspense>
        )
      },
      {
        path: 'profile',
        element: (
          <Suspense fallback={<LoadingSpinner />}>
            <Profile />
          </Suspense>
        ),
        loader: requireAuth
      }
    ]
  }
]);

// 路由守卫
const requireAuth = () => {
  const { isAuthenticated } = useAuthStore();
  if (!isAuthenticated) {
    throw redirect('/login');
  }
  return null;
};
```

## API服务层设计

```typescript
// services/api.ts
import axios, { AxiosInstance } from 'axios';

class APIClient {
  private client: AxiosInstance;
  
  constructor(baseURL: string) {
    this.client = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    this.setupInterceptors();
  }
  
  private setupInterceptors() {
    // 请求拦截器
    this.client.interceptors.request.use(
      (config) => {
        const token = useAuthStore.getState().token;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );
    
    // 响应拦截器
    this.client.interceptors.response.use(
      (response) => response.data,
      async (error) => {
        if (error.response?.status === 401) {
          // Token过期，尝试刷新
          try {
            await useAuthStore.getState().refreshToken();
            return this.client.request(error.config);
          } catch (refreshError) {
            useAuthStore.getState().logout();
            window.location.href = '/login';
          }
        }
        return Promise.reject(error);
      }
    );
  }
  
  // 通用请求方法
  async get<T>(url: string, params?: any): Promise<T> {
    return this.client.get(url, { params });
  }
  
  async post<T>(url: string, data?: any): Promise<T> {
    return this.client.post(url, data);
  }
  
  async put<T>(url: string, data?: any): Promise<T> {
    return this.client.put(url, data);
  }
  
  async delete<T>(url: string): Promise<T> {
    return this.client.delete(url);
  }
}

export const apiClient = new APIClient(
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
);

// services/books.ts
export const booksAPI = {
  getBooks: (params: BookQuery): Promise<PageResult<Book>> =>
    apiClient.get('/books', params),
    
  getBook: (id: string): Promise<Book> =>
    apiClient.get(`/books/${id}`),
    
  getBookContent: (id: string, page: number): Promise<BookContent> =>
    apiClient.get(`/books/${id}/content`, { page }),
    
  searchBooks: (query: string): Promise<SearchResult[]> =>
    apiClient.get('/books/search', { q: query }),
    
  updateReadingProgress: (bookId: string, progress: ReadingProgress): Promise<void> =>
    apiClient.put(`/books/${bookId}/progress`, progress),
    
  addBookmark: (bookmark: Bookmark): Promise<Bookmark> =>
    apiClient.post('/bookmarks', bookmark),
    
  getBookmarks: (bookId: string): Promise<Bookmark[]> =>
    apiClient.get(`/books/${bookId}/bookmarks`)
};

// services/ai.ts
export const aiAPI = {
  search: (request: AISearchRequest): Promise<AISearchResponse> =>
    apiClient.post('/ai/search', request),
    
  sendMessage: (request: ChatRequest): Promise<Response> =>
    fetch(`${apiClient.defaults.baseURL}/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${useAuthStore.getState().token}`
      },
      body: JSON.stringify(request)
    }),
    
  getRecommendations: (userId: string): Promise<BookRecommendation[]> =>
    apiClient.get(`/ai/recommendations/${userId}`)
};
```

## 性能优化策略

### 1. 代码分割和懒加载

```typescript
// 路由级别代码分割
const BookReader = lazy(() => 
  import('../pages/Reader').then(module => ({
    default: module.Reader
  }))
);

// 组件级别代码分割
const HeavyComponent = lazy(() => import('./HeavyComponent'));

// 条件加载
const AIFeatures = lazy(() => 
  import('./AIFeatures').then(module => 
    import.meta.env.VITE_ENABLE_AI === 'true' 
      ? { default: module.AIFeatures }
      : { default: () => null }
  )
);
```

### 2. 数据缓存策略

```typescript
// React Query配置
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5分钟
      cacheTime: 10 * 60 * 1000, // 10分钟
      retry: (failureCount, error) => {
        if (error.response?.status === 404) return false;
        return failureCount < 3;
      },
    },
    mutations: {
      retry: 1,
    },
  },
});

// 预加载策略
export const usePrefetchBook = () => {
  const queryClient = useQueryClient();
  
  return useCallback((bookId: string) => {
    queryClient.prefetchQuery({
      queryKey: ['book', bookId],
      queryFn: () => booksAPI.getBook(bookId),
    });
  }, [queryClient]);
};
```

### 3. 虚拟化长列表

```typescript
// components/book/VirtualBookList.tsx
import { FixedSizeList as List } from 'react-window';

interface VirtualBookListProps {
  books: Book[];
  height: number;
  itemHeight: number;
  onBookClick: (book: Book) => void;
}

export const VirtualBookList: React.FC<VirtualBookListProps> = ({
  books,
  height,
  itemHeight,
  onBookClick
}) => {
  const Row = ({ index, style }: { index: number; style: React.CSSProperties }) => (
    <div style={style}>
      <BookCard 
        book={books[index]} 
        onClick={() => onBookClick(books[index])}
      />
    </div>
  );
  
  return (
    <List
      height={height}
      itemCount={books.length}
      itemSize={itemHeight}
      width="100%"
    >
      {Row}
    </List>
  );
};
```

## 响应式设计

### 1. 断点系统

```css
/* styles/variables.css */
:root {
  /* 断点定义 */
  --breakpoint-sm: 640px;
  --breakpoint-md: 768px;
  --breakpoint-lg: 1024px;
  --breakpoint-xl: 1280px;
  --breakpoint-2xl: 1536px;
  
  /* 间距系统 */
  --spacing-xs: 0.25rem;
  --spacing-sm: 0.5rem;
  --spacing-md: 1rem;
  --spacing-lg: 1.5rem;
  --spacing-xl: 2rem;
  
  /* 颜色系统 */
  --color-primary: #3b82f6;
  --color-secondary: #6b7280;
  --color-accent: #f59e0b;
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-error: #ef4444;
}
```

### 2. 自适应布局

```typescript
// hooks/useResponsive.ts
export const useResponsive = () => {
  const [breakpoint, setBreakpoint] = useState<'sm' | 'md' | 'lg' | 'xl'>('lg');
  
  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      if (width < 768) setBreakpoint('sm');
      else if (width < 1024) setBreakpoint('md');
      else if (width < 1280) setBreakpoint('lg');
      else setBreakpoint('xl');
    };
    
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);
  
  return {
    breakpoint,
    isMobile: breakpoint === 'sm',
    isTablet: breakpoint === 'md',
    isDesktop: breakpoint === 'lg' || breakpoint === 'xl'
  };
};
```

## 测试策略

### 1. 单元测试

```typescript
// tests/components/BookReader.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { BookReader } from '../components/book/BookReader';

describe('BookReader', () => {
  const mockBook = {
    id: '1',
    title: 'Test Book',
    totalPages: 100
  };
  
  it('should render book content', () => {
    render(<BookReader bookId="1" />);
    expect(screen.getByTestId('book-reader')).toBeInTheDocument();
  });
  
  it('should handle page navigation', () => {
    const onPageChange = jest.fn();
    render(<BookReader bookId="1" onPageChange={onPageChange} />);
    
    fireEvent.click(screen.getByTestId('next-page-button'));
    expect(onPageChange).toHaveBeenCalledWith(2);
  });
});
```

### 2. E2E测试

```typescript
// tests/e2e/book-reading.spec.ts
import { test, expect } from '@playwright/test';

test('user can read a book', async ({ page }) => {
  await page.goto('/');
  
  // 搜索书籍
  await page.fill('[data-testid=search-input]', 'JavaScript');
  await page.click('[data-testid=search-button]');
  
  // 选择书籍
  await page.click('[data-testid=book-card]:first-child');
  
  // 开始阅读
  await page.click('[data-testid=start-reading-button]');
  
  // 验证阅读器
  await expect(page.locator('[data-testid=book-reader]')).toBeVisible();
  
  // 翻页测试
  await page.click('[data-testid=next-page-button]');
  await expect(page.locator('[data-testid=page-number]')).toContainText('2');
});
```

## 构建和部署配置

### Vite配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@components': resolve(__dirname, 'src/components'),
      '@pages': resolve(__dirname, 'src/pages'),
      '@hooks': resolve(__dirname, 'src/hooks'),
      '@services': resolve(__dirname, 'src/services'),
      '@utils': resolve(__dirname, 'src/utils'),
      '@types': resolve(__dirname, 'src/types'),
    },
  },
  build: {
    target: 'es2020',
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          router: ['react-router-dom'],
          ui: ['antd', '@ant-design/icons'],
          utils: ['lodash', 'dayjs', 'axios'],
        },
      },
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### 环境配置

```typescript
// .env.example
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_ENABLE_AI=true
VITE_AI_STREAM_ENDPOINT=ws://localhost:8080/ws/ai
VITE_SENTRY_DSN=your_sentry_dsn
VITE_GOOGLE_ANALYTICS_ID=your_ga_id
```

这个前端架构设计提供了:
1. **现代化技术栈**: React 18 + Vite + TypeScript
2. **模块化设计**: 清晰的组件和服务分层
3. **性能优化**: 代码分割、缓存、虚拟化
4. **类型安全**: 完整的TypeScript类型定义
5. **测试覆盖**: 单元测试和E2E测试
6. **响应式设计**: 移动端友好
7. **AI集成**: 流式响应和智能交互

该架构可以支持从开发到生产的完整生命周期。