package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.DataObject

fun generateMain(mainPackage: String, webEndpointPackage: String, dataObjectPath: String, databaseName: String, dataObjects: List<DataObject>): String {
    return """
        package $mainPackage
        
        import ${webEndpointPackage}.generalRoutes
        import io.ktor.serialization.gson.gson
        import io.ktor.server.application.Application
        import io.ktor.server.application.install
        import io.ktor.server.engine.embeddedServer
        import io.ktor.server.netty.Netty
        import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
        import io.ktor.server.routing.routing
        import org.jetbrains.exposed.v1.jdbc.Database
        import org.jetbrains.exposed.v1.jdbc.SchemaUtils
        import org.jetbrains.exposed.v1.jdbc.transactions.transaction 
        
        fun main() {
            Database.connect(
                url = "jdbc:sqlite:./${databaseName}.db",
                driver = "org.sqlite.JDBC",
            )

            transaction {
                ${
                    dataObjects.map { 
                        "SchemaUtils.create($dataObjectPath.${it.name})"
                    }.joinToString("\n")
                }
            }
            
            embeddedServer(
                Netty,
                port = 8080,
                host = "0.0.0.0",
            ) {
                install(ContentNegotiation) {
                    gson()
                }
                routing {
                    generalRoutes()
                }
            }.start(wait = true)
        }
    """.trimIndent()
}
