/**
 * MongoDB 로깅 데이터베이스 초기화 스크립트
 * Time Series Collection 및 인덱스 생성
 */

// 로깅 전용 데이터베이스 생성
db = db.getSiblingDB('holo_monitoring');

// 1. API 요청 로그 (Time Series Collection)
db.createCollection('api_requests', {
  timeseries: {
    timeField: 'timestamp',
    metaField: 'metadata',
    granularity: 'seconds'
  },
  expireAfterSeconds: 2592000  // 30일 후 자동 삭제
});

// API 요청 로그 인덱스
db.api_requests.createIndex({ 'metadata.path': 1, 'timestamp': -1 });
db.api_requests.createIndex({ 'metadata.ip': 1, 'timestamp': -1 });
db.api_requests.createIndex({ 'metadata.statusCode': 1, 'timestamp': -1 });
db.api_requests.createIndex({ 'metadata.service': 1, 'timestamp': -1 });

// 2. 보안 이벤트 로그
db.createCollection('security_events', {
  timeseries: {
    timeField: 'timestamp',
    metaField: 'threat',
    granularity: 'seconds'
  },
  expireAfterSeconds: 7776000  // 90일 보관
});

// 보안 이벤트 인덱스
db.security_events.createIndex({ 'threat.type': 1, 'timestamp': -1 });
db.security_events.createIndex({ 'threat.ip': 1, 'timestamp': -1 });
db.security_events.createIndex({ 'threat.severity': 1, 'timestamp': -1 });

// 3. 차단된 IP 목록 (영구 보관)
db.createCollection('blocked_ips');
db.blocked_ips.createIndex({ 'ip': 1 }, { unique: true });
db.blocked_ips.createIndex({ 'blockedAt': -1 });
db.blocked_ips.createIndex({ 'type': 1 });

// 4. 시간별 집계 통계 (7일 보관)
db.createCollection('hourly_stats', {
  timeseries: {
    timeField: 'hour',
    metaField: 'stats',
    granularity: 'hours'
  },
  expireAfterSeconds: 604800  // 7일
});

// 5. 일별 집계 통계 (1년 보관)
db.createCollection('daily_stats', {
  timeseries: {
    timeField: 'date',
    metaField: 'stats',
    granularity: 'hours'
  },
  expireAfterSeconds: 31536000  // 365일
});

// 6. 에러 로그 (30일 보관)
db.createCollection('error_logs', {
  timeseries: {
    timeField: 'timestamp',
    metaField: 'error',
    granularity: 'seconds'
  },
  expireAfterSeconds: 2592000  // 30일
});

db.error_logs.createIndex({ 'error.service': 1, 'timestamp': -1 });
db.error_logs.createIndex({ 'error.type': 1, 'timestamp': -1 });

// 7. 사용자 세션 추적 (선택적)
db.createCollection('user_sessions');
db.user_sessions.createIndex({ 'sessionId': 1 }, { unique: true });
db.user_sessions.createIndex({ 'userId': 1 });
db.user_sessions.createIndex({ 'createdAt': 1 }, { expireAfterSeconds: 86400 }); // 24시간 후 삭제

// 8. Rate Limiting 추적
db.createCollection('rate_limits');
db.rate_limits.createIndex({ 'ip': 1, 'window': 1 });
db.rate_limits.createIndex({ 'createdAt': 1 }, { expireAfterSeconds: 3600 }); // 1시간 후 삭제

print('MongoDB 로깅 데이터베이스 초기화 완료');
print('생성된 컬렉션:');
print('- api_requests (Time Series, 30일 TTL)');
print('- security_events (Time Series, 90일 TTL)');
print('- blocked_ips (영구 보관)');
print('- hourly_stats (Time Series, 7일 TTL)');
print('- daily_stats (Time Series, 1년 TTL)');
print('- error_logs (Time Series, 30일 TTL)');
print('- user_sessions (24시간 TTL)');
print('- rate_limits (1시간 TTL)');