package org.example

import io.netty.util.internal.RecyclableArrayList
import kotlinx.serialization.json.Json
import org.example.ch.oronk.definition.Field
import org.example.ch.oronk.definition.SchemaDefinition
import org.example.ch.oronk.definition.WebObject
import org.example.ch.oronk.generators.dataGenerateDataClass
import org.example.ch.oronk.generators.generateMain
import org.example.ch.oronk.generators.webEndpointGenerator
import org.example.ch.oronk.generators.webGenerateDataClass
import java.io.File


fun main() {
    val schema = Json.decodeFromString<SchemaDefinition>(testJson)
    val db_object_by_name = schema.data_objects.map { it.name to it }.toMap()

    val path = "./src/main/kotlin"

    val webModelPackage = listOf("ch", "oronk", "web", "model")
    val webEndpointPackage = listOf("ch", "oronk", "web", "endpoint")
    val dataModelPackage = listOf("ch", "oronk", "data", "model")
    val mainPackage = listOf("ch", "oronk")

    val webObjectPath = "$path/" + webModelPackage.joinToString("/")
    val dataPathname = "$path/" + dataModelPackage.joinToString("/")
    val webEndpointPath = "$path/" + webEndpointPackage.joinToString("/")
    val mainPath = "$path/${mainPackage.joinToString("/")}"

    val webObjectFieldList = ArrayList<Pair<WebObject, List<Field>>>()
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
        webObjectFieldList.add(Pair(webObject, fields))
        File("$webObjectPath/${webObject.ref_object}.kt").writeText(webObjectString)
    }

    File(dataPathname).mkdirs()
    for (dataObject in schema.data_objects) {
        val dataObjectString =
            dataGenerateDataClass(dataObject.name, dataModelPackage.joinToString("."), dataObject.fields)
        File("$dataPathname/${dataObject.name}.kt").writeText(dataObjectString)
    }

    val endpointString = webEndpointGenerator(
        webObjectFieldList,
        dataModelPackage.joinToString("."),
        webModelPackage.joinToString("."),
        webEndpointPackage.joinToString(".")
    )

    File(webEndpointPath).mkdirs()
    File("$webEndpointPath/Endpoints.kt").writeText(endpointString)

    val mainString = generateMain(
        mainPackage.joinToString("."),
        webEndpointPackage.joinToString("."),
        dataModelPackage.joinToString("."),
        "database",
        schema.data_objects
    )
    File(mainPath).mkdirs()
    File("${mainPath}/App.kt").writeText(mainString)
}

var testJson = """
{
  "data_objects": [
    {
      "name": "Test",
      "fields": [
        {
          "name": "email",
          "type": "int",
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
      "endpoints": [
        {
          "method": "GET",
          "plural": true
        },
        {
          "method": "GET",
          "plural": false,
          "filterParams": ["id"]
        },
        {
          "method": "POST",
          "plural": false
        }
      ],
      "ref_object": "Test",
      "path": "api",
      "exclude_fields": [
        "gender"
      ]
    }
  ]
}

"""