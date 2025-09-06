-- 비정형 데이터 저장을 위한 확장 스키마
-- PostgreSQL JSONB 타입을 활용한 유연한 데이터 저장

-- 1. 특별 스킬 테이블
CREATE TABLE IF NOT EXISTS member_special_skills (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    skill_name VARCHAR(255) NOT NULL,
    skill_category VARCHAR(100), -- gaming, singing, entertainment, etc.
    proficiency_level VARCHAR(50), -- beginner, intermediate, advanced, master
    description TEXT,
    evidence_urls TEXT[], -- 관련 영상/증거 URL들
    metadata JSONB, -- 추가 비정형 데이터
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(member_id, skill_name)
);

-- 2. 유명한 순간들 테이블
CREATE TABLE IF NOT EXISTS member_famous_moments (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    moment_title VARCHAR(255) NOT NULL,
    moment_date DATE,
    description TEXT,
    video_url VARCHAR(500),
    timestamp VARCHAR(20), -- 영상 내 타임스탬프 (예: "1:23:45")
    view_count BIGINT,
    impact_score DECIMAL(3,2), -- 0.00 ~ 9.99 영향력 점수
    tags TEXT[],
    metadata JSONB, -- 추가 컨텍스트, 반응, 밈 정보 등
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. 콜라보 파트너 관계 테이블
CREATE TABLE IF NOT EXISTS member_collaborations (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    partner_member_id INTEGER REFERENCES members(id) ON DELETE CASCADE,
    partner_external_name VARCHAR(255), -- 외부 콜라보레이터 이름
    collaboration_type VARCHAR(100), -- gaming, singing, variety, etc.
    frequency VARCHAR(50), -- rare, occasional, frequent, regular
    popular_content TEXT[], -- 인기 콜라보 콘텐츠 목록
    chemistry_score DECIMAL(3,2), -- 0.00 ~ 9.99 케미 점수
    metadata JSONB, -- 추가 관계 정보
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT check_partner CHECK (
        (partner_member_id IS NOT NULL AND partner_external_name IS NULL) OR
        (partner_member_id IS NULL AND partner_external_name IS NOT NULL)
    )
);

-- 4. 캐치프레이즈/명언 테이블
CREATE TABLE IF NOT EXISTS member_catchphrases (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    phrase_text TEXT NOT NULL,
    phrase_romanized TEXT, -- 로마자 표기
    phrase_translation TEXT, -- 번역
    context TEXT, -- 사용 상황/맥락
    popularity_rank INTEGER, -- 인기도 순위
    first_used_date DATE,
    usage_frequency VARCHAR(50), -- rare, occasional, frequent, signature
    metadata JSONB, -- 관련 밈, 팬아트 정보 등
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. 콘텐츠 스타일/장르 테이블
CREATE TABLE IF NOT EXISTS member_content_styles (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    content_type VARCHAR(100) NOT NULL, -- gaming, singing, chatting, ASMR, etc.
    style_descriptor VARCHAR(255), -- 스타일 설명
    frequency VARCHAR(50), -- never, rare, occasional, frequent, main
    viewer_demographics JSONB, -- 시청자 층 정보
    popular_series TEXT[], -- 인기 시리즈/콘텐츠
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(member_id, content_type)
);

-- 6. 팬 커뮤니티 데이터 테이블
CREATE TABLE IF NOT EXISTS member_community_data (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    platform VARCHAR(100) NOT NULL, -- reddit, twitter, discord, etc.
    community_size INTEGER,
    activity_level VARCHAR(50), -- low, medium, high, very_high
    popular_memes TEXT[], -- 인기 밈들
    fan_names JSONB, -- 언어별 팬 이름들
    community_culture TEXT, -- 커뮤니티 문화 설명
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(member_id, platform)
);

-- 7. 성격 특성 상세 테이블 (기존 personality_traits JSONB 확장)
CREATE TABLE IF NOT EXISTS member_personality_details (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    trait_category VARCHAR(100) NOT NULL, -- introvert/extrovert, energy_level, etc.
    trait_value VARCHAR(255) NOT NULL,
    trait_score DECIMAL(3,2), -- 0.00 ~ 9.99 강도
    evidence_examples TEXT[], -- 해당 특성을 보여주는 예시들
    fan_perception TEXT, -- 팬들의 인식
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. 스트리밍 패턴 분석 테이블
CREATE TABLE IF NOT EXISTS member_streaming_patterns (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    pattern_type VARCHAR(100) NOT NULL, -- schedule, duration, time_slot, etc.
    pattern_value JSONB NOT NULL, -- 패턴 데이터 (시간대, 빈도 등)
    confidence_score DECIMAL(3,2), -- 0.00 ~ 9.99 신뢰도
    analysis_period_start DATE,
    analysis_period_end DATE,
    insights TEXT, -- 패턴 분석 인사이트
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX idx_special_skills_member ON member_special_skills(member_id);
CREATE INDEX idx_special_skills_category ON member_special_skills(skill_category);
CREATE INDEX idx_famous_moments_member ON member_famous_moments(member_id);
CREATE INDEX idx_famous_moments_date ON member_famous_moments(moment_date DESC);
CREATE INDEX idx_famous_moments_tags ON member_famous_moments USING GIN(tags);
CREATE INDEX idx_collaborations_member ON member_collaborations(member_id);
CREATE INDEX idx_collaborations_partner ON member_collaborations(partner_member_id);
CREATE INDEX idx_catchphrases_member ON member_catchphrases(member_id);
CREATE INDEX idx_catchphrases_popularity ON member_catchphrases(popularity_rank);
CREATE INDEX idx_content_styles_member ON member_content_styles(member_id);
CREATE INDEX idx_content_styles_type ON member_content_styles(content_type);
CREATE INDEX idx_community_member ON member_community_data(member_id);
CREATE INDEX idx_personality_member ON member_personality_details(member_id);
CREATE INDEX idx_streaming_patterns_member ON member_streaming_patterns(member_id);

-- JSONB 인덱스
CREATE INDEX idx_special_skills_metadata ON member_special_skills USING GIN(metadata);
CREATE INDEX idx_famous_moments_metadata ON member_famous_moments USING GIN(metadata);
CREATE INDEX idx_collaborations_metadata ON member_collaborations USING GIN(metadata);
CREATE INDEX idx_catchphrases_metadata ON member_catchphrases USING GIN(metadata);
CREATE INDEX idx_content_styles_metadata ON member_content_styles USING GIN(metadata);
CREATE INDEX idx_community_metadata ON member_community_data USING GIN(metadata);
CREATE INDEX idx_personality_metadata ON member_personality_details USING GIN(metadata);
CREATE INDEX idx_streaming_patterns_value ON member_streaming_patterns USING GIN(pattern_value);

-- 뷰 생성: 멤버별 통합 비정형 데이터
CREATE OR REPLACE VIEW member_unstructured_summary AS
SELECT 
    m.id,
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    (SELECT COUNT(*) FROM member_special_skills WHERE member_id = m.id) as skill_count,
    (SELECT COUNT(*) FROM member_famous_moments WHERE member_id = m.id) as famous_moments_count,
    (SELECT COUNT(*) FROM member_collaborations WHERE member_id = m.id) as collab_count,
    (SELECT COUNT(*) FROM member_catchphrases WHERE member_id = m.id) as catchphrase_count,
    (SELECT json_agg(DISTINCT content_type) FROM member_content_styles WHERE member_id = m.id) as content_types,
    m.updated_at
FROM members m;

-- 트리거: updated_at 자동 업데이트
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_member_special_skills_updated_at BEFORE UPDATE ON member_special_skills
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_famous_moments_updated_at BEFORE UPDATE ON member_famous_moments
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_collaborations_updated_at BEFORE UPDATE ON member_collaborations
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_catchphrases_updated_at BEFORE UPDATE ON member_catchphrases
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_content_styles_updated_at BEFORE UPDATE ON member_content_styles
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_community_data_updated_at BEFORE UPDATE ON member_community_data
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_personality_details_updated_at BEFORE UPDATE ON member_personality_details
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_streaming_patterns_updated_at BEFORE UPDATE ON member_streaming_patterns
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();