package com.ahmedhnewa

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.ahmedhnewa.plugins.*
import com.ahmedhnewa.services.secret_variables.SecretVariableName
import com.ahmedhnewa.services.secret_variables.SecretVariablesService
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv {
    ignoreIfMissing = true
}

fun main() {
    embeddedServer(factory = Netty, environment = applicationEngineEnvironment {
        connector { port = SecretVariablesService.require(SecretVariableName.ServerPort).toInt() }
        module(Application::module)
        developmentMode = SecretVariablesService.get(SecretVariableName.DevelopmentMode, "true").toBoolean()
        if (developmentMode) {
            watchPaths = listOf("classes", "resources")
        }
    })
        .start(wait = true)
}

fun Application.module() {
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
