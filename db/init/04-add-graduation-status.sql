-- 졸업 상태 관리 컬럼 추가 (기존 DB용)
-- 실행일: 2025-08-13

-- 1. 활동 상태 컬럼 추가
ALTER TABLE members 
ADD COLUMN IF NOT EXISTS activity_status VARCHAR(20) DEFAULT 'active';

ALTER TABLE members 
ADD CONSTRAINT IF NOT EXISTS check_activity_status 
CHECK (activity_status IN ('active', 'graduated', 'terminated', 'retired', 'affiliate'));

-- 2. 졸업 관련 컬럼들 추가
ALTER TABLE members 
ADD COLUMN IF NOT EXISTS graduation_date DATE;

ALTER TABLE members 
ADD COLUMN IF NOT EXISTS graduation_type VARCHAR(20);

ALTER TABLE members 
ADD CONSTRAINT IF NOT EXISTS check_graduation_type 
CHECK (graduation_type IN ('graduated', 'terminated', 'retired', 'affiliate'));

ALTER TABLE members 
ADD COLUMN IF NOT EXISTS graduation_reason VARCHAR(50);

ALTER TABLE members 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 3. 인덱스 추가 (검색 성능 향상)
CREATE INDEX IF NOT EXISTS idx_members_activity_status ON members(activity_status);
CREATE INDEX IF NOT EXISTS idx_members_is_active ON members(is_active);
CREATE INDEX IF NOT EXISTS idx_members_graduation_date ON members(graduation_date);

-- 4. 졸업 멤버 데이터 업데이트 (웹 조사 결과 기반)
UPDATE members SET 
    activity_status = 'retired',
    graduation_date = '2020-08-31',
    graduation_type = 'retired',
    graduation_reason = 'premature_ending_of_contract',
    is_active = FALSE
WHERE name_en = 'Mano Aloe';

UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2021-07-01',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Kiryu Coco';

UPDATE members SET 
    activity_status = 'terminated',
    graduation_date = '2022-02-24',
    graduation_type = 'terminated',
    graduation_reason = 'contract_violation',
    is_active = FALSE
WHERE name_en = 'Uruha Rushia';

UPDATE members SET 
    activity_status = 'terminated',
    graduation_date = '2024-01-16',
    graduation_type = 'terminated',
    graduation_reason = 'contract_violation',
    is_active = FALSE
WHERE name_en = 'Yozora Mel';

UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2024-08-28',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Minato Aqua';

UPDATE members SET 
    activity_status = 'affiliate',
    graduation_date = '2025-01-26',
    graduation_type = 'affiliate',
    graduation_reason = 'activity_conclusion',
    is_active = FALSE
WHERE name_en = 'Sakamata Chloe';

UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2025-04-26',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Murasaki Shion';

-- EN 졸업 멤버들 (현재 DB에 있다면)
UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2022-07-31',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Tsukumo Sana';

UPDATE members SET 
    activity_status = 'affiliate',
    graduation_date = '2024-09-30',
    graduation_type = 'affiliate',
    graduation_reason = 'activity_conclusion',
    is_active = FALSE
WHERE name_en = 'Watson Amelia';

UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2025-01-04',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Ceres Fauna';

UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2025-04-28',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Nanashi Mumei';

UPDATE members SET 
    activity_status = 'graduated',
    graduation_date = '2025-05-01',
    graduation_type = 'graduated',
    graduation_reason = 'personal_decision',
    is_active = FALSE
WHERE name_en = 'Gawr Gura';

-- 5. 현역 멤버들의 is_active 값 명시적으로 설정
UPDATE members SET 
    is_active = TRUE,
    activity_status = 'active'
WHERE activity_status IS NULL OR activity_status = 'active';

-- 6. 졸업 상태 확인 뷰 생성
CREATE OR REPLACE VIEW v_graduation_status AS
SELECT 
    name_en,
    name_ja,
    generation,
    branch,
    activity_status,
    graduation_date,
    graduation_type,
    graduation_reason,
    is_active,
    CASE 
        WHEN is_active THEN '현역'
        WHEN activity_status = 'graduated' THEN '졸업'
        WHEN activity_status = 'terminated' THEN '계약 종료'
        WHEN activity_status = 'affiliate' THEN '소속 유지'
        WHEN activity_status = 'retired' THEN '은퇴'
        ELSE '알 수 없음'
    END as status_korean
FROM members
ORDER BY 
    is_active DESC,
    graduation_date DESC NULLS LAST,
    generation,
    name_en;

-- 7. 통계 확인 쿼리
-- SELECT 
--     activity_status,
--     COUNT(*) as count,
--     STRING_AGG(name_en, ', ' ORDER BY name_en) as members
-- FROM members 
-- GROUP BY activity_status 
-- ORDER BY count DESC;