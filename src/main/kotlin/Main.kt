package org.example

import kotlinx.serialization.json.Json
import org.example.ch.oronk.definition.DataObject
import org.example.ch.oronk.definition.SchemaDefinition
import org.example.ch.oronk.definition.WebObject
import org.example.ch.oronk.generators.dataGenerateDataClass
import org.example.ch.oronk.generators.webGenerateDataClass
import java.io.File
import java.nio.file.Files

fun main() {
    val schema = Json.decodeFromString<SchemaDefinition>(testJson)
    val db_object_by_name = schema.data_objects.map { it.name to it }.toMap()

    val path = "E:/Programmieren/Faultier/test"




     File("$path/web/model").mkdirs()
    for (webObject in schema.web_objects) {
        var ref_object = db_object_by_name[webObject.ref_object]
        if(ref_object == null) {
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

        for (include_field in webObject.include_fields) {
            val include_index = fields.indexOfFirst { it.name == include_field }
            if (include_index == -1) {
                throw IllegalArgumentException("Include field $include_field not found")
            }
            fields += ref_object.fields[include_index]
        }
        val webObjectString = webGenerateDataClass(webObject.ref_object, webObject.ref_object, fields)
        File("$path/web/model/${webObject.ref_object}.kt").writeText(webObjectString)
    }

    File("$path/data/model").mkdirs()
    for (dataObject in schema.data_objects) {
        val dataObjectString = dataGenerateDataClass(dataObject.name, dataObject.fields)
        File("$path/data/model/${dataObject.name}.kt").writeText(dataObjectString)
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