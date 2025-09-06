-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Members table
CREATE TABLE IF NOT EXISTS members (
    id SERIAL PRIMARY KEY,
    name_en VARCHAR(100) NOT NULL,
    name_ja VARCHAR(100),
    generation VARCHAR(50),
    branch VARCHAR(50),
    unit VARCHAR(100),
    debut_date DATE,
    birthday DATE,
    height INTEGER,
    fanbase_name VARCHAR(100),
    emoji VARCHAR(10),
    youtube_channel VARCHAR(255) UNIQUE,
    twitter_handle VARCHAR(100),
    tags TEXT[],
    personality_traits JSONB,
    -- 활동 상태 관리
    activity_status VARCHAR(20) DEFAULT 'active' CHECK (activity_status IN ('active', 'graduated', 'terminated', 'retired', 'affiliate')),
    graduation_date DATE,
    graduation_type VARCHAR(20) CHECK (graduation_type IN ('graduated', 'terminated', 'retired', 'affiliate')),
    graduation_reason VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- User preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    id SERIAL PRIMARY KEY,
    session_id UUID NOT NULL,
    member_id INTEGER REFERENCES members(id),
    score DECIMAL(5,2),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- User activity logs (time-series data)
CREATE TABLE IF NOT EXISTS user_activity_logs (
    time TIMESTAMPTZ NOT NULL,
    session_id UUID NOT NULL,
    action_type VARCHAR(50),
    member_id INTEGER REFERENCES members(id),
    question_id INTEGER,
    answer JSONB,
    metadata JSONB
);

-- Convert to hypertable for time-series optimization
SELECT create_hypertable('user_activity_logs', 'time', if_not_exists => TRUE);

-- Create indexes
CREATE INDEX idx_members_generation ON members(generation);
CREATE INDEX idx_members_branch ON members(branch);
CREATE INDEX idx_members_tags ON members USING GIN(tags);
CREATE INDEX idx_user_preferences_session ON user_preferences(session_id);
CREATE INDEX idx_user_activity_session ON user_activity_logs(session_id, time DESC);

-- Continuous aggregate for member popularity
CREATE MATERIALIZED VIEW member_popularity_hourly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS bucket,
    member_id,
    COUNT(*) as view_count,
    COUNT(DISTINCT session_id) as unique_viewers
FROM user_activity_logs
WHERE action_type = 'view_member'
GROUP BY bucket, member_id
WITH NO DATA;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('member_popularity_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');