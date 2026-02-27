package org.example.ch.oronk.generators

import org.example.ch.oronk.definition.Field

fun generateDataClass(className: String, fields: List<Field>): String {
    val stringBuilder = StringBuilder()

    // Add data class declaration
    stringBuilder.appendLine("data class $className(")

    // Add fields
    fields.forEachIndexed { index, field ->
        val nullableSuffix = if (!field.required) "?" else ""
        val comma = if (index < fields.size - 1) "," else ""

        stringBuilder.appendLine("    val ${field.name}: ${fieldTypeConvert(field.type)}$nullableSuffix$comma")
    }

    stringBuilder.appendLine(")")

    return stringBuilder.toString()
}

fun fieldTypeConvert(fieldType: String): String {
    return when (fieldType) {
        "String" -> "String"
        "Int" -> "Int"
        else -> throw IllegalArgumentException("fieldType ${fieldType} not allowed.")
    }
}