package org.example.ch.oronk.generators

import com.sun.tools.javac.code.TypeAnnotationPosition.field
import org.example.ch.oronk.definition.Endpoint
import org.example.ch.oronk.definition.Field
import org.example.ch.oronk.definition.WebObject
import org.jetbrains.exposed.v1.jdbc.insert
import kotlin.uuid.ExperimentalUuidApi

fun webEndpointGenerator(
    webObjects: List<Pair<WebObject, List<Field>>>,
    dataPackage: String,
    webPackage: String,
    packageName: String
): String {
    val stringBuilder = StringBuilder()
    val imports = """
        
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

import io.ktor.server.routing.get 
import io.ktor.server.routing.Route
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.post
import io.ktor.server.request.receive

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
    """
    stringBuilder.appendLine("package $packageName\n")
    stringBuilder.appendLine(imports)
    stringBuilder.appendLine("@OptIn(ExperimentalUuidApi::class)")
    stringBuilder.appendLine("fun Route.generalRoutes() {\n")
    stringBuilder.appendLine("route(\"/\") {")
    webObjects.forEachIndexed { index, (webObject, fields) ->
        webObject.endpoints.forEach { endpoint ->
            if (endpoint.auth){
                stringBuilder.appendLine("      authenticate(\"user_session\") {")
            }
            stringBuilder.appendLine("  "+routeString(webObject, fields, dataPackage, webPackage, endpoint))
            if (endpoint.auth){
                stringBuilder.appendLine("      }")
            }
        }
    }
    stringBuilder.appendLine("  }")
    stringBuilder.appendLine("}")
    return stringBuilder.toString()
}

private fun routeString(
    webObject: WebObject,
    fields: List<Field>,
    dataPackage: String,
    webPackage: String,
    endpoint: Endpoint
): String {
    return when (endpoint.method) {
        "GET" -> routeGet(webObject, fields, dataPackage, webPackage, endpoint)
        "POST" -> postRoute(webObject, fields, dataPackage, webPackage, endpoint)
        else -> throw IllegalArgumentException("Unsupported endpoint method ${endpoint.method}")
    }
}

private fun routeGet(
    webObject: WebObject,
    fields: List<Field>,
    dataPackage: String,
    webPackage: String,
    endpoint: Endpoint
): String {
    val stringBuilder = StringBuilder()
    val pluralSuffix = if (endpoint.plural) "s" else ""
    stringBuilder.appendLine("      get(\"${webObject.path}/${webObject.ref_object.lowercase()}$pluralSuffix\") {")
    val fieldNameMap = fields.associate { it.name to it.type }.toMutableMap()
    fieldNameMap["id"] = "uuid"
    stringBuilder.appendLine(
        """
            ${
            endpoint.filterParams.map { p ->
                val paramType = fieldNameMap[p]
                if (paramType == null) {
                    throw IllegalArgumentException("Endpoint ${webObject.ref_object} param: ${p} does not exist")
                }
                """
                    val ${p}ParamReq = call.request.queryParameters["$p"]
                    
                    if (${p}ParamReq == null) {
                        call.respond(HttpStatusCode.BadRequest, "${p}Param is required and needs to be type {$paramType}") 
                        return@get
                    }
                    
                    val ${p}Param = ${generateConvertParamString("${p}ParamReq", "${p}Param", paramType)}
                """
            }.joinToString("\n")
        }
            
    """
    )
    stringBuilder.appendLine("      val query = transaction { $dataPackage.${webObject.ref_object}")
    stringBuilder.appendLine("      .selectAll()")
    if (endpoint.filterParams.isNotEmpty()) {
        val selectString =
            endpoint.filterParams
                .map { param -> "($dataPackage.${webObject.ref_object}.${param} eq ${param}Param)" }
                .joinToString(" and ")
        stringBuilder.appendLine("      .where{ $selectString }")
    }
    val webObjectClass = "$webPackage.${webObject.ref_object}"
    val dataObjectClass = "$dataPackage.${webObject.ref_object}"
    if (endpoint.plural) {
        stringBuilder.appendLine(
            """
    .toList() 
    }
    val returnObj = query.map { e -> 
        $webObjectClass(
            id = e.get(${dataObjectClass}.id).toString(),
            ${
                fields.map { field ->
                    "${field.name} = e.get($dataObjectClass.${field.name}).${rowToTypeSuffix(field.type)},"
                }.joinToString("\n")
            }
        )
    }
        """
        )
    } else {
        stringBuilder.appendLine(
            """
    .singleOrNull()
    }
    if(query == null) {
        call.respond(HttpStatusCode.NotFound)
        return@get
    }
    """
        )
        stringBuilder.appendLine("  val returnObj = $webObjectClass(")
        stringBuilder.appendLine("  id = query.get(${dataObjectClass}.id).toString(),")
        fields.forEach { field ->
            stringBuilder.appendLine(
                "  ${field.name} = query.get($dataObjectClass.${field.name}).${
                    rowToTypeSuffix(
                        field.type
                    )
                },"
            )
        }
        stringBuilder.appendLine(" )")

    }

    stringBuilder.appendLine("  call.respond(returnObj)")
    stringBuilder.appendLine("}")
    return stringBuilder.toString()
}

private fun postRoute(
    webObject: WebObject,
    fields: List<Field>,
    dataPackage: String,
    webPackage: String,
    endpoint: Endpoint
): String {

    val pluralSuffix = if (endpoint.plural) "s" else ""
    return """
        post("${webObject.path}/${webObject.ref_object.lowercase()}$pluralSuffix") {
            ${
        if (endpoint.plural) {
            """
            val obj${webObject.ref_object} = call.receive<List<${webPackage}.${webObject.ref_object}>>()
            transaction {
                $dataPackage.${webObject.ref_object}.batchInsert(obj${webObject.ref_object}) { 
                ${
                fields.map { field ->
                    "this[$dataPackage.${webObject.ref_object}.${field.name}] = ${toDbObject(field.type, "it.${field.name}")}"
                }.joinToString("\n")
            }
                }
            }
            """.trimIndent()
        } else {

            """
            val obj${webObject.ref_object} = call.receive<${webPackage}.${webObject.ref_object}>()
            transaction {
            ${dataPackage}.${webObject.ref_object}.insert{
                ${
                fields.map { field ->
                    "it[${dataPackage}.${webObject.ref_object}.${field.name}]=obj${webObject.ref_object}.${field.name}"
                }.joinToString("\n")
            }   }
            }
            """.trimIndent()
        }
    }
    call.respond(HttpStatusCode.Created)
    }
    """.trimIndent()
}

private fun generateConvertParamString(param: String, newParam: String, toType: String): String {
    return when (toType) {
        "int" -> {
            """
            ${param}?.toIntOrNull()
        """.trimIndent()
        }
        "string" -> param
        "uuid" -> """
            Uuid.parseOrNull(${param})
            if (${newParam} == null) {
                call.respond(HttpStatusCode.BadRequest, "idParam needs to be type uuid")
                return@get
            }

        """.trimIndent()
        else -> throw IllegalArgumentException("Unsupported type $toType")
    }
}

private fun rowToTypeSuffix(toType: String): String {
    return when (toType) {
        "int" -> "toInt()"
        "string", "uuid" -> "toString()"
        else -> throw IllegalArgumentException("Unsupported type $toType")
    }
}

private fun toDbObject(type: String, fieldName: String): String {
    return when (type) {
        "uuid" -> """
            Uuid.parse(${fieldName})
        """.trimIndent()
        else -> "$fieldName"
    }
}