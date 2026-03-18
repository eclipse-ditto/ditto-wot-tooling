/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.wot.kotlin.generator.plugin.util

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import org.eclipse.ditto.json.JsonArray
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.slf4j.LoggerFactory

/**
 * Extracts default values from WoT Thing Model schemas and generates
 * corresponding DEFAULT_* Kotlin constants for companion objects.
 *
 * Handles all WoT data types including primitives, enums, date-time, and arrays.
 */
object DefaultValueExtractor {

    private val logger = LoggerFactory.getLogger(DefaultValueExtractor::class.java)

    /** Extracts DEFAULT_* constants from a map of schema fields (or properties). */
    fun extractDefaultConstants(
        fields: Map<String, SingleDataSchema>,
        packageName: String
    ): List<PropertySpec> {
        return fields.mapNotNull { (name, schema) ->
            extractDefaultFromSchema(name, schema, packageName)
        }
    }

    /** Extracts a default constant from a single schema field. */
    internal fun extractDefaultFromSchema(
        fieldName: String,
        schema: SingleDataSchema,
        packageName: String
    ): PropertySpec? {
        val defaultValue = schema.default.orElse(null) ?: return null
        val schemaType = schema.type.orElse(null) ?: return null

        val constantName = asScreamingSnakeCase(fieldName)

        return when (schemaType) {
            DataSchemaType.BOOLEAN ->
                createBooleanDefault(constantName, defaultValue.asBoolean())

            DataSchemaType.INTEGER ->
                createIntegerDefault(constantName, defaultValue.asLong())

            DataSchemaType.NUMBER ->
                createNumberDefault(constantName, defaultValue.asDouble())

            DataSchemaType.STRING -> when {
                isStringEnum(schema) -> {
                    val enumName = asClassName(fieldName)
                    createEnumDefault(constantName, ClassName("", enumName), defaultValue.asString(), enumName)
                }
                isDateTimeFormat(schema) ->
                    createInstantDefault(constantName, defaultValue.asString())
                else ->
                    createStringDefault(constantName, defaultValue.asString())
            }

            DataSchemaType.OBJECT -> {
                if (isObjectEnum(schema)) {
                    if (!defaultValue.isObject) {
                        logger.warn("Object enum default for '$fieldName' is not an object, skipping")
                        return null
                    }
                    val nameField = defaultValue.asObject().getValue("name").orElse(null)
                    if (nameField == null || !nameField.isString) {
                        logger.warn("Object enum default for '$fieldName' is missing 'name' field, skipping")
                        return null
                    }
                    createObjectEnumDefault(constantName, ClassName("", asClassName(fieldName)), asValidEnumConstant(nameField.asString()))
                } else {
                    null
                }
            }

            DataSchemaType.ARRAY -> {
                if (!defaultValue.isArray) return null
                val defaultArray = defaultValue.asArray()
                val elementType = resolveArrayElementType(schema, packageName, fieldName)
                if (defaultArray.isEmpty) {
                    createEmptyListDefault(constantName, elementType)
                } else {
                    createListDefault(constantName, elementType, defaultArray, resolveArrayItemsEnum(schema))
                        ?: run {
                            logger.warn("Non-empty array default for '$fieldName' contains unsupported element type, skipping")
                            null
                        }
                }
            }

            DataSchemaType.NULL -> null
        }
    }

    private fun resolveArrayElementType(schema: SingleDataSchema, packageName: String, fieldName: String): TypeName {
        val itemsJson = schema.toJson().getValue("items").orElse(null)?.asObject() ?: run {
            logger.warn("Missing array items schema for '$fieldName', defaulting to String")
            return STRING
        }
        val itemsSchema = SingleDataSchema.fromJson(itemsJson)
        val itemType = itemsSchema.type.orElse(null) ?: run {
            logger.warn("Missing type in array items schema for '$fieldName', defaulting to String")
            return STRING
        }
        return when (itemType) {
            DataSchemaType.BOOLEAN -> BOOLEAN
            DataSchemaType.INTEGER -> LONG
            DataSchemaType.NUMBER -> DOUBLE
            DataSchemaType.STRING ->
                if (isStringEnum(itemsSchema)) ClassName("", asClassName(fieldName) + "Item")
                else STRING
            DataSchemaType.OBJECT -> ClassName(packageName, asClassName(fieldName) + "Item")
            DataSchemaType.ARRAY, DataSchemaType.NULL -> {
                logger.warn("Unsupported array items type '$itemType' for '$fieldName', defaulting to String")
                STRING
            }
        }
    }

    /** Creates a PropertySpec for a boolean default constant. */
    internal fun createBooleanDefault(
        constantName: String,
        value: Boolean
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", BOOLEAN)
            .addModifiers(KModifier.CONST)
            .initializer("%L", value)
            .build()
    }

    /** Creates a PropertySpec for an integer (Long) default constant. */
    internal fun createIntegerDefault(
        constantName: String,
        value: Long
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", LONG)
            .addModifiers(KModifier.CONST)
            .initializer("%L", value)
            .build()
    }

    /** Creates a PropertySpec for a number (Double) default constant. */
    internal fun createNumberDefault(
        constantName: String,
        value: Double
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", DOUBLE)
            .addModifiers(KModifier.CONST)
            .initializer("%L", value)
            .build()
    }

    /** Creates a PropertySpec for a string default constant. */
    internal fun createStringDefault(
        constantName: String,
        value: String
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", STRING)
            .addModifiers(KModifier.CONST)
            .initializer("%S", value)
            .build()
    }

    /** Creates a PropertySpec for a date-time (Instant) default constant. */
    internal fun createInstantDefault(
        constantName: String,
        value: String
    ): PropertySpec {
        val instantType = ClassName("java.time", "Instant")
        return PropertySpec.builder("DEFAULT_$constantName", instantType)
            .initializer("%T.parse(%S)", instantType, value)
            .build()
    }

    /** Creates a PropertySpec for a string enum default constant. */
    internal fun createEnumDefault(
        constantName: String,
        enumTypeName: ClassName,
        enumConstantValue: String,
        enumName: String
    ): PropertySpec {
        val convertedConstant = asEnumConstantName(enumName, enumConstantValue)
        return PropertySpec.builder("DEFAULT_$constantName", enumTypeName)
            .initializer("%T.%L", enumTypeName, convertedConstant)
            .build()
    }

    /** Creates a PropertySpec for an object enum (sealed class) default constant. */
    internal fun createObjectEnumDefault(
        constantName: String,
        sealedTypeName: ClassName,
        objectInstanceName: String
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", sealedTypeName)
            .initializer("%T.%L", sealedTypeName, objectInstanceName)
            .build()
    }

    /** Creates a PropertySpec for an empty list default constant. */
    internal fun createEmptyListDefault(
        constantName: String,
        elementType: TypeName
    ): PropertySpec {
        val listType = LIST.parameterizedBy(elementType)
        return PropertySpec.builder("DEFAULT_$constantName", listType)
            .initializer("emptyList()")
            .build()
    }

    /** Creates a PropertySpec for a non-empty list default constant. Returns null if element type is unsupported. */
    internal fun createListDefault(
        constantName: String,
        elementType: TypeName,
        defaultArray: JsonArray,
        itemsEnum: List<String>?
    ): PropertySpec? {
        val elements = mutableListOf<CodeBlock>()

        for (value in defaultArray) {
            val element = when (elementType) {
                BOOLEAN -> CodeBlock.of("%L", value.asBoolean())
                LONG -> CodeBlock.of("%L", value.asLong())
                DOUBLE -> CodeBlock.of("%L", value.asDouble())
                STRING -> CodeBlock.of("%S", value.asString())
                else -> {
                    // Check if it's an enum type
                    if (itemsEnum != null && value.isString && elementType is ClassName) {
                        val enumName = elementType.simpleName
                        val convertedConstant = asEnumConstantName(enumName, value.asString())
                        CodeBlock.of("%T.%L", elementType, convertedConstant)
                    } else {
                        return null
                    }
                }
            }
            elements.add(element)
        }

        val listType = LIST.parameterizedBy(elementType)
        val initializer = CodeBlock.builder()
            .add("listOf(")
            .add(elements.joinToString(", ") { it.toString() })
            .add(")")
            .build()
        return PropertySpec.builder("DEFAULT_$constantName", listType)
            .initializer(initializer)
            .build()
    }

    private fun resolveArrayItemsEnum(schema: SingleDataSchema): List<String>? {
        val itemsJson = schema.toJson().getValue("items").orElse(null)?.asObject() ?: return null
        val itemsSchema = SingleDataSchema.fromJson(itemsJson)
        if (!isStringEnum(itemsSchema)) return null
        return itemsSchema.enum.map { it.asString() }
    }

    /** Checks if a schema represents a string enum. */
    internal fun isStringEnum(schema: SingleDataSchema): Boolean {
        val schemaType = schema.type.orElse(null) ?: return false
        if (schemaType != DataSchemaType.STRING) return false
        return schema.enum.isNotEmpty()
    }

    /** Checks if a schema represents an object enum (sealed class). */
    internal fun isObjectEnum(schema: SingleDataSchema): Boolean {
        val schemaType = schema.type.orElse(null) ?: return false
        if (schemaType != DataSchemaType.OBJECT) return false
        return schema.enum.isNotEmpty()
    }

    /** Checks if a schema has a date-time format. */
    internal fun isDateTimeFormat(schema: SingleDataSchema): Boolean {
        val schemaType = schema.type.orElse(null) ?: return false
        if (schemaType != DataSchemaType.STRING) return false
        val format = schema.format.orElse(null) ?: return false
        return format.equals("date-time", ignoreCase = true)
    }
}
