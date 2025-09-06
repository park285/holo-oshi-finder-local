# 프로젝트 구조 현황

## 📊 파일 통계
- **Kotlin 파일**: 80개
- **Go 파일**: 1개
- **YAML 파일**: 15개
- **Gradle 파일**: 11개
- **Markdown 문서**: 4개
- **총 파일 수**: 111개

## 📁 디렉토리 구조

```
services/
├── msa/ (6개 마이크로서비스)
│   ├── api-gateway/         # Spring Cloud Gateway (백업, 포트 50099)
│   ├── eureka-server/       # Service Discovery (포트 50008)
│   ├── member-service/      # Member 관리 (포트 50001)
│   ├── vector-service/      # Vector 검색 (포트 50002)
│   ├── llm-analyzer-service/ # LLM 분석 (포트 50004)
│   └── search-service/      # 통합 검색 (포트 50006)
│
├── go-api-gateway/          # Go Gateway (메인, 포트 50000)
│   ├── main.go
│   ├── api-gateway (실행파일)
│   ├── config.yaml
│   ├── start-go-gateway.sh
│   └── GO_GATEWAY_QUICK_START.md
│
├── shared/                  # 공유 라이브러리
│   ├── common/             # 공통 코드
│   ├── config/             # 환경 설정
│   │   ├── application-common.yml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   ├── monitoring/         # 모니터링
│   └── monitoring-legacy/  # 레거시 모니터링
│
├── infrastructure/         # 인프라
│   ├── database/          # SQL 스크립트
│   ├── docker/            # Docker 설정
│   └── scripts/           # 운영 스크립트
│
├── docs/                   # 문서
│   ├── api/               # API 문서
│   ├── architecture/      # 아키텍처 문서
│   │   ├── KOTLIN_BEST_PRACTICES_CHECKLIST.md
│   │   └── MSA_PHASE1_VERIFICATION_REPORT.md
│   └── operations/        # 운영 가이드
│
├── tests/                  # 테스트
│   ├── integration/       # 통합 테스트
│   ├── performance/       # 성능 테스트
│   └── e2e/              # E2E 테스트
│
├── build.gradle.kts       # 루트 빌드 파일
├── settings.gradle.kts    # 프로젝트 설정
├── gradle.properties      # Gradle 속성
└── README.md             # 프로젝트 설명

```

## 🔍 각 서비스별 구조

### MSA 서비스 공통 구조
```
msa/{service-name}/
├── src/
│   ├── main/
│   │   ├── kotlin/com/holo/oshi/{service}/
│   │   │   ├── controller/      # REST 컨트롤러
│   │   │   ├── service/         # 비즈니스 로직
│   │   │   ├── repository/      # 데이터 접근
│   │   │   ├── model/           # 데이터 모델
│   │   │   ├── config/          # 설정
│   │   │   └── exception/       # 예외 처리
│   │   └── resources/
│   │       └── application.yml  # 서비스 설정
│   └── test/
│       └── kotlin/com/holo/oshi/{service}/
│           └── test/            # 테스트 코드
├── build.gradle.kts            # 빌드 설정
└── bin/                        # 컴파일된 파일
```

## 🚀 실행 가능한 서비스

### Go API Gateway (메인)
- **포트**: 50000
- **파일**: `go-api-gateway/main.go`
- **실행**: `./start-go-gateway.sh`

### MSA 서비스들
1. **Eureka Server**: 50008
2. **Member Service**: 50001
3. **Vector Service**: 50002
4. **LLM Analyzer**: 50004
5. **Search Service**: 50006
6. **API Gateway (Spring)**: 50099 (백업)

## 📝 주요 설정 파일

### Gradle 설정
- `settings.gradle.kts`: 프로젝트 모듈 정의
- `build.gradle.kts`: 루트 빌드 설정
- 각 서비스별 `build.gradle.kts`

### 환경 설정
- `shared/config/application-common.yml`: 공통 설정
- `shared/config/application-dev.yml`: 개발 환경
- `shared/config/application-prod.yml`: 운영 환경
- `go-api-gateway/config.yaml`: Go Gateway 설정

## 🔧 빌드 및 실행

### 전체 빌드
```bash
./gradlew clean build -x test
```

### 개별 서비스 실행
```bash
# Go API Gateway
./start-go-gateway.sh

# MSA 서비스
./gradlew :msa:eureka-server:bootRun
./gradlew :msa:member-service:bootRun
# ... 기타 서비스
```

## 📌 참고사항

- Go API Gateway가 메인 게이트웨이 (포트 50000)
- Spring Cloud Gateway는 백업용 (포트 50099)
- 모든 MSA 서비스는 Kotlin으로 구현
- Coroutines 기반 비동기 처리
- PostgreSQL + pgvector 사용
- Redis 캐싱 (포트 6380)
- RabbitMQ 메시징