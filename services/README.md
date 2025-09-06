# Holo-Oshi-Finder MSA Services

## Architecture Overview

This project implements a microservices architecture for the Holo-Oshi-Finder system using Spring Boot 3.5.5 and Kotlin 2.2.10. The system provides member search, vector similarity search, and AI-powered analysis capabilities through a distributed service ecosystem.

## Service Architecture

### Core Services

| Service | Port | Purpose | Technology Stack |
|---------|------|---------|------------------|
| Eureka Server | 50008 | Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | 50099 | Request Routing, Load Balancing | Spring Cloud Gateway |
| Member Service | 50001 | Member CRUD Operations | Spring WebFlux, R2DBC, PostgreSQL |
| Vector Service | 50002 | Vector Similarity Search | pgvector, Gemini Embeddings |
| LLM Analyzer Service | 50004 | AI Analysis Engine | Google Gemini API |
| Search Service | 50006 | Search Orchestration | Service Integration |
| Notification Service | - | Real-time Notifications | Redis, WebSocket |

### Infrastructure Components

| Component | Port | Purpose | Configuration |
|-----------|------|---------|---------------|
| PostgreSQL | 5433 | Primary Database | TimescaleDB with pgvector extension |
| Redis | 6380 | Caching and Session Store | Single instance, persistence enabled |
| RabbitMQ | 15672 | Event-driven Messaging | Management UI enabled |

## Technical Specifications

### Core Technologies
- **Language**: Kotlin 2.2.10
- **Framework**: Spring Boot 3.5.5
- **Reactive Stack**: Spring WebFlux with Kotlin Coroutines
- **Build System**: Gradle 8.11.1 with Kotlin DSL
- **JVM**: Java 23

### Key Features
- Reactive Programming with Coroutines
- Event-Driven Architecture via RabbitMQ
- Service Discovery and Registration
- Circuit Breaker Pattern Implementation
- Distributed Caching with Redis
- Vector Similarity Search with pgvector
- AI Integration with Google Gemini

### Data Management
- **Database**: 74 member records (67 active, 7 graduated)
- **Vector Embeddings**: 1536-dimension Gemini embeddings
- **Search Performance**: Sub-second response times
- **Cache Strategy**: Redis-based distributed caching

## Build and Deployment

### Prerequisites
```bash
# Required software
java -version    # Java 23
kotlin -version  # Kotlin 2.2.10
gradle -version  # Gradle 8.11.1

# Infrastructure services
docker compose up -d postgres redis rabbitmq
```

### Environment Configuration
Create `.env` file with the following variables:
```bash
# Google AI Configuration
GOOGLE_API_KEY=your_api_key
GOOGLE_PROJECT_ID=your_project_id

# Database Configuration
DB_HOST=localhost
DB_PORT=5433
DB_USER=holo_user
DB_PASSWORD=holo_password
DB_NAME=holo_oshi_db

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6380

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### Build Process
```bash
# Full project build
./gradlew build

# Service-specific build
./gradlew :msa:member-service:build
./gradlew :msa:vector-service:build
./gradlew :msa:llm-analyzer-service:build

# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

### Service Execution

#### Start Infrastructure Services
```bash
# Start required infrastructure
docker compose up -d postgres redis rabbitmq

# Verify services are running
docker ps
```

#### Start MSA Services
```bash
# Load environment variables
source load-env.sh

# Start services in dependency order
./gradlew :msa:eureka-server:bootRun --no-daemon &
./gradlew :msa:member-service:bootRun --no-daemon &
./gradlew :msa:vector-service:bootRun --no-daemon &
./gradlew :msa:llm-analyzer-service:bootRun --no-daemon &
./gradlew :msa:search-service:bootRun --no-daemon &
./gradlew :msa:api-gateway:bootRun --no-daemon &
```

#### Service Health Verification
```bash
# Check service health endpoints
curl http://localhost:50008/actuator/health  # Eureka
curl http://localhost:50001/health           # Member Service
curl http://localhost:50002/health           # Vector Service
curl http://localhost:50004/health           # LLM Analyzer
curl http://localhost:50006/actuator/health  # Search Service
```

## API Documentation

### API Gateway Endpoints
Base URL: `http://localhost:50099`

```
GET  /actuator/health                    # Gateway health check
POST /api/members/**                     # Member service proxy
POST /api/vector/**                      # Vector service proxy
POST /api/search/**                      # Search service proxy
POST /api/analyze/**                     # LLM analyzer proxy
```

### Core API Operations

#### Member Operations
```bash
# List active members
curl "http://localhost:50001/api/members?activeOnly=true"

# Get specific member
curl "http://localhost:50001/api/members/1"

# Search by branch
curl "http://localhost:50001/api/members/branch/jp"
```

#### Vector Search Operations
```bash
# Semantic search
curl -X POST "http://localhost:50002/api/vector/search" \
  -H "Content-Type: application/json" \
  -d '{"query": "singing talent", "limit": 5}'

# Check vector index status
curl "http://localhost:50002/api/vector/status"
```

#### AI Analysis Operations
```bash
# Perform analysis
curl -X POST "http://localhost:50004/api/analyze/quick" \
  -H "Content-Type: application/json" \
  -d '{"query": "recommend members", "context": {}}'
```

## Monitoring and Operations

### Service Discovery
- Eureka Dashboard: `http://localhost:50008`
- All services automatically register with Eureka
- Health checks performed every 30 seconds

### Logging Configuration
```bash
# Service logs location
tail -f services/msa/member-service/logs/member-service.log
tail -f services/msa/vector-service/logs/vector-service.log
tail -f services/msa/llm-analyzer-service/logs/llm-analyzer.log

# Application log levels
LOG_LEVEL=DEBUG ./gradlew :msa:member-service:bootRun
```

### Performance Monitoring
- JVM metrics exposed via Micrometer
- Custom business metrics collection
- Response time tracking for all endpoints

## Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check port usage
lsof -i :50001
lsof -i :50002

# Kill conflicting processes
kill -9 $(lsof -t -i:50001)
```

#### Database Connection Issues
```bash
# Test PostgreSQL connection
PGPASSWORD=holo_password psql -h localhost -p 5433 -U holo_user -d holo_oshi_db

# Check database extensions
\dx
```

#### Service Registration Issues
```bash
# Check Eureka registration
curl "http://localhost:50008/eureka/apps"

# Verify service configuration
grep "eureka" services/msa/*/src/main/resources/application.yml
```

#### Build Failures
```bash
# Clean Gradle cache
rm -rf ~/.gradle/caches/

# Refresh dependencies
./gradlew clean build --refresh-dependencies --no-daemon
```

## Development Guidelines

### Code Standards
- Kotlin coding conventions strictly enforced
- Reactive programming patterns mandatory
- Comprehensive error handling required
- Unit test coverage minimum 80%

### Service Communication
- All inter-service communication via REST APIs
- Event-driven updates through RabbitMQ
- Circuit breaker pattern for fault tolerance
- Request correlation IDs for tracing

### Database Operations
- R2DBC for reactive database access
- Connection pooling configured for optimal performance
- Database migrations managed through Flyway
- Backup and recovery procedures documented

## Security Considerations

### API Security
- Request rate limiting implemented
- Input validation on all endpoints
- SQL injection prevention via parameterized queries
- XSS protection through output encoding

### Infrastructure Security
- Database access restricted to application services
- Redis access password-protected
- Environment variables for sensitive configuration
- Container security scanning enabled

This documentation provides the technical foundation for operating, maintaining, and extending the Holo-Oshi-Finder MSA system.