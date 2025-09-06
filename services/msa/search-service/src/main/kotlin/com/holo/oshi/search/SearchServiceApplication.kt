package com.holo.oshi.search

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class SearchServiceApplication

fun main(args: Array<String>) {
    runApplication<SearchServiceApplication>(*args)
}