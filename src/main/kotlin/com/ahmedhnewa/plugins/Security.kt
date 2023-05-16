package com.ahmedhnewa.plugins

import com.ahmedhnewa.utils.constants.FoldersConstants
import com.ahmedhnewa.utils.extensions.getUserWorkingDirectory
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.io.File
import kotlin.collections.listOf
import kotlin.collections.set

data class MySession(val count: Int = 0)

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    intercept(ApplicationCallPipeline.Call) {
        val file = File(getUserWorkingDirectory(), FoldersConstants.SERVER_LOCKED_FILE_NAME)
        if (file.exists()) {
            call.respond(HttpStatusCode.ServiceUnavailable, "Sorry, the service is undergoing maintenance.")
            return@intercept
        }
    }

    authentication {
        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
                )
            }
            client = HttpClient(Apache)
        }
    }
    authentication {
        jwt {
//                val jwtAudience = this@configureSecurity.environment.config.property("jwt.audience").getString()
//                realm = this@configureSecurity.environment.config.property("jwt.realm").getString()
            verifier(
                JWT
                    .require(Algorithm.HMAC256("secret"))
                    .withAudience("jwtAudience")
                    .withIssuer("ddsadas")
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains("jwtAudience")) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
