package com.holo.oshi.notification

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun main() {
    embeddedServer(Netty, port = 50010, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        
        routing {
            get("/health") {
                call.respond(mapOf(
                    "status" to "UP",
                    "service" to "notification-service", 
                    "port" to 50010,
                    "timestamp" to Clock.System.now()
                ))
            }
            
            route("/api") {
                route("/notifications") {
                    get {
                        call.respond(mapOf(
                            "message" to "Notification Service is running",
                            "timestamp" to Clock.System.now()
                        ))
                    }
                }
            }
        }
    }.start(wait = true)
}