rootProject.name = "holo-oshi-services-kotlin"

// 공통 모듈
include(
    "shared:common"
)

// MSA 마이크로서비스
include(
    "msa:eureka-server",
    "msa:api-gateway", 
    "msa:member-service",
    "msa:vector-service",
    "msa:search-service",
    "msa:llm-analyzer-service",
    "msa:notification-service"
)