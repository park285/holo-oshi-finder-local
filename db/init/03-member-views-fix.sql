-- 7. 멤버별 전체 프로필 요약 뷰 (수정본)
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
    (SELECT array_agg(skill_name ORDER BY skill_name) 
     FROM (SELECT skill_name FROM member_special_skills WHERE member_id = m.id ORDER BY skill_name LIMIT 3) s) as top_skills,
    
    -- 유명한 순간 수
    (SELECT COUNT(*) FROM member_famous_moments WHERE member_id = m.id) as famous_moments_count,
    
    -- 대표 캐치프레이즈
    (SELECT phrase_text FROM member_catchphrases 
     WHERE member_id = m.id AND popularity_rank = 1 LIMIT 1) as signature_catchphrase,
    
    -- 주요 콘텐츠 타입
    (SELECT array_agg(DISTINCT content_type) 
     FROM member_content_styles WHERE member_id = m.id) as content_types,
    
    -- 주요 성격 특성 (상위 5개)
    (SELECT array_agg(trait_value ORDER BY trait_score DESC NULLS LAST) 
     FROM (SELECT trait_value, trait_score FROM member_personality_details WHERE member_id = m.id ORDER BY trait_score DESC NULLS LAST LIMIT 5) p) as main_personality_traits,
    
    -- 콜라보 파트너 수
    (SELECT COUNT(DISTINCT COALESCE(partner_member_id::text, partner_external_name)) 
     FROM member_collaborations WHERE member_id = m.id) as collab_partner_count
     
FROM members m
ORDER BY m.generation, m.name_en;

COMMENT ON VIEW v_member_complete_profile IS '멤버별 전체 프로필 요약 - 모든 비정형 데이터 통합';