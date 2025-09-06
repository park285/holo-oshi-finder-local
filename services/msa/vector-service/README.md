# Vector Service

## Service Overview

The Vector Service implements semantic search capabilities using PostgreSQL's pgvector extension and Google Gemini embeddings. It provides vector similarity search for Hololive member data, enabling natural language queries and content-based recommendations.

## Technical Architecture

### Technology Stack
- **Framework**: Spring Boot 3.5.5 with Spring WebFlux
- **Language**: Kotlin 2.2.10 with Coroutines
- **Vector Database**: PostgreSQL 16 with pgvector extension
- **Embedding Provider**: Google Gemini text-embedding-004
- **Caching**: Redis for embedding and result caching
- **Messaging**: RabbitMQ for event-driven updates
- **Service Discovery**: Netflix Eureka client

### Core Capabilities
- High-dimensional vector similarity search (1536 dimensions)
- Real-time embedding generation via Google Gemini API
- HNSW index optimization for fast retrieval
- Distributed caching for performance optimization
- Event-driven re-indexing on data changes

## Vector Search Architecture

### Embedding Model Specifications
- **Provider**: Google Gemini text-embedding-004
- **Dimensions**: 1536
- **Input Token Limit**: 2048 tokens per request
- **Language Support**: Multi-language (optimized for English/Japanese)
- **Similarity Metric**: Cosine similarity

### Vector Storage
```sql
-- pgvector table structure
CREATE TABLE member_embeddings (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES members(id),
    embedding vector(1536),
    text_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- HNSW index for efficient similarity search
CREATE INDEX ON member_embeddings 
USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 64);
```

### Current Vector Dataset
- **Total Embeddings**: 74 member embeddings
- **Vector Dimensions**: 1536 per embedding
- **Index Type**: HNSW (Hierarchical Navigable Small World)
- **Storage Size**: Approximately 400KB per embedding
- **Search Performance**: Sub-500ms for typical queries

## API Endpoints

### Health Check
```
GET /health
```
Returns service health including vector database status and embedding service connectivity.

### Vector Search Operations
```
POST /api/vector/search
Content-Type: application/json

Request Body:
{
  "query": "string",           // Natural language search query
  "limit": 10,                 // Max results (default: 10, max: 50)
  "activeOnly": true,          // Filter active members only
  "minSimilarity": 0.7         // Minimum cosine similarity threshold
}

Response:
{
  "success": true,
  "data": [
    {
      "memberId": 1,
      "name": "Member Name",
      "similarity": 0.85,
      "matchedContent": "...",
      "memberDetails": {...}
    }
  ],
  "metadata": {
    "queryTime": "450ms",
    "embeddingTime": "200ms",
    "searchTime": "250ms",
    "totalResults": 15,
    "cached": false
  }
}
```

### Vector Management
```
GET /api/vector/status
Returns vector database statistics and index health.

POST /api/vector/reindex/{memberId}
Path Parameters:
  - memberId: Long (specific member to re-index)

POST /api/vector/reindex/all
Triggers full re-indexing of all member embeddings.
```

## Configuration

### Application Properties
```yaml
server:
  port: 50002

spring:
  application:
    name: vector-service
  datasource:
    url: r2dbc:postgresql://localhost:5433/holo_oshi_db
    username: holo_user
    password: holo_password
  data:
    redis:
      host: localhost
      port: 6380
      timeout: 5000ms

google:
  api:
    key: ${GOOGLE_API_KEY}
    project: ${GOOGLE_PROJECT_ID}
  embedding:
    model: text-embedding-004
    dimensions: 1536
    batchSize: 100

vector:
  search:
    defaultLimit: 10
    maxLimit: 50
    minSimilarity: 0.3
    cacheEnabled: true
    cacheTtl: 3600  # 1 hour
```

### Environment Variables
```bash
GOOGLE_API_KEY=your_gemini_api_key
GOOGLE_PROJECT_ID=your_project_id
DB_HOST=localhost
DB_PORT=5433
DB_USER=holo_user
DB_PASSWORD=holo_password
REDIS_HOST=localhost
REDIS_PORT=6380
```

## Development Setup

### Prerequisites
- PostgreSQL 16 with pgvector extension installed
- Google Cloud project with Gemini API enabled
- Redis server for caching
- RabbitMQ for event messaging

### Database Setup
```bash
# Connect to PostgreSQL
PGPASSWORD=holo_password psql -h localhost -p 5433 -U holo_user -d holo_oshi_db

# Install pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

# Verify installation
SELECT * FROM pg_extension WHERE extname = 'vector';
```

### Build and Run
```bash
# Build the service
./gradlew :msa:vector-service:build

# Run locally
./gradlew :msa:vector-service:bootRun

# Run with debug logging
LOG_LEVEL=DEBUG ./gradlew :msa:vector-service:bootRun
```

## Vector Operations

### Embedding Generation
The service generates embeddings for member data using the following content:
```kotlin
// Content composition for embedding
fun composeMemberContent(member: Member): String {
    return buildString {
        append("Name: ${member.name}")
        append(" Branch: ${member.branch}")
        append(" Generation: ${member.generation}")
        member.description?.let { append(" Description: $it") }
        if (member.traits.isNotEmpty()) {
            append(" Traits: ${member.traits.joinToString(", ")}")
        }
        if (member.colors.isNotEmpty()) {
            append(" Colors: ${member.colors.joinToString(", ")}")
        }
    }
}
```

### Search Algorithm
1. **Query Processing**: Convert natural language query to embedding
2. **Vector Search**: Perform cosine similarity search in pgvector
3. **Result Ranking**: Apply business logic and similarity thresholds
4. **Member Enhancement**: Fetch complete member data for results
5. **Caching**: Store results in Redis for subsequent requests

### Performance Optimization
- **Embedding Caching**: Generated embeddings cached for 24 hours
- **Query Result Caching**: Search results cached for 1 hour
- **Connection Pooling**: R2DBC connection pool optimization
- **Index Tuning**: HNSW parameters optimized for dataset size

## Testing

### Unit Tests
```bash
# Run vector service tests
./gradlew :msa:vector-service:test

# Test specific functionality
./gradlew :msa:vector-service:test --tests "*VectorSearchServiceTest"
```

### Integration Tests
```bash
# Test with real database (requires pgvector setup)
./gradlew :msa:vector-service:integrationTest
```

### API Testing Examples
```bash
# Test vector search
curl -X POST "http://localhost:50002/api/vector/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "cheerful singing member",
    "limit": 5,
    "activeOnly": true
  }'

# Check service status
curl "http://localhost:50002/api/vector/status"

# Test embedding generation
curl -X POST "http://localhost:50002/api/vector/reindex/1" \
  -H "Content-Type: application/json"
```

## Performance Metrics

### Search Performance
- **Average Query Time**: 300-500ms total
  - Embedding Generation: 150-250ms
  - Vector Search: 50-150ms
  - Result Enhancement: 100-150ms
- **Cache Hit Ratio**: 60-80% for repeated queries
- **Concurrent Capacity**: 100+ simultaneous searches

### Vector Index Statistics
```sql
-- Check index usage and performance
SELECT 
    schemaname, 
    tablename, 
    indexname, 
    idx_scan, 
    idx_tup_read, 
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE indexname LIKE '%embedding%';
```

## Event Handling

### Member Update Events
The service subscribes to member change events:
```json
{
  "eventType": "MEMBER_UPDATED",
  "memberId": 1,
  "changedFields": ["description", "traits"],
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Re-indexing Strategy
- **Incremental Updates**: Single member re-indexing on data changes
- **Batch Processing**: Multiple members processed in parallel
- **Error Recovery**: Failed embeddings retried with exponential backoff
- **Version Control**: Embedding versions tracked for consistency

## Monitoring and Operations

### Health Metrics
```json
{
  "status": "UP",
  "components": {
    "vectorDatabase": {
      "status": "UP",
      "details": {
        "totalEmbeddings": 74,
        "indexHealth": "healthy",
        "lastUpdate": "2025-01-15T09:30:00Z"
      }
    },
    "embeddingService": {
      "status": "UP",
      "details": {
        "apiKeyValid": true,
        "quotaRemaining": "95%",
        "avgResponseTime": "200ms"
      }
    }
  }
}
```

### Performance Monitoring
Key metrics tracked:
- Embedding generation latency
- Vector search response times
- Cache hit/miss ratios
- API quota utilization
- Index scan efficiency

### Operational Commands
```bash
# Monitor vector search performance
curl "http://localhost:50002/actuator/metrics/vector.search.duration"

# Check embedding cache statistics
curl "http://localhost:50002/actuator/metrics/cache.gets"

# View detailed service metrics
curl "http://localhost:50002/actuator/prometheus"
```

## Error Handling

### Common Error Scenarios
```json
// Embedding generation failure
{
  "success": false,
  "error": {
    "code": "EMBEDDING_GENERATION_FAILED",
    "message": "Failed to generate embedding for query",
    "details": {
      "reason": "API quota exceeded",
      "retryAfter": "3600s"
    }
  }
}

// Vector search failure
{
  "success": false,
  "error": {
    "code": "VECTOR_SEARCH_FAILED", 
    "message": "Vector similarity search failed",
    "details": {
      "reason": "Database connection timeout"
    }
  }
}
```

### Error Recovery Strategies
- **API Quota Limits**: Implement exponential backoff and retry logic
- **Database Timeouts**: Connection pool management and query optimization
- **Cache Failures**: Graceful degradation to direct database queries
- **Embedding Corruption**: Automatic re-generation with validation

## Security and Best Practices

### API Security
- Google API key rotation procedures
- Request rate limiting per client
- Input validation and sanitization
- Query length and complexity limits

### Data Protection
- Embedding data encryption at rest
- Secure API key storage
- Network encryption for external API calls
- Access logging for audit purposes

## Troubleshooting Guide

### Common Issues
1. **Slow Search Performance**
   ```bash
   # Check index usage
   EXPLAIN ANALYZE SELECT * FROM member_embeddings 
   ORDER BY embedding <=> '[vector]' LIMIT 10;
   
   # Rebuild HNSW index if needed
   REINDEX INDEX member_embeddings_embedding_idx;
   ```

2. **Embedding Generation Failures**
   ```bash
   # Test Gemini API connectivity
   curl -H "Authorization: Bearer $GOOGLE_API_KEY" \
     "https://generativelanguage.googleapis.com/v1/models"
   
   # Check API quota
   gcloud auth application-default print-access-token
   ```

3. **Memory Issues**
   ```bash
   # Monitor JVM heap usage
   curl "http://localhost:50002/actuator/metrics/jvm.memory.used"
   
   # Adjust heap size if needed
   JAVA_OPTS="-Xmx2g" ./gradlew :msa:vector-service:bootRun
   ```

### Debug Procedures
- Enable detailed query logging for pgvector operations
- Monitor Google API response times and error rates
- Track vector search accuracy through manual validation
- Analyze cache performance and hit ratios

This documentation provides comprehensive guidance for operating and maintaining the Vector Service in the Holo-Oshi-Finder ecosystem.