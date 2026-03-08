package org.example.ch.oronk.generators

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
                        if (session.userId.isNotBlank()) {
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

private fun dataClassTypeConverter(fieldType: String): String {
    return when (fieldType) {
        "string" -> "String"
        "int" -> "Int"
        else -> throw IllegalArgumentException("fieldType $fieldType not allowed.")
    }
}