-- 사용자 접근 모니터링 테이블
-- IP, User Agent, API 호출 등 추적

-- 사용자 세션 테이블
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ip_address INET NOT NULL,
    user_agent TEXT,
    country_code CHAR(2),
    city VARCHAR(100),
    first_visit TIMESTAMPTZ DEFAULT NOW(),
    last_visit TIMESTAMPTZ DEFAULT NOW(),
    total_visits INTEGER DEFAULT 1,
    is_bot BOOLEAN DEFAULT FALSE,
    fingerprint VARCHAR(64), -- 브라우저 핑거프린트
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- API 호출 로그 테이블
CREATE TABLE IF NOT EXISTS api_calls (
    call_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES user_sessions(session_id),
    ip_address INET NOT NULL,
    method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    user_agent TEXT,
    status_code INTEGER,
    response_time INTEGER, -- 밀리초
    request_size INTEGER,
    response_size INTEGER,
    query_params JSONB,
    request_body_hash VARCHAR(64), -- 요청 본문 해시 (개인정보 보호)
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 봇/크롤러 감지 테이블
CREATE TABLE IF NOT EXISTS bot_detection (
    detection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ip_address INET NOT NULL,
    user_agent TEXT,
    bot_type VARCHAR(50), -- googlebot, bingbot, etc.
    confidence_score DECIMAL(3,2), -- 0.00-1.00
    detection_reason TEXT[],
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 의심스러운 활동 추적
CREATE TABLE IF NOT EXISTS suspicious_activities (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES user_sessions(session_id),
    ip_address INET NOT NULL,
    activity_type VARCHAR(50), -- rate_limit_exceeded, unusual_pattern, etc.
    severity VARCHAR(20), -- low, medium, high, critical
    details JSONB,
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_user_sessions_ip ON user_sessions(ip_address);
CREATE INDEX IF NOT EXISTS idx_user_sessions_created ON user_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_api_calls_ip ON api_calls(ip_address);
CREATE INDEX IF NOT EXISTS idx_api_calls_endpoint ON api_calls(endpoint);
CREATE INDEX IF NOT EXISTS idx_api_calls_created ON api_calls(created_at);
CREATE INDEX IF NOT EXISTS idx_api_calls_session ON api_calls(session_id);
CREATE INDEX IF NOT EXISTS idx_bot_detection_ip ON bot_detection(ip_address);
CREATE INDEX IF NOT EXISTS idx_suspicious_activities_ip ON suspicious_activities(ip_address);

-- 뷰 생성 (자주 사용되는 쿼리)
CREATE OR REPLACE VIEW daily_stats AS
SELECT 
    DATE(created_at) as date,
    COUNT(DISTINCT ip_address) as unique_ips,
    COUNT(*) as total_calls,
    AVG(response_time) as avg_response_time,
    COUNT(CASE WHEN status_code >= 400 THEN 1 END) as error_count
FROM api_calls 
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;

CREATE OR REPLACE VIEW top_ips AS
SELECT 
    ip_address,
    COUNT(*) as call_count,
    AVG(response_time) as avg_response_time,
    MIN(created_at) as first_seen,
    MAX(created_at) as last_seen,
    COUNT(DISTINCT endpoint) as unique_endpoints
FROM api_calls 
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY ip_address
ORDER BY call_count DESC
LIMIT 100;

CREATE OR REPLACE VIEW endpoint_stats AS
SELECT 
    endpoint,
    COUNT(*) as call_count,
    AVG(response_time) as avg_response_time,
    COUNT(CASE WHEN status_code >= 400 THEN 1 END) as error_count,
    COUNT(DISTINCT ip_address) as unique_users
FROM api_calls 
WHERE created_at >= NOW() - INTERVAL '24 hours'
GROUP BY endpoint
ORDER BY call_count DESC;