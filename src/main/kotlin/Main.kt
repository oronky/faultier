package org.example

import ch.oronk.data.model.Test
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import org.example.ch.oronk.definition.SchemaDefinition
import org.example.ch.oronk.generators.dataGenerateDataClass
import org.example.ch.oronk.generators.webGenerateDataClass
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import java.io.File

fun Route.main() {
    route("/") {
        get("api"){
            val select = Test.select(Test.email eq "test")
                .singleOrNull()
            if(select == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            ch.oronk.web.model.Test(
                email = "test",

            )
            val email: String = select[Test.email]

            call.respond("Hello World!")
        }
    }
}

fun main() {
    val schema = Json.decodeFromString<SchemaDefinition>(testJson)
    val db_object_by_name = schema.data_objects.map { it.name to it }.toMap()

    val path = "E:/Programmieren/Faultier/test"

    val webModelPackage = listOf("ch", "oronk", "web", "model")
    val dataModelPackage = listOf("ch", "oronk", "data", "model")


    val webObjectPath = "$path/" + webModelPackage.joinToString("/")
    val dataPathname = "$path/" + dataModelPackage.joinToString("/")

    File(webObjectPath).mkdirs()
    for (webObject in schema.web_objects) {
        var ref_object = db_object_by_name[webObject.ref_object]
        if (ref_object == null) {
            throw IllegalArgumentException("No reference object found for ${webObject.ref_object}.")
        }
        if (webObject.exclude_fields.isNotEmpty() and webObject.include_fields.isNotEmpty()) {
            throw IllegalArgumentException("You cant exclude and include fields at the same time. Just do one.")
        }
        val fields = (if (webObject.exclude_fields.isNotEmpty()) ref_object.fields else emptyList()).toMutableList()
        for (exclude_field in webObject.exclude_fields) {
            val exclude_index = fields.indexOfFirst { it.name == exclude_field }
            if (exclude_index == -1) {
                throw IllegalArgumentException("Exclude field $exclude_field not found")
            }
            fields.drop(exclude_index)
        }

        for (includeField in webObject.include_fields) {
            val includeIndex = fields.indexOfFirst { it.name == includeField }
            if (includeIndex == -1) {
                throw IllegalArgumentException("Include field $includeField not found")
            }
            fields += ref_object.fields[includeIndex]
        }
        val webObjectString =
            webGenerateDataClass(
                webObject.ref_object,
                webModelPackage.joinToString("."),
                dataModelPackage.joinToString("."),
                webObject.ref_object,
                fields
            )
        File("$webObjectPath/${webObject.ref_object}.kt").writeText(webObjectString)
    }

    File(dataPathname).mkdirs()
    for (dataObject in schema.data_objects) {
        val dataObjectString =
            dataGenerateDataClass(dataObject.name, dataModelPackage.joinToString("."), dataObject.fields)
        File("$dataPathname/${dataObject.name}.kt").writeText(dataObjectString)
    }

}

var testJson = """
{
  "data_objects": [
    {
      "name": "Test",
      "fields": [
        {
          "name": "email",
          "type": "string",
          "required": true,
          "fk": "Table.id"
        },
        {
          "name": "gender",
          "type": "int",
          "required": true,
          "fk": "Table.id"
        }
      ]
    }
  ],
  "web_objects": [
    {
      "methods": ["GET, POST"],
      "ref_object": "Test",
      "path": "/api/",
      "exclude_fields": [
        "gender"
      ]
    }
  ]
}
"""