package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.Field

fun dataGenerateDataClass(className: String, packageName: String, fields: List<Field>): String {
    val stringBuilder = StringBuilder()

    stringBuilder.append("package $packageName\n")

    stringBuilder.appendLine("import org.jetbrains.exposed.v1.core.Table\n")
    stringBuilder.appendLine("import kotlin.uuid.ExperimentalUuidApi\n")


    // Add data class declaration
    stringBuilder.appendLine("object $className : Table() {\n")
    stringBuilder.appendLine("    @OptIn(ExperimentalUuidApi::class)\n")
    stringBuilder.appendLine("    val id = uuid(\"id\").autoGenerate()")
    // Add fields
    fields.forEachIndexed { index, field ->
        val nullableSuffix = if (!field.required) ".nullable()" else ""
        val foreignKeySuffix = if (field.fk == null) "references(${field.fk})" else ""

        stringBuilder.appendLine("    val ${field.name} = ${fieldTypeConvert(field.type, field.name)}$nullableSuffix$foreignKeySuffix\n")
    }
    stringBuilder.appendLine("    @OptIn(ExperimentalUuidApi::class)\n")
    stringBuilder.appendLine("    override val primaryKey = PrimaryKey(id)\n")
    stringBuilder.appendLine("}")


    return stringBuilder.toString()
}

private fun fieldTypeConvert(fieldType: String, name: String): String {
    val name = name.lowercase()
    return when (fieldType) {
        "string" -> "varchar(\"$name\", 255)"
        "int" -> "integer(\"$name\")"
        else -> throw IllegalArgumentException("fieldType $fieldType not allowed.")
    }
}
