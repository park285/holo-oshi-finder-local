package com.holo.oshi.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

/**
 * API Gateway Application
 * 
 * MSA 시스템의 단일 진입점 (Single Entry Point)
 * 
 * 주요 책임:
 * 1. 라우팅: 클라이언트 요청을 적절한 마이크로서비스로 라우팅
 * 2. 부하 분산: Eureka + LoadBalancer를 통한 서비스 인스턴스 선택
 * 3. Circuit Breaker: 서비스 장애 시 격리 및 Fallback 처리
 * 4. 보안: CORS, Rate Limiting, 인증/인가 (향후 확장)
 * 5. 모니터링: 요청 추적, 메트릭 수집
 */
@SpringBootApplication
@EnableDiscoveryClient
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}