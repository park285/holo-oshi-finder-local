#!/bin/bash
# 통합 서비스 실행 스크립트
# Usage: ./run-service.sh <service-name> [port]

set -e

# 현재 스크립트 위치 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 사용법 출력
usage() {
    echo "Kotlin 서비스 실행 스크립트"
    echo ""
    echo "사용법: $0 <service-name> [port]"
    echo ""
    echo "서비스 목록:"
    echo "  llm-analyzer        LLM 분석 서비스 (기본 포트: 9008)"
    echo "  unified             통합 검색 서비스 (기본 포트: 8080)" 
    echo "  nlp                 NLP 처리 서비스 (기본 포트: 3001)"
    echo "  gemini-classifier   Gemini 분류 서비스 (기본 포트: 3002)"
    echo "  pgvector            PGVector 서비스 (기본 포트: 3003)"
    echo "  monitoring          모니터링 서비스 (기본 포트: 9010)"
    echo ""
    echo "예시:"
    echo "  $0 llm-analyzer           # 기본 포트로 실행"
    echo "  $0 llm-analyzer 9009      # 포트 9009로 실행"
    echo "  $0 unified 9005           # 통합 서비스를 포트 9005로 실행"
    exit 1
}

# 파라미터 검증
if [ $# -eq 0 ]; then
    usage
fi

SERVICE_NAME=$1
CUSTOM_PORT=$2

# 환경변수 로드
echo "환경변수 로드 중..."
source "$PROJECT_ROOT/load-env-safe.sh"

# 필수 환경변수 검증
if [ -z "$GOOGLE_API_KEY" ]; then
    echo "GOOGLE_API_KEY가 설정되지 않았습니다."
    exit 1
fi

# GOOGLE_PROJECT_ID 기본값 설정
if [ -z "$GOOGLE_PROJECT_ID" ]; then
    export GOOGLE_PROJECT_ID="holo-oshi-finder"
    echo "GOOGLE_PROJECT_ID를 기본값 'holo-oshi-finder'로 설정했습니다."
fi

# 서비스별 설정
case $SERVICE_NAME in
    "llm-analyzer"|"llm")
        GRADLE_PROJECT="llm-analyzer-service"
        DEFAULT_PORT=9008
        ENV_PREFIX="GOOGLE_API_KEY=$GOOGLE_API_KEY GOOGLE_PROJECT_ID=${GOOGLE_PROJECT_ID:-holo-oshi-finder} OPENAI_API_KEY=$OPENAI_API_KEY"
        ;;
    "unified"|"search")
        GRADLE_PROJECT="unified-service"
        DEFAULT_PORT=8080
        ENV_PREFIX="GOOGLE_API_KEY=$GOOGLE_API_KEY GOOGLE_PROJECT_ID=${GOOGLE_PROJECT_ID:-holo-oshi-finder} OPENAI_API_KEY=$OPENAI_API_KEY DB_HOST=${DB_HOST:-localhost} DB_PORT=${DB_PORT:-5433} REDIS_HOST=${REDIS_HOST:-localhost} REDIS_PORT=${REDIS_PORT:-6380}"
        ;;
    "nlp")
        GRADLE_PROJECT="nlp-service"
        DEFAULT_PORT=3001
        ENV_PREFIX=""
        ;;
    "gemini-classifier"|"gemini")
        GRADLE_PROJECT="gemini-classifier-service"
        DEFAULT_PORT=3002
        ENV_PREFIX="GOOGLE_API_KEY=$GOOGLE_API_KEY"
        ;;
    "pgvector"|"vector")
        GRADLE_PROJECT="pgvector-service"
        DEFAULT_PORT=3003
        ENV_PREFIX="GOOGLE_API_KEY=$GOOGLE_API_KEY"
        ;;
    "monitoring"|"monitor")
        GRADLE_PROJECT="monitoring-service"
        DEFAULT_PORT=9010
        ENV_PREFIX=""
        ;;
    *)
        echo "알 수 없는 서비스: $SERVICE_NAME"
        usage
        ;;
esac

# 포트 설정
SERVER_PORT=${CUSTOM_PORT:-$DEFAULT_PORT}
echo " $SERVICE_NAME 서비스를 포트 $SERVER_PORT 에서 실행합니다."

# 포트 충돌 확인
if lsof -i :$SERVER_PORT >/dev/null 2>&1; then
    echo "포트 $SERVER_PORT 이미 사용 중입니다."
    echo " 기존 프로세스를 종료하시겠습니까? (y/N)"
    read -r response
    if [[ "$response" =~ ^[yY]$ ]]; then
        echo "기존 프로세스 종료 중..."
        lsof -ti:$SERVER_PORT | xargs kill -9 2>/dev/null || true
        sleep 2
    else
        exit 1
    fi
fi

# Gradle 실행
echo " $SERVICE_NAME 서비스 실행 중..."
cd "$SCRIPT_DIR"

# 환경변수 설정 및 실행
eval "$ENV_PREFIX SERVER_PORT=$SERVER_PORT ./gradlew :$GRADLE_PROJECT:bootRun --no-daemon"