-- Jina Embeddings v4 벡터 스키마 (1024차원)
-- pgvector 0.8.0 + Jina v4 통합

-- 기존 테이블 백업 (필요시)
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'member_vectors') THEN
        DROP TABLE member_vectors CASCADE;
    END IF;
END $$;

-- Jina v4 벡터 테이블 생성 (1024차원)
CREATE TABLE IF NOT EXISTS member_vectors (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
  
  -- 각 컴포넌트별 1024차원 벡터
  name_embedding vector(1024),
  description_embedding vector(1024),
  personality_embedding vector(1024),
  content_style_embedding vector(1024),
  combined_embedding vector(1024),
  
  -- 메타데이터
  searchable_text TEXT NOT NULL,
  model_version VARCHAR(50) DEFAULT 'jina-embeddings-v4',
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  
  -- 유니크 제약
  UNIQUE(member_id)
);

-- HNSW 인덱스 생성 (pgvector 0.8.0 최적화 설정)
-- Combined embedding (주 검색용)
CREATE INDEX IF NOT EXISTS idx_member_vectors_combined_hnsw 
ON member_vectors USING hnsw (combined_embedding vector_cosine_ops)
WITH (m = 32, ef_construction = 128);

-- Name embedding (이름 검색용)  
CREATE INDEX IF NOT EXISTS idx_member_vectors_name_hnsw
ON member_vectors USING hnsw (name_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Personality embedding (성격 검색용)
CREATE INDEX IF NOT EXISTS idx_member_vectors_personality_hnsw
ON member_vectors USING hnsw (personality_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 추가 인덱스들
CREATE INDEX IF NOT EXISTS idx_member_vectors_member_id ON member_vectors(member_id);
CREATE INDEX IF NOT EXISTS idx_member_vectors_model ON member_vectors(model_version);
CREATE INDEX IF NOT EXISTS idx_member_vectors_updated ON member_vectors(updated_at);

-- 텍스트 검색 인덱스 (하이브리드 검색용)
CREATE INDEX IF NOT EXISTS idx_member_vectors_searchable_text 
ON member_vectors USING gin(to_tsvector('english', searchable_text));

-- 통계 뷰 생성
CREATE OR REPLACE VIEW v_vector_stats AS
SELECT 
  COUNT(*) as total_vectors,
  COUNT(DISTINCT member_id) as unique_members,
  COUNT(DISTINCT model_version) as different_models,
  MIN(created_at) as first_created,
  MAX(updated_at) as last_updated,
  array_agg(DISTINCT model_version) as models_used
FROM member_vectors;

-- 벡터 검색 함수 (1024차원 최적화)
CREATE OR REPLACE FUNCTION search_similar_members(
  query_vector vector(1024),
  similarity_threshold FLOAT DEFAULT 0.7,
  result_limit INTEGER DEFAULT 10
)
RETURNS TABLE (
  member_id INTEGER,
  name_en TEXT,
  name_ja TEXT,
  similarity FLOAT,
  model_version TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    mv.member_id,
    m.name_en,
    m.name_ja,
    (1 - (mv.combined_embedding <=> query_vector))::FLOAT as similarity,
    mv.model_version
  FROM member_vectors mv
  JOIN members m ON mv.member_id = m.id
  WHERE (1 - (mv.combined_embedding <=> query_vector)) >= similarity_threshold
  ORDER BY mv.combined_embedding <=> query_vector
  LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;

-- 하이브리드 검색 함수 (벡터 + 텍스트)
CREATE OR REPLACE FUNCTION hybrid_search(
  query_text TEXT,
  query_vector vector(1024),
  vector_weight FLOAT DEFAULT 0.7,
  text_weight FLOAT DEFAULT 0.3,
  result_limit INTEGER DEFAULT 10
)
RETURNS TABLE (
  member_id INTEGER,
  name_en TEXT,
  name_ja TEXT,
  combined_score FLOAT,
  vector_score FLOAT,
  text_score FLOAT,
  model_version TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    mv.member_id,
    m.name_en,
    m.name_ja,
    (vector_weight * (1 - (mv.combined_embedding <=> query_vector)) + 
     text_weight * ts_rank(to_tsvector('english', mv.searchable_text), plainto_tsquery('english', query_text)))::FLOAT as combined_score,
    (1 - (mv.combined_embedding <=> query_vector))::FLOAT as vector_score,
    ts_rank(to_tsvector('english', mv.searchable_text), plainto_tsquery('english', query_text))::FLOAT as text_score,
    mv.model_version
  FROM member_vectors mv
  JOIN members m ON mv.member_id = m.id
  WHERE 
    mv.combined_embedding <=> query_vector < 0.5 OR
    to_tsvector('english', mv.searchable_text) @@ plainto_tsquery('english', query_text)
  ORDER BY combined_score DESC
  LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;

-- 멤버별 벡터 품질 체크 함수
CREATE OR REPLACE FUNCTION check_vector_quality()
RETURNS TABLE (
  member_id INTEGER,
  name_en TEXT,
  has_all_vectors BOOLEAN,
  vector_dimension INTEGER,
  last_updated TIMESTAMP
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    mv.member_id,
    m.name_en,
    (mv.name_embedding IS NOT NULL AND 
     mv.description_embedding IS NOT NULL AND 
     mv.personality_embedding IS NOT NULL AND 
     mv.content_style_embedding IS NOT NULL AND 
     mv.combined_embedding IS NOT NULL) as has_all_vectors,
    1024 as vector_dimension,
    mv.updated_at
  FROM member_vectors mv
  JOIN members m ON mv.member_id = m.id
  ORDER BY mv.updated_at DESC;
END;
$$ LANGUAGE plpgsql;

-- 업데이트 타임스탬프 트리거
CREATE OR REPLACE FUNCTION update_vector_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_vector_timestamp
  BEFORE UPDATE ON member_vectors
  FOR EACH ROW
  EXECUTE FUNCTION update_vector_timestamp();

-- 권한 설정
GRANT SELECT, INSERT, UPDATE, DELETE ON member_vectors TO holo_user;
GRANT USAGE, SELECT ON SEQUENCE member_vectors_id_seq TO holo_user;
GRANT SELECT ON v_vector_stats TO holo_user;

-- 설정 완료 메시지
DO $$
BEGIN
  RAISE NOTICE '✅ Jina Embeddings v4 벡터 스키마 (1024차원) 생성 완료';
  RAISE NOTICE '📊 인덱스: HNSW (m=32, ef_construction=128) for combined_embedding';
  RAISE NOTICE '🔍 검색 함수: search_similar_members, hybrid_search';
  RAISE NOTICE '📈 통계 뷰: v_vector_stats';
END $$;