package com.holo.oshi.llmanalyzer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

/**
 * LLM Analyzer Service Application
 * 
 * MSA 환경에서 LLM 기반 분석을 전담하는 독립 서비스
 * 
 * 주요 책임:
 * 1. RAG 검색 결과에 대한 최종 LLM 분석
 * 2. 설문 응답 기반 개인화 분석
 * 3. 빠른 LLM 분석 (캐시 활용)
 * 4. AI 모델 관리 및 토큰 비용 계산
 * 5. 멀티 모델 지원 (Gemini, OpenAI 등)
 * 
 * 포트: 50004
 */
@SpringBootApplication
@EnableDiscoveryClient
class LLMAnalyzerServiceApplication

fun main(args: Array<String>) {
    runApplication<LLMAnalyzerServiceApplication>(*args)
}