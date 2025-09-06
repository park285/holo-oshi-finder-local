package com.holo.oshi.member

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableR2dbcRepositories
class MemberServiceApplication

fun main(args: Array<String>) {
    runApplication<MemberServiceApplication>(*args)
}