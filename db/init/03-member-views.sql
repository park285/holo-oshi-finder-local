-- 멤버 정보와 비정형 데이터를 연결하는 뷰들

-- 1. 멤버별 특별 스킬 통합 뷰
CREATE OR REPLACE VIEW v_member_skills AS
SELECT 
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    ms.skill_name,
    ms.skill_category,
    ms.proficiency_level,
    ms.description,
    ms.evidence_urls
FROM members m
INNER JOIN member_special_skills ms ON m.id = ms.member_id
ORDER BY m.generation, m.name_en, ms.skill_name;

-- 2. 멤버별 유명한 순간들 통합 뷰
CREATE OR REPLACE VIEW v_member_famous_moments AS
SELECT 
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    mf.moment_title,
    mf.moment_date,
    mf.description,
    mf.video_url,
    mf.timestamp,
    mf.view_count,
    mf.impact_score,
    mf.tags
FROM members m
INNER JOIN member_famous_moments mf ON m.id = mf.member_id
ORDER BY m.generation, m.name_en, mf.moment_date DESC NULLS LAST;

-- 3. 멤버별 캐치프레이즈 통합 뷰
CREATE OR REPLACE VIEW v_member_catchphrases AS
SELECT 
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    mc.phrase_text,
    mc.phrase_romanized,
    mc.phrase_translation,
    mc.context,
    mc.popularity_rank,
    mc.usage_frequency
FROM members m
INNER JOIN member_catchphrases mc ON m.id = mc.member_id
ORDER BY m.generation, m.name_en, mc.popularity_rank;

-- 4. 멤버별 콘텐츠 스타일 통합 뷰
CREATE OR REPLACE VIEW v_member_content_styles AS
SELECT 
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    mcs.content_type,
    mcs.style_descriptor,
    mcs.frequency,
    mcs.popular_series,
    mcs.viewer_demographics
FROM members m
INNER JOIN member_content_styles mcs ON m.id = mcs.member_id
ORDER BY m.generation, m.name_en, mcs.content_type;

-- 5. 멤버별 성격 특성 통합 뷰
CREATE OR REPLACE VIEW v_member_personality AS
SELECT 
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    mp.trait_category,
    mp.trait_value,
    mp.trait_score,
    mp.evidence_examples,
    mp.fan_perception
FROM members m
INNER JOIN member_personality_details mp ON m.id = mp.member_id
ORDER BY m.generation, m.name_en, mp.trait_category, mp.trait_value;

-- 6. 멤버별 콜라보레이션 통합 뷰
CREATE OR REPLACE VIEW v_member_collaborations AS
SELECT 
    m1.name_en as member_name,
    m1.generation as member_generation,
    COALESCE(m2.name_en, mc.partner_external_name) as partner_name,
    mc.collaboration_type,
    mc.frequency,
    mc.chemistry_score,
    mc.popular_content
FROM members m1
INNER JOIN member_collaborations mc ON m1.id = mc.member_id
LEFT JOIN members m2 ON mc.partner_member_id = m2.id
ORDER BY m1.generation, m1.name_en, partner_name;

-- 7. 멤버별 전체 프로필 요약 뷰
CREATE OR REPLACE VIEW v_member_complete_profile AS
SELECT 
    m.id,
    m.name_en,
    m.name_ja,
    m.generation,
    m.branch,
    m.unit,
    m.debut_date,
    m.birthday,
    m.fanbase_name,
    m.emoji,
    m.youtube_channel,
    m.twitter_handle,
    
    -- 스킬 수
    (SELECT COUNT(*) FROM member_special_skills WHERE member_id = m.id) as skill_count,
    
    -- 대표 스킬 (상위 3개)
    (SELECT array_agg(skill_name ORDER BY skill_name LIMIT 3) 
     FROM member_special_skills WHERE member_id = m.id) as top_skills,
    
    -- 유명한 순간 수
    (SELECT COUNT(*) FROM member_famous_moments WHERE member_id = m.id) as famous_moments_count,
    
    -- 대표 캐치프레이즈
    (SELECT phrase_text FROM member_catchphrases 
     WHERE member_id = m.id AND popularity_rank = 1 LIMIT 1) as signature_catchphrase,
    
    -- 주요 콘텐츠 타입
    (SELECT array_agg(DISTINCT content_type) 
     FROM member_content_styles WHERE member_id = m.id) as content_types,
    
    -- 주요 성격 특성 (상위 5개)
    (SELECT array_agg(trait_value ORDER BY trait_score DESC NULLS LAST LIMIT 5) 
     FROM member_personality_details WHERE member_id = m.id) as main_personality_traits,
    
    -- 콜라보 파트너 수
    (SELECT COUNT(DISTINCT COALESCE(partner_member_id::text, partner_external_name)) 
     FROM member_collaborations WHERE member_id = m.id) as collab_partner_count
     
FROM members m
ORDER BY m.generation, m.name_en;

-- 8. 세대별 통계 뷰
CREATE OR REPLACE VIEW v_generation_stats AS
SELECT 
    m.generation,
    m.branch,
    COUNT(DISTINCT m.id) as member_count,
    COUNT(DISTINCT ms.id) as total_skills,
    COUNT(DISTINCT mf.id) as total_famous_moments,
    COUNT(DISTINCT mc.id) as total_catchphrases,
    AVG((SELECT COUNT(*) FROM member_special_skills WHERE member_id = m.id)) as avg_skills_per_member,
    AVG((SELECT COUNT(*) FROM member_famous_moments WHERE member_id = m.id)) as avg_moments_per_member
FROM members m
LEFT JOIN member_special_skills ms ON m.id = ms.member_id
LEFT JOIN member_famous_moments mf ON m.id = mf.member_id
LEFT JOIN member_catchphrases mc ON m.id = mc.member_id
GROUP BY m.generation, m.branch
ORDER BY m.generation;

-- 9. 인기 캐치프레이즈 랭킹 뷰
CREATE OR REPLACE VIEW v_popular_catchphrases AS
SELECT 
    m.name_en,
    m.generation,
    mc.phrase_text,
    mc.phrase_translation,
    mc.popularity_rank,
    mc.usage_frequency
FROM member_catchphrases mc
INNER JOIN members m ON mc.member_id = m.id
WHERE mc.popularity_rank <= 3
ORDER BY mc.popularity_rank, m.generation, m.name_en;

-- 10. 콘텐츠 타입별 멤버 분포 뷰
CREATE OR REPLACE VIEW v_content_type_distribution AS
SELECT 
    mcs.content_type,
    COUNT(DISTINCT m.id) as member_count,
    array_agg(DISTINCT m.name_en ORDER BY m.name_en) as members,
    array_agg(DISTINCT m.generation ORDER BY m.generation) as generations
FROM member_content_styles mcs
INNER JOIN members m ON mcs.member_id = m.id
GROUP BY mcs.content_type
ORDER BY member_count DESC, mcs.content_type;

-- 인덱스 추가 (뷰 성능 향상)
CREATE INDEX IF NOT EXISTS idx_members_name_en ON members(name_en);
CREATE INDEX IF NOT EXISTS idx_members_generation ON members(generation);
CREATE INDEX IF NOT EXISTS idx_members_branch ON members(branch);

-- 사용 예시를 위한 코멘트
COMMENT ON VIEW v_member_complete_profile IS '멤버별 전체 프로필 요약 - 모든 비정형 데이터 통합';
COMMENT ON VIEW v_member_skills IS '멤버별 특별 스킬 목록';
COMMENT ON VIEW v_member_famous_moments IS '멤버별 유명한 순간들';
COMMENT ON VIEW v_member_catchphrases IS '멤버별 캐치프레이즈';
COMMENT ON VIEW v_member_personality IS '멤버별 성격 특성';
COMMENT ON VIEW v_generation_stats IS '세대별 통계 요약';
COMMENT ON VIEW v_popular_catchphrases IS '인기 캐치프레이즈 TOP 3';
COMMENT ON VIEW v_content_type_distribution IS '콘텐츠 타입별 멤버 분포';