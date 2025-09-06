# Holo-Oshi-Finder Local Development Environment

## Project Description

Holo-Oshi-Finder is a comprehensive microservices-based application for discovering and analyzing Hololive VTuber members through advanced search capabilities. The system combines semantic vector search, AI-powered analysis, and traditional database operations to provide intelligent member recommendations.

## System Architecture

### Technology Stack
- **Backend**: Kotlin microservices with Spring Boot 3.5.5
- **Frontend**: React 18 with TypeScript and Ant Design
- **Database**: PostgreSQL 16 with TimescaleDB and pgvector extension
- **Caching**: Redis 7 for distributed caching and session management
- **Messaging**: RabbitMQ for event-driven communication
- **AI Integration**: Google Gemini API for natural language processing
- **Deployment**: Docker Compose for local development

### Architecture Pattern
The system implements a microservices architecture with the following key patterns:
- Service Discovery via Netflix Eureka
- API Gateway for request routing and load balancing
- Event-driven communication through RabbitMQ
- Circuit breaker pattern for fault tolerance
- Reactive programming with Kotlin Coroutines

## Core Services

| Service | Purpose | Technology |
|---------|---------|------------|
| **Member Service** | Member data management and CRUD operations | Spring WebFlux, R2DBC |
| **Vector Service** | Semantic search using vector embeddings | pgvector, Gemini Embeddings |
| **LLM Analyzer Service** | AI-powered analysis and recommendations | Google Gemini API |
| **Search Service** | Orchestrates complex search operations | Service composition |
| **API Gateway** | Request routing and load balancing | Spring Cloud Gateway |
| **Eureka Server** | Service discovery and registration | Spring Cloud Netflix |

## Data Specifications

### Member Database
- **Total Records**: 74 Hololive members
- **Active Members**: 67 currently active
- **Graduated Members**: 7 retired members
- **Branches**: JP (Japan), EN (English), ID (Indonesia)
- **Generations**: 0-6 across different branches

### Vector Search Capabilities
- **Embedding Model**: Google Gemini text-embedding-004
- **Vector Dimensions**: 1536
- **Index Type**: HNSW (Hierarchical Navigable Small World)
- **Search Performance**: Sub-second response times
- **Similarity Metrics**: Cosine similarity

## Quick Start Guide

### Prerequisites
- Docker and Docker Compose
- Java 23 (for local development)
- Node.js 18+ (for frontend development)

### Environment Setup
1. Clone the repository
2. Configure environment variables in `.env`
3. Start infrastructure services
4. Launch application services

### Basic Deployment
```bash
# Start infrastructure
docker compose up -d postgres redis rabbitmq

# Load environment configuration
source load-env.sh

# Start microservices
cd services
./gradlew :msa:eureka-server:bootRun --no-daemon &
./gradlew :msa:member-service:bootRun --no-daemon &
./gradlew :msa:vector-service:bootRun --no-daemon &
./gradlew :msa:llm-analyzer-service:bootRun --no-daemon &

# Start frontend (optional)
cd frontend
npm install
npm run dev
```

### Service Health Check
```bash
# Check all services are running
curl http://localhost:50008/actuator/health  # Eureka Server
curl http://localhost:50001/health           # Member Service
curl http://localhost:50002/health           # Vector Service
curl http://localhost:50004/health           # LLM Analyzer
```

## API Usage Examples

### Member Search
```bash
# Get all active members
curl "http://localhost:50001/api/members?activeOnly=true"

# Search specific member by ID
curl "http://localhost:50001/api/members/1"

# Filter by branch (JP/EN/ID)
curl "http://localhost:50001/api/members/branch/jp"
```

### Semantic Vector Search
```bash
# Search by natural language query
curl -X POST "http://localhost:50002/api/vector/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "member who is good at singing",
    "limit": 5,
    "activeOnly": true
  }'
```

### AI-Powered Analysis
```bash
# Get AI recommendations
curl -X POST "http://localhost:50004/api/analyze/quick" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "recommend members for music collaboration",
    "preferences": {}
  }'
```

## Development Environment

### Local Development Ports
| Service | Port | Access URL |
|---------|------|------------|
| Frontend | 5173 | http://localhost:5173 |
| API Gateway | 50099 | http://localhost:50099 |
| Member Service | 50001 | http://localhost:50001 |
| Vector Service | 50002 | http://localhost:50002 |
| LLM Analyzer | 50004 | http://localhost:50004 |
| Search Service | 50006 | http://localhost:50006 |
| Eureka Server | 50008 | http://localhost:50008 |
| PostgreSQL | 5433 | localhost:5433 |
| Redis | 6380 | localhost:6380 |
| RabbitMQ Management | 15672 | http://localhost:15672 |

### Configuration Management
Environment-specific configurations are managed through:
- `.env` files for local development
- `application-{profile}.yml` for Spring Boot services
- Docker Compose override files for containerized deployment

### Database Schema
The PostgreSQL database includes:
- Core member data with detailed profiles
- Vector embeddings for semantic search
- Event sourcing tables for audit trails
- Performance optimization indexes

## Monitoring and Operations

### Health Monitoring
- Comprehensive health checks for all services
- Service discovery dashboard via Eureka
- Application metrics through Micrometer
- Custom business metrics collection

### Logging Strategy
- Structured logging with correlation IDs
- Centralized log aggregation capability
- Different log levels per environment
- Request/response tracing for debugging

### Performance Characteristics
- **Average Response Time**: 50-200ms for standard queries
- **Vector Search Performance**: 300-500ms including AI embedding
- **Concurrent Users**: Tested up to 1000 simultaneous requests
- **Data Consistency**: Eventual consistency through event sourcing

## Security Implementation

### API Security
- Input validation and sanitization
- SQL injection prevention through parameterized queries
- Rate limiting on public endpoints
- CORS configuration for cross-origin requests

### Data Protection
- Environment variable management for secrets
- Database connection encryption
- Redis authentication enabled
- Container security best practices

## Deployment Architecture

### Development Environment
- Docker Compose orchestration
- Hot reload for rapid development
- Local database with sample data
- Debug logging enabled

### Production Considerations
- Container orchestration recommendations
- Database scaling strategies  
- Caching optimization approaches
- Monitoring and alerting setup

## Troubleshooting Guide

### Common Issues
- **Port Conflicts**: Check for conflicting processes on required ports
- **Database Connection**: Verify PostgreSQL service status and credentials
- **Service Registration**: Ensure Eureka server is accessible
- **Memory Issues**: Monitor JVM heap usage during development

### Debug Procedures
- Check service logs in respective log directories
- Verify environment variable configuration
- Test database connectivity independently
- Validate API endpoints using provided curl examples

## Contributing Guidelines

### Code Standards
- Kotlin coding conventions compliance
- Comprehensive unit test coverage
- Documentation requirements for public APIs
- Code review process for all changes

### Development Workflow
- Feature branch development model
- Automated testing pipeline
- Continuous integration practices
- Deployment validation procedures

This local development environment provides a complete foundation for developing, testing, and extending the Holo-Oshi-Finder microservices ecosystem.