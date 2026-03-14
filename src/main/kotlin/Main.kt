package org.example

import kotlinx.serialization.json.Json
import org.example.ch.oronk.definition.DataObject
import org.example.ch.oronk.definition.Field
import org.example.ch.oronk.definition.SchemaDefinition
import org.example.ch.oronk.definition.WebObject
import org.example.ch.oronk.generators.*
import java.io.File


fun main() {
    val schema = Json.decodeFromString<SchemaDefinition>(testJson)
    val db_object_by_name = schema.data_objects.map { it.name to it }.toMap()

    val path = "./src/main/kotlin"

    val webModelPackage = listOf("ch", "oronk", "web", "model")
    val webEndpointPackage = listOf("ch", "oronk", "web", "endpoint")
    val dataModelPackage = listOf("ch", "oronk", "data", "model")
    val mainPackage = listOf("ch", "oronk")
    val authPackage = listOf("ch", "oronk", "auth")

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

        val fields = fields(webObject.exclude_fields, webObject.include_fields, ref_object)
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
        schema.data_objects,
        schema.auth != null,
        authPackage.joinToString(".")
    )
    File(mainPath).mkdirs()
    File("${mainPath}/App.kt").writeText(mainString)

    setupAuth(schema, path, dataModelPackage.joinToString("."), authPackage)
}

fun setupAuth(schema: SchemaDefinition, path: String, dataPackage: String, authPackage: List<String>) {
    val authPackageString = authPackage.joinToString(".")
    val authPath = "$path/${authPackage.joinToString("/")}"
    val authObj = schema.auth ?: return
    val refObj = schema.data_objects.find { it.name == authObj.ref_object }
    if (refObj == null) {
        throw IllegalArgumentException("No reference object found for Auth ${authObj.ref_object}.")
    }
    if (authObj.exclude_fields.isNotEmpty() && authObj.include_fields.isNotEmpty()) {
        throw IllegalArgumentException("You cant exclude and include fields at the same time. Auth")
    }

    val fields = fields(authObj.exclude_fields, authObj.include_fields, refObj)

    val authString = generateAuthString(authObj, authPackageString, dataPackage,  fields)

    File(authPath).mkdirs()
    File("${authPath}/Auth.kt").writeText(authString)
}

fun fields(excludeFields: List<String>, includeFields: List<String>, ref_object: DataObject): List<Field> {
    val fields = (if (includeFields.isEmpty()) ref_object.fields else emptyList()).toMutableList()
    for (exclude_field in excludeFields) {
        val exclude_index = fields.indexOfFirst { it.name == exclude_field }
        if (exclude_index == -1) {
            throw IllegalArgumentException("Exclude field $exclude_field not found")
        }
        fields.removeAt(exclude_index)
    }

    for (includeField in includeFields) {
        val includeIndex = fields.indexOfFirst { it.name == includeField }
        if (includeIndex == -1) {
            throw IllegalArgumentException("Include field $includeField not found")
        }
        fields += ref_object.fields[includeIndex]
    }
    return fields
}

var testJson = """
{
  "auth" : {
    "ref_object": "User",
    "pwd_field" : "password"
  },
  "data_objects": [
    {
      "name":  "User",
      "fields" : [
        {
          "name": "password",
          "type": "string",
          "required": true
        } 
      ]
    },
    {
      "name": "Test",
      "fields": [
        {
          "name": "email",
          "type": "int",
          "required": true
        },
        {
          "name": "gender",
          "type": "int",
          "required": false
        },
        {
          "name": "user",
          "type": "uuid",
          "required": true,
          "fk": "User.id"
        }
      ]
    }
  ],
  "web_objects": [
    {
      "endpoints": [
        {
          "method": "GET",
          "plural": true,
          "auth" : true
        },
        {
          "method": "GET",
          "plural": false,
          "filterParams": ["id"]
        },
        {
          "method": "POST",
          "plural": true 
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