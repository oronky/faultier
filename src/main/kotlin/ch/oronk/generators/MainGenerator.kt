package org.example.ch.oronk.generators

import org.jetbrains.exposed.v1.jdbc.Database

fun generateMain(mainPackage: String, databaseName: String): String {

    return """
        package $mainPackage
        
        import org.jetbrains.exposed.sql.Database
        
        fun main() {
            Database.connect(
                url = "jdbc:sqlite:./${databaseName}.db",
                driver = "org.sqlite.JDBC",
            )
        }
    """.trimIndent()
}