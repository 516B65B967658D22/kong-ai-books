# 部署指南 - Kong AI Books

## 部署架构概览

### 生产环境架构
```
                    ┌─────────────┐
                    │   用户访问   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   CDN/WAF   │ ← Cloudflare/AWS CloudFront
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Load Balancer│ ← Nginx/HAProxy
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │Frontend │       │Frontend │       │Frontend │
   │Instance │       │Instance │       │Instance │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    ┌──────▼──────┐
                    │ API Gateway │ ← Kong/Zuul
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │Backend  │       │Backend  │       │Backend  │
   │Instance │       │Instance │       │Instance │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
    ┌─────────────────────┼─────────────────────┐
    │                     │                     │
┌───▼───┐         ┌──────▼──────┐         ┌───▼───┐
│ Redis │         │ PostgreSQL  │         │ AI    │
│Cluster│         │   Cluster   │         │Service│
└───────┘         └─────────────┘         └───────┘
```

## 容器化部署

### 1. Docker Compose - 开发环境

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  # 前端开发服务
  frontend-dev:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    ports:
      - "3000:3000"
    volumes:
      - ./frontend:/app
      - /app/node_modules
    environment:
      - VITE_API_BASE_URL=http://localhost:8080/api/v1
      - VITE_WS_URL=ws://localhost:8080/ws
    depends_on:
      - backend

  # 后端服务
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
      - "5005:5005" # 调试端口
    volumes:
      - ./backend:/app
      - ~/.m2:/root/.m2
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DATABASE_URL=postgresql://postgres:5432/kong_ai_books
      - REDIS_URL=redis://redis:6379
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CHROMA_URL=http://chroma:8000
    depends_on:
      - postgres
      - redis
      - chroma

  # 数据库
  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=kong_ai_books
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=dev_password
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
      - ./database/init:/docker-entrypoint-initdb.d

  # 缓存
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_dev_data:/data

  # 向量数据库
  chroma:
    image: chromadb/chroma:latest
    ports:
      - "8000:8000"
    environment:
      - CHROMA_SERVER_HOST=0.0.0.0
    volumes:
      - chroma_dev_data:/chroma/chroma

  # 消息队列
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin
    volumes:
      - rabbitmq_dev_data:/var/lib/rabbitmq

volumes:
  postgres_dev_data:
  redis_dev_data:
  chroma_dev_data:
  rabbitmq_dev_data:
```

### 2. Docker Compose - 生产环境

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  # Nginx反向代理
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
      - frontend_static:/var/www/static
    depends_on:
      - frontend
      - backend
    restart: unless-stopped

  # 前端生产构建
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.prod
    volumes:
      - frontend_static:/app/dist
    environment:
      - NODE_ENV=production
    restart: unless-stopped

  # 后端服务集群
  backend-1:
    build:
      context: ./backend
      dockerfile: Dockerfile.prod
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=postgresql://postgres-primary:5432/kong_ai_books
      - REDIS_URL=redis://redis-cluster:6379
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - VECTOR_STORE_URL=${VECTOR_STORE_URL}
      - INSTANCE_ID=backend-1
    depends_on:
      - postgres-primary
      - redis-cluster
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'

  backend-2:
    build:
      context: ./backend
      dockerfile: Dockerfile.prod
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=postgresql://postgres-primary:5432/kong_ai_books
      - REDIS_URL=redis://redis-cluster:6379
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - VECTOR_STORE_URL=${VECTOR_STORE_URL}
      - INSTANCE_ID=backend-2
    depends_on:
      - postgres-primary
      - redis-cluster
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'

  # PostgreSQL主从复制
  postgres-primary:
    image: postgres:15
    environment:
      - POSTGRES_DB=kong_ai_books
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
      - POSTGRES_REPLICATION_USER=replicator
      - POSTGRES_REPLICATION_PASSWORD=${POSTGRES_REPLICATION_PASSWORD}
    volumes:
      - postgres_primary_data:/var/lib/postgresql/data
      - ./database/postgresql.conf:/etc/postgresql/postgresql.conf
      - ./database/pg_hba.conf:/etc/postgresql/pg_hba.conf
    command: postgres -c config_file=/etc/postgresql/postgresql.conf
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2.0'

  postgres-replica:
    image: postgres:15
    environment:
      - PGUSER=postgres
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
      - PGPASSWORD=${POSTGRES_REPLICATION_PASSWORD}
      - POSTGRES_MASTER_SERVICE=postgres-primary
    volumes:
      - postgres_replica_data:/var/lib/postgresql/data
    depends_on:
      - postgres-primary
    restart: unless-stopped

  # Redis集群
  redis-cluster:
    image: redis:7-alpine
    command: redis-server --appendonly yes --cluster-enabled yes
    volumes:
      - redis_cluster_data:/data
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'

  # 监控服务
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  frontend_static:
  postgres_primary_data:
  postgres_replica_data:
  redis_cluster_data:
  prometheus_data:
  grafana_data:

networks:
  default:
    driver: bridge
```

## Kubernetes部署

### 1. 命名空间和配置

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: kong-ai-books
  labels:
    name: kong-ai-books

---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: kong-ai-books
data:
  application.yml: |
    spring:
      profiles:
        active: prod
      datasource:
        url: postgresql://postgres-service:5432/kong_ai_books
        username: postgres
      redis:
        host: redis-service
        port: 6379
    
    app:
      ai:
        vector-store:
          type: pinecone
          url: ${VECTOR_STORE_URL}
      
      storage:
        type: s3
        s3:
          bucket: ${S3_BUCKET}
          region: ${AWS_REGION}

---
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: kong-ai-books
type: Opaque
data:
  database-password: <base64-encoded-password>
  openai-api-key: <base64-encoded-api-key>
  jwt-secret: <base64-encoded-jwt-secret>
  pinecone-api-key: <base64-encoded-pinecone-key>
```

### 2. 应用部署

```yaml
# k8s/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-deployment
  namespace: kong-ai-books
  labels:
    app: backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: kong-ai-books/backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-password
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: openai-api-key
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
      volumes:
      - name: config-volume
        configMap:
          name: app-config

---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: kong-ai-books
spec:
  selector:
    app: backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP

---
# k8s/frontend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend-deployment
  namespace: kong-ai-books
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: kong-ai-books/frontend:latest
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"

---
apiVersion: v1
kind: Service
metadata:
  name: frontend-service
  namespace: kong-ai-books
spec:
  selector:
    app: frontend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 80
  type: ClusterIP
```

### 3. 数据库部署

```yaml
# k8s/postgres-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-primary
  namespace: kong-ai-books
spec:
  serviceName: postgres-service
  replicas: 1
  selector:
    matchLabels:
      app: postgres-primary
  template:
    metadata:
      labels:
        app: postgres-primary
    spec:
      containers:
      - name: postgres
        image: postgres:15
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: "kong_ai_books"
        - name: POSTGRES_USER
          value: "postgres"
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-password
        - name: POSTGRES_REPLICATION_USER
          value: "replicator"
        - name: POSTGRES_REPLICATION_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-replication-password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: postgres-config
          mountPath: /etc/postgresql
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
      volumes:
      - name: postgres-config
        configMap:
          name: postgres-config
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 100Gi

---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: kong-ai-books
spec:
  selector:
    app: postgres-primary
  ports:
  - protocol: TCP
    port: 5432
    targetPort: 5432
  type: ClusterIP
```

### 4. Ingress配置

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kong-ai-books-ingress
  namespace: kong-ai-books
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - kong-ai-books.com
    - api.kong-ai-books.com
    secretName: kong-ai-books-tls
  rules:
  - host: kong-ai-books.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
  - host: api.kong-ai-books.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 80
```

## 自动化部署

### 1. GitHub Actions CI/CD

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json
    
    - name: Set up Java
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'
    
    - name: Run Frontend Tests
      run: |
        cd frontend
        npm ci
        npm run test:ci
        npm run build
    
    - name: Run Backend Tests
      run: |
        cd backend
        ./mvnw clean test
    
    - name: SonarQube Analysis
      uses: sonarqube-quality-gate-action@master
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    strategy:
      matrix:
        component: [frontend, backend]
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.component }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix={{branch}}-
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: ./${{ matrix.component }}
        file: ./${{ matrix.component }}/Dockerfile.prod
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: 'v1.28.0'
    
    - name: Configure kubectl
      run: |
        echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig
    
    - name: Deploy to Kubernetes
      run: |
        export KUBECONFIG=kubeconfig
        
        # 更新镜像标签
        kubectl set image deployment/backend-deployment \
          backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/backend:${{ github.sha }} \
          -n kong-ai-books
        
        kubectl set image deployment/frontend-deployment \
          frontend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/frontend:${{ github.sha }} \
          -n kong-ai-books
        
        # 等待部署完成
        kubectl rollout status deployment/backend-deployment -n kong-ai-books
        kubectl rollout status deployment/frontend-deployment -n kong-ai-books
    
    - name: Run Database Migrations
      run: |
        export KUBECONFIG=kubeconfig
        
        kubectl create job migration-${{ github.sha }} \
          --from=cronjob/database-migration \
          -n kong-ai-books
        
        kubectl wait --for=condition=complete job/migration-${{ github.sha }} \
          --timeout=300s -n kong-ai-books
```

### 2. Helm Chart部署

```yaml
# helm/kong-ai-books/Chart.yaml
apiVersion: v2
name: kong-ai-books
description: Kong AI Books Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"

dependencies:
- name: postgresql
  version: 12.1.9
  repository: https://charts.bitnami.com/bitnami
- name: redis
  version: 17.4.3
  repository: https://charts.bitnami.com/bitnami

---
# helm/kong-ai-books/values.yaml
global:
  imageRegistry: "ghcr.io"
  imagePullSecrets:
    - name: ghcr-secret

frontend:
  replicaCount: 2
  image:
    repository: kong-ai-books/frontend
    tag: latest
    pullPolicy: Always
  
  service:
    type: ClusterIP
    port: 80
  
  resources:
    limits:
      cpu: 200m
      memory: 256Mi
    requests:
      cpu: 100m
      memory: 128Mi

backend:
  replicaCount: 3
  image:
    repository: kong-ai-books/backend
    tag: latest
    pullPolicy: Always
  
  service:
    type: ClusterIP
    port: 80
    targetPort: 8080
  
  resources:
    limits:
      cpu: 1000m
      memory: 2Gi
    requests:
      cpu: 500m
      memory: 1Gi
  
  env:
    SPRING_PROFILES_ACTIVE: "prod"
    OPENAI_API_KEY:
      valueFrom:
        secretKeyRef:
          name: ai-secrets
          key: openai-api-key

postgresql:
  enabled: true
  auth:
    postgresPassword: "your-secure-password"
    database: "kong_ai_books"
  primary:
    persistence:
      enabled: true
      size: 100Gi
      storageClass: "fast-ssd"
  metrics:
    enabled: true

redis:
  enabled: true
  auth:
    enabled: false
  master:
    persistence:
      enabled: true
      size: 20Gi

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
  hosts:
    - host: kong-ai-books.com
      paths:
        - path: /
          pathType: Prefix
          service: frontend
    - host: api.kong-ai-books.com
      paths:
        - path: /
          pathType: Prefix
          service: backend
  tls:
    - secretName: kong-ai-books-tls
      hosts:
        - kong-ai-books.com
        - api.kong-ai-books.com

monitoring:
  prometheus:
    enabled: true
  grafana:
    enabled: true
    adminPassword: "admin-password"
```

### 3. Helm部署命令

```bash
# 部署脚本
#!/bin/bash
# scripts/deploy.sh

NAMESPACE="kong-ai-books"
RELEASE_NAME="kong-ai-books"
CHART_PATH="./helm/kong-ai-books"

# 创建命名空间
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# 添加Helm仓库
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# 安装/升级应用
helm upgrade --install $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --values ./helm/kong-ai-books/values.prod.yaml \
  --set backend.image.tag=$IMAGE_TAG \
  --set frontend.image.tag=$IMAGE_TAG \
  --wait \
  --timeout=600s

# 验证部署
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE
kubectl get ingress -n $NAMESPACE

echo "Deployment completed successfully!"
```

## 监控和日志

### 1. Prometheus监控配置

```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

scrape_configs:
  - job_name: 'kong-ai-books-backend'
    static_configs:
      - targets: ['backend-service:80']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
  
  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx-exporter:9113']

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

---
# monitoring/alert_rules.yml
groups:
- name: kong-ai-books-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }} errors per second"
  
  - alert: DatabaseConnectionHigh
    expr: pg_stat_activity_count > 80
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High database connections"
      description: "Database has {{ $value }} active connections"
  
  - alert: AIServiceDown
    expr: up{job="kong-ai-books-backend"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "AI service is down"
      description: "AI service has been down for more than 1 minute"
```

### 2. Grafana仪表板

```json
{
  "dashboard": {
    "title": "Kong AI Books - System Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{method}} {{endpoint}}"
          }
        ]
      },
      {
        "title": "AI Service Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(ai_response_time_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(ai_response_time_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ]
      },
      {
        "title": "Database Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(pg_stat_database_tup_fetched[5m])",
            "legendFormat": "Rows fetched/sec"
          },
          {
            "expr": "rate(pg_stat_database_tup_inserted[5m])",
            "legendFormat": "Rows inserted/sec"
          }
        ]
      }
    ]
  }
}
```

### 3. 日志聚合

```yaml
# logging/fluentd-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-config
  namespace: kong-ai-books
data:
  fluent.conf: |
    <source>
      @type tail
      path /var/log/containers/*kong-ai-books*.log
      pos_file /var/log/fluentd-containers.log.pos
      tag kubernetes.*
      format json
      read_from_head true
    </source>
    
    <filter kubernetes.**>
      @type kubernetes_metadata
    </filter>
    
    <match kubernetes.**>
      @type elasticsearch
      host elasticsearch-service
      port 9200
      index_name kong-ai-books
      type_name _doc
    </match>

---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
  namespace: kong-ai-books
spec:
  selector:
    matchLabels:
      name: fluentd
  template:
    metadata:
      labels:
        name: fluentd
    spec:
      containers:
      - name: fluentd
        image: fluent/fluentd-kubernetes-daemonset:v1-debian-elasticsearch
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: config-volume
          mountPath: /fluentd/etc
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: config-volume
        configMap:
          name: fluentd-config
```

## 环境配置

### 1. 环境变量管理

```bash
# .env.production
# 数据库配置
DATABASE_URL=postgresql://postgres:password@postgres-primary:5432/kong_ai_books
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-secure-password
DATABASE_POOL_SIZE=20

# Redis配置
REDIS_URL=redis://redis-cluster:6379
REDIS_PASSWORD=your-redis-password

# AI服务配置
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-4
PINECONE_API_KEY=your-pinecone-api-key
PINECONE_ENVIRONMENT=us-west1-gcp
PINECONE_INDEX=kong-ai-books

# 存储配置
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
S3_BUCKET=kong-ai-books-storage
AWS_REGION=us-west-2

# 安全配置
JWT_SECRET=your-jwt-secret-key
ENCRYPTION_KEY=your-encryption-key

# 监控配置
SENTRY_DSN=your-sentry-dsn
GRAFANA_PASSWORD=your-grafana-password

# 应用配置
APP_DOMAIN=kong-ai-books.com
API_DOMAIN=api.kong-ai-books.com
CDN_URL=https://cdn.kong-ai-books.com
```

### 2. 配置验证脚本

```bash
#!/bin/bash
# scripts/validate-config.sh

echo "Validating configuration..."

# 检查必需的环境变量
required_vars=(
    "DATABASE_URL"
    "REDIS_URL" 
    "OPENAI_API_KEY"
    "JWT_SECRET"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "ERROR: $var is not set"
        exit 1
    fi
done

# 测试数据库连接
echo "Testing database connection..."
pg_isready -h postgres-primary -p 5432 -U postgres
if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to database"
    exit 1
fi

# 测试Redis连接
echo "Testing Redis connection..."
redis-cli -h redis-cluster -p 6379 ping
if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to Redis"
    exit 1
fi

# 测试AI服务
echo "Testing AI service..."
curl -s -H "Authorization: Bearer $OPENAI_API_KEY" \
     https://api.openai.com/v1/models > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to OpenAI API"
    exit 1
fi

echo "Configuration validation completed successfully!"
```

## 性能调优

### 1. 数据库调优

```bash
# scripts/tune-database.sh
#!/bin/bash

# PostgreSQL性能调优
cat > /etc/postgresql/15/main/postgresql.conf << EOF
# 内存配置
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 256MB
maintenance_work_mem = 1GB

# 连接配置
max_connections = 200
max_worker_processes = 8
max_parallel_workers = 8
max_parallel_workers_per_gather = 4

# WAL配置
wal_buffers = 64MB
checkpoint_completion_target = 0.9
checkpoint_timeout = 15min
max_wal_size = 4GB
min_wal_size = 1GB

# 查询优化
random_page_cost = 1.1
effective_io_concurrency = 200
default_statistics_target = 100

# 日志配置
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on
EOF

# 重启PostgreSQL
systemctl restart postgresql
```

### 2. 应用性能调优

```yaml
# k8s/backend-tuned.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-deployment-tuned
spec:
  template:
    spec:
      containers:
      - name: backend
        env:
        - name: JAVA_OPTS
          value: >-
            -Xmx1536m
            -Xms1536m
            -XX:+UseG1GC
            -XX:MaxGCPauseMillis=200
            -XX:+UseStringDeduplication
            -XX:+OptimizeStringConcat
            -Dspring.jpa.hibernate.jdbc.batch_size=25
            -Dspring.jpa.hibernate.order_inserts=true
            -Dspring.jpa.hibernate.order_updates=true
            -Dspring.jpa.hibernate.jdbc.batch_versioned_data=true
        
        # JVM调优参数
        - name: JVM_OPTS
          value: >-
            -server
            -Djava.awt.headless=true
            -Djava.security.egd=file:/dev/./urandom
            -Dfile.encoding=UTF-8
            -Duser.timezone=Asia/Shanghai
```

## 灾难恢复

### 1. 备份策略

```bash
#!/bin/bash
# scripts/disaster-recovery.sh

# 多区域备份策略
BACKUP_REGIONS=("us-west-2" "us-east-1" "eu-west-1")
DATE=$(date +%Y%m%d_%H%M%S)

for region in "${BACKUP_REGIONS[@]}"; do
    echo "Creating backup in region: $region"
    
    # 数据库备份
    aws s3 cp /backup/postgres/full_backup_$DATE.dump \
        s3://kong-ai-books-backup-$region/database/ \
        --region $region
    
    # 文件存储备份
    aws s3 sync /data/books/ \
        s3://kong-ai-books-backup-$region/books/ \
        --region $region \
        --delete
    
    # 配置备份
    kubectl get all -n kong-ai-books -o yaml > k8s-backup-$DATE.yaml
    aws s3 cp k8s-backup-$DATE.yaml \
        s3://kong-ai-books-backup-$region/k8s/ \
        --region $region
done

echo "Multi-region backup completed"
```

### 2. 恢复流程

```bash
#!/bin/bash
# scripts/restore-from-disaster.sh

BACKUP_REGION=${1:-us-west-2}
BACKUP_DATE=${2:-latest}

echo "Starting disaster recovery from region: $BACKUP_REGION"

# 1. 恢复Kubernetes配置
if [ "$BACKUP_DATE" = "latest" ]; then
    BACKUP_FILE=$(aws s3 ls s3://kong-ai-books-backup-$BACKUP_REGION/k8s/ \
        --region $BACKUP_REGION | sort | tail -n 1 | awk '{print $4}')
else
    BACKUP_FILE="k8s-backup-$BACKUP_DATE.yaml"
fi

aws s3 cp s3://kong-ai-books-backup-$BACKUP_REGION/k8s/$BACKUP_FILE \
    ./k8s-restore.yaml --region $BACKUP_REGION

kubectl apply -f ./k8s-restore.yaml

# 2. 恢复数据库
if [ "$BACKUP_DATE" = "latest" ]; then
    DB_BACKUP=$(aws s3 ls s3://kong-ai-books-backup-$BACKUP_REGION/database/ \
        --region $BACKUP_REGION | sort | tail -n 1 | awk '{print $4}')
else
    DB_BACKUP="full_backup_$BACKUP_DATE.dump"
fi

aws s3 cp s3://kong-ai-books-backup-$BACKUP_REGION/database/$DB_BACKUP \
    ./db-restore.dump --region $BACKUP_REGION

# 在Kubernetes中恢复数据库
kubectl exec -n kong-ai-books postgres-primary-0 -- \
    pg_restore -U postgres -d kong_ai_books /backup/db-restore.dump

# 3. 恢复文件存储
aws s3 sync s3://kong-ai-books-backup-$BACKUP_REGION/books/ \
    /data/books/ --region $BACKUP_REGION

# 4. 验证恢复
echo "Verifying disaster recovery..."
kubectl get pods -n kong-ai-books
kubectl logs -n kong-ai-books deployment/backend-deployment

echo "Disaster recovery completed from region: $BACKUP_REGION"
```

## 安全加固

### 1. 网络安全

```yaml
# k8s/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: kong-ai-books-network-policy
  namespace: kong-ai-books
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  
  ingress:
  # 允许来自Ingress的流量
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 80
  
  # 允许内部服务间通信
  - from:
    - podSelector: {}
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 5432
    - protocol: TCP
      port: 6379
  
  egress:
  # 允许DNS解析
  - to: []
    ports:
    - protocol: UDP
      port: 53
  
  # 允许访问外部AI服务
  - to: []
    ports:
    - protocol: TCP
      port: 443
```

### 2. Pod安全策略

```yaml
# k8s/pod-security-policy.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: kong-ai-books-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

## 部署检查清单

### 1. 部署前检查

```bash
#!/bin/bash
# scripts/pre-deployment-check.sh

echo "=== Pre-deployment Checklist ==="

# 1. 检查镜像版本
echo "Checking image versions..."
docker images | grep kong-ai-books

# 2. 检查配置文件
echo "Validating configuration files..."
yamllint k8s/*.yaml
helm lint helm/kong-ai-books/

# 3. 检查密钥和证书
echo "Checking secrets and certificates..."
kubectl get secrets -n kong-ai-books
kubectl get certificates -n kong-ai-books

# 4. 检查资源配额
echo "Checking resource quotas..."
kubectl describe quota -n kong-ai-books

# 5. 运行集成测试
echo "Running integration tests..."
cd backend && ./mvnw test -Dtest=IntegrationTests

# 6. 检查外部依赖
echo "Checking external dependencies..."
curl -s https://api.openai.com/v1/models > /dev/null
if [ $? -eq 0 ]; then
    echo "✓ OpenAI API accessible"
else
    echo "✗ OpenAI API not accessible"
fi

echo "Pre-deployment check completed"
```

### 2. 部署后验证

```bash
#!/bin/bash
# scripts/post-deployment-verify.sh

echo "=== Post-deployment Verification ==="

NAMESPACE="kong-ai-books"
DOMAIN="kong-ai-books.com"

# 1. 检查Pod状态
echo "Checking pod status..."
kubectl get pods -n $NAMESPACE
kubectl wait --for=condition=ready pod -l app=backend -n $NAMESPACE --timeout=300s

# 2. 检查服务健康
echo "Checking service health..."
for service in frontend backend postgres redis; do
    kubectl get svc $service-service -n $NAMESPACE
done

# 3. 测试API端点
echo "Testing API endpoints..."
curl -f https://api.$DOMAIN/actuator/health
curl -f https://api.$DOMAIN/api/v1/books?page=0&size=10

# 4. 测试AI功能
echo "Testing AI functionality..."
curl -X POST https://api.$DOMAIN/api/v1/ai/search \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TEST_TOKEN" \
    -d '{"query": "机器学习", "topK": 5}'

# 5. 检查监控指标
echo "Checking monitoring metrics..."
curl -s http://prometheus:9090/api/v1/query?query=up | jq '.data.result'

# 6. 验证SSL证书
echo "Verifying SSL certificate..."
echo | openssl s_client -servername $DOMAIN -connect $DOMAIN:443 2>/dev/null | \
    openssl x509 -noout -dates

echo "Post-deployment verification completed"
```

这个部署指南提供了:

1. **完整的部署方案**: 从开发到生产的全套部署配置
2. **容器化支持**: Docker和Kubernetes完整配置
3. **自动化CI/CD**: GitHub Actions和Helm自动化部署
4. **监控告警**: Prometheus、Grafana、日志聚合
5. **高可用设计**: 负载均衡、数据库集群、故障转移
6. **安全加固**: 网络策略、安全配置、证书管理
7. **灾难恢复**: 备份策略、多区域部署、恢复流程

该部署方案可以支持从小规模到大规模的生产环境需求。