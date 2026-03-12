package org.example.ch.oronk.definition

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SchemaDefinition(
    val auth: Auth? = null,
    val data_objects: List<DataObject>,
    val web_objects: List<WebObject>
)

@Serializable
data class Auth(
    val ref_object: String,
    val exclude_fields: List<String> = emptyList(),
    val include_fields: List<String> = emptyList(),
    val pwd_field: String,
)

@Serializable
data class DataObject(
    val name: String,
    val fields: List<Field>
)

@Serializable
data class Field(
    val name: String,
    val type: String,
    val required: Boolean,
    val fk: String? = null  // Made nullable since it might not always be present
)

@Serializable
data class WebObject(
    val ref_object: String,
    val path: String,
    val endpoints: List<Endpoint>,
    val exclude_fields: List<String> = emptyList(),
    val include_fields: List<String> = emptyList()
)

@Serializable
data class Endpoint(
    val method: String,
    val plural: Boolean = false,
    val filterParams: List<String> = emptyList(),
    val auth: Boolean = false
)