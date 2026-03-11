package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.Auth
import org.example.ch.oronk.definition.Field

fun generateImports(authPackage: String): String {
    return """
        package ${'$'}authPackage

        import io.ktor.server.sessions.Sessions
    """.trimIndent()
}

fun generateAuthUser(fields: List<Field>): String {
    return """
        @Serializable
        data class AuthUser(
        id: String,
        ${
        fields.map { "val ${it.name}: ${dataClassTypeConverter(it.type)}" }.joinToString(",\n")
    }
        ) 
    """.trimIndent()
}

fun generateConfigureSession(authPackage: String): String {
    return """
        
        fun fun Application.configureSessions() {
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

fun generateAuthRoute(auth: Auth, dataPackage: String, fields: List<Field>) : String {
    auth.ref_object
    return """
        fun Route.authRoutes() {
          route("/auth") {
            post("/login") {
                val requestAuthUser = call.receive<AuthUser>()    
                val query = transaction {
                    ${dataPackage}.${auth.ref_object} 
                        .selectAll()
                        .where { (${dataPackage}.${auth.ref_object}.id eq requestAuthUser.id) }
                        .singleOrNull()
                }
                if (query == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }
                val dbPwd = query[${dataPackage}.${auth.ref_object}.${auth.pwd_field}]
                if (dbPwd == null && BCrypt.verifyer().verify($dataPackage.${auth.ref_object}.${auth.pwd_field}.toCharArray(), dbPwd)) {
                    call.sessions.set(
                        AuthUser(
                            id = query[${dataPackage}.${auth.ref_object}.id],
                            ${
                                fields.map {
                                    "${it.name} = query[${dataPackage}.${auth.ref_object}.${it.name}]"
                                }.joinToString(",\n")
                            }
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