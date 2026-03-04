package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.Endpoint
import org.example.ch.oronk.definition.Field
import org.example.ch.oronk.definition.WebObject

fun webEndpointGenerator(
    webObjects: List<WebObject>,
    dataPackage: String,
    webPackage: String,
    packageName: String
): String {
    val stringBuilder = StringBuilder()
    val imports = """
import io.ktor.server.routing.get 
import io.ktor.server.routing.Route
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.route

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
    """
    stringBuilder.appendLine("package $packageName)\n")
    stringBuilder.appendLine(imports)
    stringBuilder.appendLine("fun Route.generalRoutes() {\n")
    stringBuilder.appendLine("route(\"/\") {")
    webObjects.forEachIndexed { index, webObject ->
        webObject.endpoints.forEach { endpoint ->

        }
    }

    return stringBuilder.toString()
}

private fun routeString(webObject: WebObject, dataPackage: String, webPackage: String, endpoint: Endpoint): String {
    return when (endpoint.method) {
        "GET" -> endpointGet(webObject, endpoint, dataPackage, webPackage)
        else -> throw IllegalArgumentException("Unsupported endpoint method ${endpoint.method}")
    }
}

private fun routeGet(webObject: WebObject, fields: List<Field>, dataPackage: String, webPackage: String, endpoint: Endpoint): String {
    val stringBuilder = StringBuilder()
    val pluralSuffix = if (endpoint.plural) "s" else ""
    stringBuilder.appendLine("  get(\"${webObject.path}/${webObject.ref_object.lowercase()}$pluralSuffix\") {)")
    stringBuilder.appendLine("      val query = $dataPackage.${webObject.ref_object}")
    if (endpoint.filterParams.isNotEmpty()) {
        val selectString =
            endpoint.filterParams
                .map { param -> "($dataPackage.${webObject.ref_object}.${endpoint.filterParams})" }
                .joinToString(" and ")
        stringBuilder.appendLine("      .select{ $selectString }")
    }
    if (endpoint.plural) {

    } else {
        stringBuilder.appendLine("""
    .singleOrNull()
    if(select == null) {
        call.respond(HttpStatusCode.NotFound)
        return@get
    }
    """)
        stringBuilder.appendLine("  val returnObj = ${webObject.ref_object}(")
        stringBuilder.appendLine("  id = select[\"id\"],")
        fields.forEach { field ->
            stringBuilder.appendLine("  ${field.name} = select[\"${field.name}\",")
        }
        stringBuilder.appendLine(" )")

    }
    return stringBuilder.toString()
}