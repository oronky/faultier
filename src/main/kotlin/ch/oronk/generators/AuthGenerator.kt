package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.Auth
import org.example.ch.oronk.definition.Field

fun generateAuthString(auth: Auth, authPackage: String, dataPackage: String, fields: List<Field>): String {
    return """
        ${generateImports(authPackage)}
        
        ${generateAuthUser(fields)}
        
        ${generateSessionUser()}
        
        ${generateConfigureSession(authPackage)}
        
        ${generateAuthRoute(auth, dataPackage, fields)}
    """.trimIndent()
}

private fun generateImports(authPackage: String): String {
    return """
        package $authPackage

        import at.favre.lib.crypto.bcrypt.BCrypt
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.Application
        import io.ktor.server.application.install
        import io.ktor.server.auth.authentication
        import io.ktor.server.auth.session
        import io.ktor.server.request.receive
        import io.ktor.server.response.respond
        import io.ktor.server.response.respondRedirect
        import io.ktor.server.routing.Route
        import io.ktor.server.routing.post
        import io.ktor.server.routing.route
        import io.ktor.server.sessions.Sessions
        import io.ktor.server.sessions.cookie
        import io.ktor.server.sessions.sessions
        import io.ktor.server.sessions.set
        import kotlinx.serialization.Serializable
        import org.jetbrains.exposed.v1.jdbc.transactions.transaction
        import org.jetbrains.exposed.v1.core.eq
        import org.jetbrains.exposed.v1.jdbc.selectAll
        import kotlin.uuid.Uuid
        import kotlin.uuid.ExperimentalUuidApi

    """.trimIndent()
}

private fun generateAuthUser(fields: List<Field>): String {
    return """
        @Serializable
        data class AuthUser(
        val id: String,
        ${
            fields.map { "val ${it.name}: ${dataClassTypeConverter(it.type)}" }.joinToString(",\n")
    }
        ) 
    """.trimIndent()
}

private fun generateSessionUser(): String {
    return """
        @Serializable
        data class SessionUser(
            val id: String,
        ) 
    """.trimIndent()
}

private fun generateConfigureSession(authPackage: String): String {
    return """
        
        fun Application.configureSessions() {
            install(Sessions) {
                cookie<AuthUser>("user_session") {
                    cookie.path = "/"
                    cookie.maxAgeInSeconds = 3600
                }
            }
            
            authentication {
                session<AuthUser>("auth-session") {
                    validate { session ->
                        if (session != null) {
                            session
                        } else {
                            null
                        }
                    }
                    challenge {
                        call.respondRedirect("/login")
                    }
                }
            }
        }
    """.trimIndent()
}

private fun generateAuthRoute(auth: Auth, dataPackage: String, fields: List<Field>) : String {
    auth.ref_object
    return """
        @OptIn(ExperimentalUuidApi::class)
        fun Route.authRoutes() {
          route("/auth") {
            post("/login") {
                val requestAuthUser = call.receive<AuthUser>()    
                val idParam = Uuid.parseOrNull(requestAuthUser.id) 
                if (idParam == null) {
                    call.respond(HttpStatusCode.BadRequest, "idParam needs to be type uuid")
                    return@post
                }

                val query = transaction {
                    ${dataPackage}.${auth.ref_object} 
                        .selectAll()
                        .where { (${dataPackage}.${auth.ref_object}.id eq idParam) }
                        .singleOrNull()
                }
                if (query == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }
                val dbPwd = query[${dataPackage}.${auth.ref_object}.${auth.pwd_field}]
                if (BCrypt.verifyer().verify(requestAuthUser.${auth.pwd_field}.toCharArray(), dbPwd.toCharArray()).verified) {
                    call.sessions.set(
                        SessionUser(
                            id = query[${dataPackage}.${auth.ref_object}.id].toString()
                        )
                    )
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
          }
        }
    """.trimIndent()
}

private fun dataClassTypeConverter(fieldType: String): String {
    return when (fieldType) {
        "string" -> "String"
        "int" -> "Int"
        else -> throw IllegalArgumentException("fieldType $fieldType not allowed.")
    }
}