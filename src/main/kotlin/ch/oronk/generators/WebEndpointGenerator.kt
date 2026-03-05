package org.example.ch.oronk.generators

import com.sun.tools.javac.code.TypeAnnotationPosition.field
import org.example.ch.oronk.definition.Endpoint
import org.example.ch.oronk.definition.Field
import org.example.ch.oronk.definition.WebObject
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

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
    """
    stringBuilder.appendLine("package $packageName\n")
    stringBuilder.appendLine(imports)
    stringBuilder.appendLine("@OptIn(ExperimentalUuidApi::class)")
    stringBuilder.appendLine("fun Route.generalRoutes() {\n")
    stringBuilder.appendLine("route(\"/\") {")
    webObjects.forEachIndexed { index, (webObject, fields) ->
        webObject.endpoints.forEach { endpoint ->
            stringBuilder.appendLine(routeString(webObject, fields, dataPackage, webPackage, endpoint))
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
    val fieldNameMap = fields.associate{it.name to it.type}.toMutableMap()
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
                    
                    val ${p}Param = ${generateConvertParamString("${p}ParamReq", paramType)}
                """
            }.joinToString("\n")
        }
            
    """
    )
    stringBuilder.appendLine("      val query = $dataPackage.${webObject.ref_object}")
    if (endpoint.filterParams.isNotEmpty()) {
        val selectString =
            endpoint.filterParams
                .map { param -> "($dataPackage.${webObject.ref_object}.${param} eq ${param}Param)" }
                .joinToString(" and ")
        stringBuilder.appendLine("      .select($selectString)")
    }
    if (endpoint.plural) {
        stringBuilder.appendLine(
            """
    .toList()
    val returnObj = query.map { e -> 
        $webPackage.${webObject.ref_object}(
            id = e["id"],
            ${
                fields.map { field ->
                    "${field.name} = e[\"${field.name}\"],"
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
    if(query == null) {
        call.respond(HttpStatusCode.NotFound)
        return@get
    }
    """
        )
        stringBuilder.appendLine("  val returnObj = $webPackage.${webObject.ref_object}(")
        stringBuilder.appendLine("  id = query[\"id\"],")
        fields.forEach { field ->
            stringBuilder.appendLine("  ${field.name} = select[\"${field.name}\",")
        }
        stringBuilder.appendLine(" )")

    }

    stringBuilder.appendLine("  call.respond(returnObj)")
    stringBuilder.appendLine("}")
    return stringBuilder.toString()
}

private fun generateConvertParamString(param: String, toType: String): String {
    return when (toType) {
        "int" -> {
            """
            ${param}?.toIntOrNull()
        """.trimIndent()
        }
        "string" -> param
        "uuid" -> "Uuid.parse($param)"
        else -> throw IllegalArgumentException("Unsupported type $toType")
    }
}