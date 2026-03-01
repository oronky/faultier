package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.Field

fun webGenerateDataClass(className: String, refClassName: String, fields: List<Field>): String {
    val stringBuilder = StringBuilder()

    // Add data class declaration
    stringBuilder.appendLine("data class $className(")

    // Add fields
    fields.forEachIndexed { index, field ->
        val nullableSuffix = if (!field.required) "?" else ""
        val comma = if (index < fields.size - 1) "," else ""

        stringBuilder.appendLine("    val ${field.name}: ${fieldTypeConvert(field.type)}$nullableSuffix$comma")
    }

    stringBuilder.appendLine(") {")
    // Add copyFrom method
    stringBuilder.appendLine()
    stringBuilder.appendLine("    fun copyFrom(other: $refClassName): $className {")
    stringBuilder.appendLine("        return copy(")

    fields.forEachIndexed { index, field ->
        val comma = if (index < fields.size - 1) "," else ""
        stringBuilder.appendLine("            ${field.name} = other.${field.name}$comma")
    }

    stringBuilder.appendLine("        )")
    stringBuilder.appendLine("    }")
    stringBuilder.appendLine("}")

    return stringBuilder.toString()
}

private fun fieldTypeConvert(fieldType: String): String {
    return when (fieldType) {
        "string" -> "String"
        "int" -> "Int"
        else -> throw IllegalArgumentException("fieldType $fieldType not allowed.")
    }
}