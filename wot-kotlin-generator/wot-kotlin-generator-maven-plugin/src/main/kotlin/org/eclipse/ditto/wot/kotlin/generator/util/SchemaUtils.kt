/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.wot.kotlin.generator.util

import com.squareup.kotlinpoet.*
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.ObjectSchema
import org.eclipse.ditto.wot.model.SingleDataSchema

/**
 * Converts a JSON array to a list of typed values.
 *
 * @param array The JSON array to convert
 * @param schemaType The expected data type for the array elements
 * @return A list of typed values
 * @throws IllegalArgumentException if the schema type is not supported
 */
fun asListOf(array: MutableSet<JsonValue>, schemaType: DataSchemaType): List<Any> {
    return when (schemaType) {
        DataSchemaType.INTEGER -> array.map { it.asInt() }
        DataSchemaType.NUMBER  -> array.map { it.asDouble() }
        DataSchemaType.STRING  -> array.map { it.asString() }
        DataSchemaType.OBJECT -> array.map { it.asObject() }
        else                   -> throw IllegalArgumentException("Unsupported type for enum property $schemaType")
    }
}

/**
 * Checks if a schema type is primitive.
 *
 * @param schemaType The schema type to check
 * @return true if the schema type is primitive, false otherwise
 */
fun isPrimitive(schemaType: DataSchemaType?): Boolean {
    return when (schemaType) {
        DataSchemaType.OBJECT  -> false
        DataSchemaType.BOOLEAN -> true
        DataSchemaType.INTEGER -> true
        DataSchemaType.NUMBER  -> true
        DataSchemaType.STRING  -> true
        DataSchemaType.ARRAY   -> false
        DataSchemaType.NULL    -> false
        null                   -> false
    }
}

/**
 * Checks if a Kotlin type is primitive.
 *
 * @param type The Kotlin type to check
 * @return true if the type is primitive, false otherwise
 */
fun isPrimitive(type: TypeName): Boolean {
    val nonNullableType = type.copy(nullable = false)
    return nonNullableType == STRING ||
        nonNullableType == INT ||
        nonNullableType == LONG ||
        nonNullableType == ClassName.bigInteger() ||
        nonNullableType == BOOLEAN ||
        nonNullableType == FLOAT ||
        nonNullableType == DOUBLE ||
        nonNullableType == ClassName.bigDecimal()

}

/**
 * Checks if a schema type is an array.
 *
 * @param schemaType The schema type to check
 * @return true if the schema type is an array, false otherwise
 */
fun isArray(schemaType: DataSchemaType?): Boolean {
    return when (schemaType) {
        DataSchemaType.ARRAY -> true
        else                 -> false
    }
}

/**
 * Converts a schema to its corresponding Kotlin primitive class name.
 *
 * @param schema The schema to convert
 * @param nullable Whether the resulting type should be nullable
 * @return The Kotlin class name for the primitive type
 * @throws IllegalArgumentException if the schema is not of primitive type
 */
fun asPrimitiveClassName(schema: SingleDataSchema, nullable: Boolean = false): ClassName {
    return when (schema.type.get()) {
        DataSchemaType.BOOLEAN -> BOOLEAN.copy(nullable = nullable) as ClassName
        DataSchemaType.INTEGER -> LONG.copy(nullable = nullable) as ClassName
        DataSchemaType.NUMBER  -> DOUBLE.copy(nullable = nullable) as ClassName
        DataSchemaType.STRING  -> {
            if (schema.format.isPresent && schema.format.get().equals("date-time", ignoreCase = true)) {
                ClassName("java.time", "Instant").copy(nullable = nullable) as ClassName
            } else {
                STRING.copy(nullable = nullable) as ClassName
            }
        }

        else                   -> throw IllegalArgumentException("$schema is not of primitive type!")
    }
}

fun ObjectSchema.isAdditionalPropertiesSchema() =
    this.toJson().getValue(JsonPointer.of("additionalProperties")).filter { it.isObject }.isPresent

fun ObjectSchema.isPatternPropertiesSchema() =
    this.toJson().getValue(JsonPointer.of("patternProperties")).filter { it.isObject }.isPresent

fun ClassName.Companion.bigInteger(nullable: Boolean = false) =
    ClassName("java.math", "BigInteger").copy(nullable = nullable) as ClassName

fun ClassName.Companion.bigDecimal(nullable: Boolean = false) =
    ClassName("java.math", "BigDecimal").copy(nullable = nullable) as ClassName


fun extractAndFormatValue(schemaType: DataSchemaType, jsonValue: JsonValue?): Pair<String, Any> {
    return when (schemaType) {
        DataSchemaType.STRING  -> "%S" to (jsonValue?.asString() ?: "")
        DataSchemaType.INTEGER -> "%L" to (jsonValue?.asLong() ?: 0L)
        DataSchemaType.NUMBER  -> "%L" to (jsonValue?.asDouble() ?: 0.0)
        DataSchemaType.BOOLEAN -> "%L" to (jsonValue?.asBoolean() ?: false)
        else                   -> throw IllegalArgumentException("Unsupported enum value type: ${schemaType}")
    }
}


fun asValidEnumConstant(input: String): String {
    val result = input
        .uppercase()
        .replace(Regex("[^A-Z0-9_]"), "_")
        .replace(Regex("__+"), "_")
        .trim('_')

    return if (result.isEmpty() || result.firstOrNull()?.isDigit() == true) {
        "VALUE_$result"
    } else {
        result
    }
}

fun extractObjectSchemaForClassGeneration(schemaJson: org.eclipse.ditto.json.JsonObject): ObjectSchema? {
    val typeValue = schemaJson.getValue(JsonPointer.of("type"))
    val typeString = if (typeValue.isPresent) typeValue.get().toString().trim('"') else null
    if (typeString == "array") {
        val itemsValue = schemaJson.getValue(JsonPointer.of("items"))
        val itemsJson = if (itemsValue.isPresent && itemsValue.get().isObject) itemsValue.get().asObject() else null
        if (itemsJson != null) return ObjectSchema.fromJson(itemsJson)
    }
    if (typeString == "object") {
        return ObjectSchema.fromJson(schemaJson)
    }
    val patternPropsValue = schemaJson.getValue(JsonPointer.of("patternProperties"))
    val patternProps = if (patternPropsValue.isPresent && patternPropsValue.get().isObject) patternPropsValue.get().asObject() else null
    if (patternProps != null) {
        val firstPattern = patternProps.keys.firstOrNull()
        if (firstPattern != null) {
            val patternField = patternProps.getField(firstPattern)
            val patternObj = if (patternField.isPresent && patternField.get().value.isObject) patternField.get().value.asObject() else null
            if (patternObj != null) {
                return extractObjectSchemaForClassGeneration(patternObj)
            }
        }
    }
    val addPropsValue = schemaJson.getValue(JsonPointer.of("additionalProperties"))
    val addProps = if (addPropsValue.isPresent && addPropsValue.get().isObject) addPropsValue.get().asObject() else null
    if (addProps != null) {
        return extractObjectSchemaForClassGeneration(addProps)
    }
    return null
}