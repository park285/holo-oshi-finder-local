#!/bin/bash
# 환경변수 자동 로드 스크립트

# .env 파일 먼저 로드 (백업용)
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
    echo ".env 환경변수 로드 완료"
fi

# .env.local 파일이 존재하면 나중에 로드 (우선순위) - 현재 디렉토리와 상위 디렉토리 모두 확인
if [ -f ".env.local" ]; then
    export $(grep -v '^#' .env.local | xargs)
    echo ".env.local 환경변수 로드 완료"
elif [ -f "../.env.local" ]; then
    export $(grep -v '^#' ../.env.local | xargs)
    echo "../.env.local 환경변수 로드 완료"
fi

# 필수 환경변수 확인
echo "주요 환경변수 확인:"
echo "GOOGLE_API_KEY: ${GOOGLE_API_KEY:0:20}..."
echo "OPENAI_API_KEY: ${OPENAI_API_KEY:0:20}..."
echo "REDIS_HOST: $REDIS_HOST"
echo "REDIS_PORT: $REDIS_PORT"
echo "DB_HOST: $DB_HOST"
echo "DB_PORT: $DB_PORT"
echo "DB_NAME: $DB_NAME"
echo "SERVER_PORT: ${SERVER_PORT:-8080}"