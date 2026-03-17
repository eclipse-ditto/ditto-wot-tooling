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
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.slf4j.LoggerFactory

/**
 * Extracts default values from WoT Thing Model schemas and generates
 * corresponding Kotlin constants for companion objects.
 *
 * This object handles the extraction of `"default"` values from JSON-LD schemas
 * and generates `DEFAULT_<NAME>` companion object constants with appropriate types:
 * - Boolean defaults: `const val DEFAULT_<NAME>: Boolean = <value>`
 * - Integer defaults: `const val DEFAULT_<NAME>: Long = <value>`
 * - Number defaults: `const val DEFAULT_<NAME>: Double = <value>`
 * - String defaults: `const val DEFAULT_<NAME>: String = "<value>"`
 * - DateTime defaults: `val DEFAULT_<NAME>: Instant = Instant.parse("<value>")`
 * - Enum defaults: `val DEFAULT_<NAME>: EnumType = EnumType.CONSTANT`
 * - Object enum (sealed class) defaults: `val DEFAULT_<NAME>: SealedType = SealedType.INSTANCE`
 * - Empty array defaults: `val DEFAULT_<NAME>: List<T> = emptyList()`
 *
 * @since 1.0.0
 */
object DefaultValueExtractor {

    private val logger = LoggerFactory.getLogger(DefaultValueExtractor::class.java)

    /**
     * Extracts default value constants from a collection of properties.
     *
     * Processes each property in the map and extracts any defined default values,
     * generating appropriate constant definitions for companion objects.
     *
     * @param properties Map of property names to their Property definitions
     * @param packageName The package name for type resolution
     * @return List of PropertySpec for DEFAULT_* constants, empty if no defaults found
     */
    fun extractDefaultConstants(
        properties: Map<String, Property>,
        packageName: String
    ): List<PropertySpec> {
        return properties.mapNotNull { (name, property) ->
            extractDefaultFromProperty(name, property, packageName)
        }
    }

    /**
     * Extracts default value constants from a collection of schema fields.
     *
     * Processes each field in the map and extracts any defined default values,
     * generating appropriate constant definitions for companion objects.
     *
     * @param fields Map of field names to their SingleDataSchema definitions
     * @param packageName The package name for type resolution
     * @return List of PropertySpec for DEFAULT_* constants, empty if no defaults found
     */
    fun extractDefaultConstantsFromFields(
        fields: Map<String, SingleDataSchema>,
        packageName: String
    ): List<PropertySpec> {
        return fields.mapNotNull { (name, schema) ->
            extractDefaultFromSchema(name, schema, packageName)
        }
    }

    /**
     * Extracts a default constant from a single property.
     *
     * Analyzes the property schema to determine the type and default value,
     * then generates the appropriate constant specification.
     *
     * @param propertyName The original property name (will be converted to SCREAMING_SNAKE_CASE)
     * @param property The Property definition containing the schema
     * @param packageName The package name for type resolution
     * @return The extracted default, or null if no default is defined or type is unsupported
     */
    private fun extractDefaultFromProperty(
        propertyName: String,
        property: Property,
        packageName: String
    ): PropertySpec? {
        // Property extends SingleDataSchema, so we can treat it as a schema
        return extractDefaultFromSchema(propertyName, property, packageName)
    }

    /**
     * Extracts a default constant from a single schema field.
     *
     * Analyzes the schema to determine the type and default value,
     * then generates the appropriate constant specification.
     *
     * @param fieldName The original field name (will be converted to SCREAMING_SNAKE_CASE)
     * @param schema The SingleDataSchema definition
     * @param packageName The package name for type resolution
     * @return The extracted default, or null if no default is defined or type is unsupported
     */
    internal fun extractDefaultFromSchema(
        fieldName: String,
        schema: SingleDataSchema,
        packageName: String
    ): PropertySpec? {
        val defaultValue = schema.default.orElse(null) ?: return null
        val schemaType = schema.type.orElse(null) ?: return null

        return when (schemaType) {
            DataSchemaType.BOOLEAN -> {
                val value = defaultValue.asBoolean()
                val propSpec = createBooleanDefault(asScreamingSnakeCase(fieldName), value)
                propSpec
            }

            DataSchemaType.INTEGER -> {
                val value = defaultValue.asLong()
                val propSpec = createIntegerDefault(asScreamingSnakeCase(fieldName), value)
                propSpec
            }

            DataSchemaType.NUMBER -> {
                val value = defaultValue.asDouble()
                val propSpec = createNumberDefault(asScreamingSnakeCase(fieldName), value)
                propSpec
            }

            DataSchemaType.STRING -> {
                // Check if it's an enum
                if (isStringEnum(schema)) {
                    val enumName = asClassName(fieldName)
                    val enumTypeName = ClassName("", enumName)
                    val enumConstantValue = defaultValue.asString()
                    val propSpec = createEnumDefault(asScreamingSnakeCase(fieldName), enumTypeName, enumConstantValue, enumName)
                    propSpec
                }
                // Check if it's a date-time format
                else if (isDateTimeFormat(schema)) {
                    val value = defaultValue.asString()
                    val propSpec = createInstantDefault(asScreamingSnakeCase(fieldName), value)
                    propSpec
                }
                // Plain string
                else {
                    val value = defaultValue.asString()
                    val propSpec = createStringDefault(asScreamingSnakeCase(fieldName), value)
                    propSpec
                }
            }

            DataSchemaType.OBJECT -> {
                // Check if it's an object enum (sealed class)
                if (isObjectEnum(schema)) {
                    // Default must be an object with a "name" field
                    if (!defaultValue.isObject) {
                        logger.warn("Object enum default for '$fieldName' is not an object, skipping")
                        return null
                    }
                    val defaultObj = defaultValue.asObject()
                    val nameField = defaultObj.getValue("name").orElse(null)
                    if (nameField == null || !nameField.isString) {
                        logger.warn("Object enum default for '$fieldName' is missing 'name' field, skipping")
                        return null
                    }
                    val nameValue = nameField.asString()
                    val sealedTypeName = ClassName("", asClassName(fieldName))
                    val objectInstanceName = asValidEnumConstant(nameValue)
                    val propSpec = createObjectEnumDefault(asScreamingSnakeCase(fieldName), sealedTypeName, objectInstanceName)
                    propSpec
                } else {
                    // EDGE-2: Nested object defaults - skip silently
                    null
                }
            }

            DataSchemaType.ARRAY -> {
                if (!defaultValue.isArray) {
                    return null
                }
                val defaultArray = defaultValue.asArray()
                val elementType = resolveArrayElementType(schema, packageName, fieldName)
                if (defaultArray.isEmpty) {
                    val propSpec = createEmptyListDefault(asScreamingSnakeCase(fieldName), elementType)
                    propSpec
                } else {
                    val itemsEnum = resolveArrayItemsEnum(schema)
                    val propSpec = createListDefault(asScreamingSnakeCase(fieldName), elementType, defaultArray, itemsEnum, fieldName)
                    if (propSpec != null) {
                        propSpec
                    } else {
                        logger.warn("Non-empty array default for '$fieldName' contains unsupported element type, skipping")
                        null
                    }
                }
            }

            DataSchemaType.NULL -> null
        }
    }

    /**
     * Resolves the element type for an array schema.
     */
    private fun resolveArrayElementType(schema: SingleDataSchema, packageName: String, fieldName: String): TypeName {
        val schemaJson = schema.toJson()
        val itemsValue = schemaJson.getValue("items").orElse(null)
        if (itemsValue != null && itemsValue.isObject) {
            val itemsJson = itemsValue.asObject()
            val itemType = itemsJson.getValue("type").orElse(null)
            if (itemType != null && itemType.isString) {
                return when (itemType.asString()) {
                    "boolean" -> BOOLEAN
                    "integer" -> LONG
                    "number" -> DOUBLE
                    "string" -> {
                        val hasEnum = itemsJson.getValue("enum").orElse(null)
                        if (hasEnum != null && hasEnum.isArray && !hasEnum.asArray().isEmpty) {
                            ClassName("", asClassName(fieldName) + "Item")
                        } else {
                            STRING
                        }
                    }
                    "object" -> ClassName(packageName, asClassName(fieldName) + "Item")
                    else -> STRING
                }
            }
        }
        return STRING
    }

    /**
     * Creates a PropertySpec for a boolean default constant.
     *
     * Generates: `const val DEFAULT_<NAME>: Boolean = <value>`
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param value The boolean default value
     * @return PropertySpec for the constant
     */
    internal fun createBooleanDefault(
        constantName: String,
        value: Boolean
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", BOOLEAN)
            .addModifiers(KModifier.CONST)
            .initializer("%L", value)
            .build()
    }

    /**
     * Creates a PropertySpec for an integer (Long) default constant.
     *
     * Generates: `const val DEFAULT_<NAME>: Long = <value>`
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param value The Long default value
     * @return PropertySpec for the constant
     */
    internal fun createIntegerDefault(
        constantName: String,
        value: Long
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", LONG)
            .addModifiers(KModifier.CONST)
            .initializer("%L", value)
            .build()
    }

    /**
     * Creates a PropertySpec for a number (Double) default constant.
     *
     * Generates: `const val DEFAULT_<NAME>: Double = <value>`
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param value The Double default value
     * @return PropertySpec for the constant
     */
    internal fun createNumberDefault(
        constantName: String,
        value: Double
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", DOUBLE)
            .addModifiers(KModifier.CONST)
            .initializer("%L", value)
            .build()
    }

    /**
     * Creates a PropertySpec for a plain string default constant.
     *
     * Generates: `const val DEFAULT_<NAME>: String = "<value>"`
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param value The String default value
     * @return PropertySpec for the constant
     */
    internal fun createStringDefault(
        constantName: String,
        value: String
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", STRING)
            .addModifiers(KModifier.CONST)
            .initializer("%S", value)
            .build()
    }

    /**
     * Creates a PropertySpec for a date-time (Instant) default constant.
     *
     * Generates: `val DEFAULT_<NAME>: Instant = Instant.parse("<value>")`
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param value The ISO-8601 date-time string
     * @return PropertySpec for the constant
     */
    internal fun createInstantDefault(
        constantName: String,
        value: String
    ): PropertySpec {
        val instantType = ClassName("java.time", "Instant")
        return PropertySpec.builder("DEFAULT_$constantName", instantType)
            .initializer("%T.parse(%S)", instantType, value)
            .build()
    }

    /**
     * Creates a PropertySpec for a string enum default constant.
     *
     * Generates: `val DEFAULT_<NAME>: EnumType = EnumType.CONSTANT`
     * Uses `asEnumConstantName()` to convert the default value to enum constant format.
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param enumTypeName The ClassName of the enum type (unqualified for inline enums)
     * @param enumConstantValue The raw enum constant value from the schema default
     * @param enumName The name of the enum class (for asEnumConstantName conversion)
     * @return PropertySpec for the constant
     */
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

    /**
     * Creates a PropertySpec for an object enum (sealed class) default constant.
     *
     * Generates: `val DEFAULT_<NAME>: SealedType = SealedType.INSTANCE`
     * Extracts the `"name"` field from the JSON object default and uses
     * `asValidEnumConstant()` to convert to object name format.
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param sealedTypeName The ClassName of the sealed class type (unqualified for inline)
     * @param objectInstanceName The name of the sealed class object instance
     * @return PropertySpec for the constant
     */
    internal fun createObjectEnumDefault(
        constantName: String,
        sealedTypeName: ClassName,
        objectInstanceName: String
    ): PropertySpec {
        return PropertySpec.builder("DEFAULT_$constantName", sealedTypeName)
            .initializer("%T.%L", sealedTypeName, objectInstanceName)
            .build()
    }

    /**
     * Creates a PropertySpec for an empty list default constant.
     *
     * Generates: `val DEFAULT_<NAME>: List<T> = emptyList()`
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param elementType The type of elements in the list
     * @return PropertySpec for the constant
     */
    internal fun createEmptyListDefault(
        constantName: String,
        elementType: TypeName
    ): PropertySpec {
        val listType = LIST.parameterizedBy(elementType)
        return PropertySpec.builder("DEFAULT_$constantName", listType)
            .initializer("emptyList()")
            .build()
    }

    /**
     * Creates a PropertySpec for a non-empty list default constant.
     *
     * Supports primitive element types (Boolean, Long, Double, String) and string enums.
     *
     * @param constantName The constant name in SCREAMING_SNAKE_CASE (without DEFAULT_ prefix)
     * @param elementType The type of elements in the list
     * @param defaultArray The JSON array containing the default values
     * @param itemsEnum The enum values from the items schema, or null if not an enum array
     * @param fieldName The original field name (for enum name resolution)
     * @return PropertySpec for the constant, or null if the element type is unsupported
     */
    internal fun createListDefault(
        constantName: String,
        elementType: TypeName,
        defaultArray: JsonArray,
        itemsEnum: List<String>?,
        fieldName: String
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

    /**
     * Resolves enum values from the items schema of an array, if present.
     *
     * @param schema The array schema
     * @return List of enum string values, or null if items is not a string enum
     */
    private fun resolveArrayItemsEnum(schema: SingleDataSchema): List<String>? {
        val schemaJson = schema.toJson()
        val itemsValue = schemaJson.getValue("items").orElse(null) ?: return null
        if (!itemsValue.isObject) return null
        val itemsJson = itemsValue.asObject()
        val itemType = itemsJson.getValue("type").orElse(null)
        if (itemType == null || !itemType.isString || itemType.asString() != "string") return null
        val enumArray = itemsJson.getValue("enum").orElse(null) ?: return null
        if (!enumArray.isArray) return null
        return enumArray.asArray().map { it.asString() }
    }

    /**
     * Determines if a schema represents a string enum.
     *
     * @param schema The SingleDataSchema to check
     * @return true if this is a string type with enum values
     */
    internal fun isStringEnum(schema: SingleDataSchema): Boolean {
        val schemaType = schema.type.orElse(null) ?: return false
        if (schemaType != DataSchemaType.STRING) return false
        return schema.enum.isNotEmpty()
    }

    /**
     * Determines if a schema represents an object enum (sealed class).
     *
     * @param schema The SingleDataSchema to check
     * @return true if this is an object type with enum values
     */
    internal fun isObjectEnum(schema: SingleDataSchema): Boolean {
        val schemaType = schema.type.orElse(null) ?: return false
        if (schemaType != DataSchemaType.OBJECT) return false
        return schema.enum.isNotEmpty()
    }

    /**
     * Determines if a schema has a date-time format.
     *
     * @param schema The SingleDataSchema to check
     * @return true if this is a string type with format "date-time"
     */
    internal fun isDateTimeFormat(schema: SingleDataSchema): Boolean {
        val schemaType = schema.type.orElse(null) ?: return false
        if (schemaType != DataSchemaType.STRING) return false
        val format = schema.format.orElse(null) ?: return false
        return format.equals("date-time", ignoreCase = true)
    }
}
