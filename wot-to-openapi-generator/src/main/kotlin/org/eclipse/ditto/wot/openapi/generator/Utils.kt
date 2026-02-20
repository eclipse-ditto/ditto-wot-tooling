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
package org.eclipse.ditto.wot.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.*
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.model.Action
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.MultipleDataSchema
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.model.SingleDataSchema
import java.math.BigDecimal
import kotlin.jvm.optionals.getOrNull
import org.eclipse.ditto.wot.model.ArraySchema as WotArraySchema
import org.eclipse.ditto.wot.model.ObjectSchema as WotObjectSchema
import org.eclipse.ditto.wot.model.SingleDataSchema as WotSchema

/**
 * Utility functions for Web of Things (WoT) to OpenAPI schema conversion.
 * This object provides helper functions for schema transformation, naming conventions,
 * and type validation used throughout the OpenAPI generation process.
 */
object Utils {

    private const val DITTO_DEPRECATION_NOTICE = "ditto:deprecationNotice"
    private const val DEPRECATED = "deprecated"
    private const val SUPERSEDED_BY = "supersededBy"
    private const val REMOVAL_VERSION = "removalVersion"

    /**
     * Structured representation of the Ditto WoT deprecation notice extension.
     */
    data class DeprecationNotice(
        val deprecated: Boolean,
        val supersededBy: String? = null,
        val removalVersion: String? = null
    )

    /**
     * Converts a string to a valid Kotlin class name by applying proper naming conventions.
     *
     * @param name The input string to convert
     * @return A properly formatted class name with PascalCase
     */
    fun asClassName(name: String): String {
        return name.split(" ").map { allUppercaseToLowercase(it) }.map { splitByUppercase(it) }.flatten()
            .joinToString("") { it.lowercase().replaceFirstChar { it.titlecase() } }
    }

    /**
     * Converts a string to a valid Kotlin property name by applying proper naming conventions.
     * Handles special characters, hyphens, and ensures the result is a valid Kotlin identifier.
     *
     * @param name The input string to convert
     * @return A properly formatted property name with camelCase
     */
    fun asPropertyName(name: String): String {
        // First, replace hyphens with underscores
        val dehyphenated = name.replace("-", "_")
        // Then replace any remaining special characters with underscores
        val sanitized = dehyphenated.replace(Regex("[^a-zA-Z0-9_]"), "_")
        // Then apply the existing transformation
        val firstAttempt = asClassName(sanitized).replaceFirstChar { it.lowercase() }
        return if (isValidKotlinIdentifier(firstAttempt)) {
            firstAttempt
        } else {
            "_${firstAttempt}"
        }
    }

    /**
     * Validates if a string is a valid Kotlin identifier.
     * Checks against Kotlin keywords and naming conventions.
     *
     * @param name The string to validate
     * @return true if the string is a valid Kotlin identifier, false otherwise
     */
    fun isValidKotlinIdentifier(name: String): Boolean {
        val keywords = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
            "if", "in", "interface", "is", "null", "object", "package", "return", "super",
            "this", "throw", "true", "try", "typealias", "val", "var", "when", "while"
        )

        if (name.isEmpty()) {
            return false
        }

        if (!name[0].isLetter() && name[0] != '_') {  // Remove hyphen check since we're converting them
            return false
        }

        for (char in name) {
            if (!(char.isLetterOrDigit() || char == '_')) {  // Remove hyphen from valid characters
                return false
            }
        }

        return !keywords.contains(name)
    }

    /**
     * Converts all-uppercase strings to lowercase while preserving mixed-case strings.
     *
     * @param it The input string
     * @return The processed string
     */
    fun allUppercaseToLowercase(it: String): String {
        return if (it.length > 1 && it == it.uppercase()) {
            it.lowercase()
        } else {
            it
        }
    }

    /**
     * Splits a string by uppercase letters to separate camelCase words.
     *
     * @param input The input string to split
     * @return List of split strings
     */
    fun splitByUppercase(input: String): List<String> {
        val regex = Regex("(?=[A-Z])")
        return input.split(regex)
    }

    /**
     * Determines if a WoT data schema type is primitive.
     *
     * @param schemaType The WoT data schema type to check
     * @return true if the type is primitive (boolean, integer, number, string), false otherwise
     */
    fun isPrimitive(schemaType: DataSchemaType?): Boolean {
        return when (schemaType) {
            DataSchemaType.OBJECT -> false
            DataSchemaType.BOOLEAN -> true
            DataSchemaType.INTEGER -> true
            DataSchemaType.NUMBER -> true
            DataSchemaType.STRING -> true
            DataSchemaType.ARRAY -> false
            DataSchemaType.NULL -> false
            null -> false
        }
    }

    /**
     * Checks if a WoT object schema has additional properties defined.
     *
     * @return true if additional properties are defined, false otherwise
     */
    fun WotObjectSchema.isAdditionalPropertiesSchema() =
        this.toJson().getValue(JsonPointer.of("additionalProperties")).isEmpty.not()

    /**
     * Checks if a WoT object schema has pattern properties defined.
     *
     * @return true if pattern properties are defined, false otherwise
     */
    fun WotObjectSchema.isPatternPropertiesSchema() =
        this.toJson().getValue(JsonPointer.of("patternProperties")).isEmpty.not()

    /**
     * Converts a WoT single data schema to an OpenAPI schema.
     *
     * @param wotSchema The WoT schema to convert
     * @param featureName Optional feature name for schema naming
     * @param propertyOrAction Optional property or action identifier for schema naming
     * @param openAPI Optional OpenAPI instance for schema registration
     * @return The converted OpenAPI schema, or null if conversion is not possible
     */
    fun asOpenApiSchema(
        wotSchema: SingleDataSchema,
        featureName: String? = null,
        propertyOrAction: String? = null,
        openAPI: OpenAPI? = null
    ): Schema<*>? {
        val wotSchemaType = wotSchema.type.getOrNull() ?: error("SingleDataSchema 'type' is not defined: $wotSchema")
        val propertyOrFieldSchema = when (wotSchemaType) {
            DataSchemaType.BOOLEAN -> convert(wotSchema, BooleanSchema())
            DataSchemaType.INTEGER -> convert(wotSchema, IntegerSchema())
            DataSchemaType.NUMBER -> convert(wotSchema, NumberSchema())
            DataSchemaType.STRING -> convert(wotSchema, StringSchema())
            DataSchemaType.OBJECT -> convert(wotSchema, ObjectSchema(), featureName, propertyOrAction, openAPI)
            DataSchemaType.ARRAY -> convert(wotSchema, ArraySchema(), featureName, propertyOrAction, openAPI)
            DataSchemaType.NULL -> null // Handle NULL type
        }

        if (wotSchemaType != DataSchemaType.OBJECT && wotSchemaType != DataSchemaType.ARRAY) {
            propertyOrFieldSchema?.title(wotSchema.title.getOrNull()?.toString())
                ?.description(wotSchema.description.getOrNull()?.toString())
        }
        return propertyOrFieldSchema
    }

    /**
     * Converts a WoT schema to an OpenAPI schema with proper type handling and metadata.
     *
     * @param wotSchema The WoT schema to convert
     * @param schema The target OpenAPI schema instance
     * @param featureName Optional feature name for schema naming
     * @param propertyOrAction Optional property or action identifier for schema naming
     * @param openAPI Optional OpenAPI instance for schema registration
     * @return The converted OpenAPI schema
     */
    fun convert(
        wotSchema: WotSchema,
        schema: Schema<*>,
        featureName: String? = null,
        propertyOrAction: String? = null,
        openAPI: OpenAPI? = null
    ): Schema<*> {
        schema.readOnly(wotSchema.isReadOnly).writeOnly(wotSchema.isWriteOnly)
        when (schema) {
            is IntegerSchema, is NumberSchema -> {
                wotSchema.toJson().getValue("minimum")?.getOrNull()?.let {
                    schema.minimum = when {
                        it.isNumber && schema is IntegerSchema -> BigDecimal(it.asInt())
                        it.isNumber && schema is NumberSchema -> BigDecimal(it.asDouble())
                        else -> null
                    }
                }
                wotSchema.toJson().getValue("maximum")?.getOrNull()?.let {
                    schema.maximum = when {
                        it.isNumber && schema is IntegerSchema -> BigDecimal(it.asInt())
                        it.isNumber && schema is NumberSchema -> BigDecimal(it.asDouble())
                        else -> null
                    }
                }
            }

            is StringSchema -> {
                val enumValues = wotSchema.toJson().getValue("enum")?.getOrNull()?.asArray()
                if (enumValues != null && enumValues.isEmpty.not()) {
                    schema.enum = enumValues.map { it.asString() }
                }
                schema.format = wotSchema.toJson().getValue("format")?.getOrNull()?.asString()
                schema.pattern = wotSchema.toJson().getValue("pattern")?.getOrNull()?.asString()
            }

        }
        return createComplexTypeStructureIfNeeded(wotSchema, schema, featureName, propertyOrAction, openAPI)
    }

    private fun createComplexTypeStructureIfNeeded(
        wotSchema: SingleDataSchema,
        schema: Schema<*>,
        featureName: String?,
        propertyOrAction: String?,
        openAPI: OpenAPI?
    ): Schema<*> {
        val schemaType = wotSchema.type.getOrNull() ?: return schema
        if (schemaType != DataSchemaType.OBJECT && schemaType != DataSchemaType.ARRAY) {
            return schema
        }

        when (schemaType) {
            DataSchemaType.OBJECT -> {
                createObjectTypeStructure(
                    if (wotSchema is Property) wotSchema.asObjectSchema() else wotSchema as WotObjectSchema,
                    schema as ObjectSchema,
                    propertyOrAction,
                    openAPI
                )
            }
            DataSchemaType.ARRAY -> {
                createArrayTypeStructure(
                    if (wotSchema is Property) wotSchema.asArraySchema() else wotSchema as WotArraySchema,
                    schema as ArraySchema,
                    openAPI
                )
            }
            else -> return schema
        }

        val name = when (wotSchema) {
            is Property -> wotSchema.propertyName
            else -> wotSchema.title.getOrNull()?.toString()
        } ?: return schema

        val schemaRefName = when {
            featureName != null -> "feature_${asPropertyName(featureName)}_${propertyOrAction}_${asPropertyName(name)}"
            propertyOrAction != null -> "top_${propertyOrAction}_${asPropertyName(name)}"
            else -> return schema
        }

        if (openAPI!!.components?.schemas?.containsKey(schemaRefName) == true) {
            return Schema<Any>().`$ref`("#/components/schemas/$schemaRefName")
        }

        openAPI.schema(schemaRefName, schema)
        return Schema<Any>().`$ref`("#/components/schemas/$schemaRefName")
    }

    /**
     * Creates an OpenAPI array schema structure from a WoT array schema.
     *
     * @param wotArraySchema The WoT array schema to convert
     * @param openApiArraySchema The target OpenAPI array schema
     * @param openAPI Optional OpenAPI instance for schema registration
     * @return The configured OpenAPI array schema
     */
    fun createArrayTypeStructure(
        wotArraySchema: org.eclipse.ditto.wot.model.ArraySchema,
        openApiArraySchema: ArraySchema,
        openAPI: OpenAPI?
    ): Schema<*> {
        openApiArraySchema.title = wotArraySchema.title.getOrNull()?.toString()
        openApiArraySchema.description = wotArraySchema.description.getOrNull()?.toString()
        return when (wotArraySchema.items.get()) {
            is SingleDataSchema -> openApiArraySchema.items(
                asOpenApiSchema(
                    wotArraySchema.items.get() as SingleDataSchema,
                    null,
                    "property",
                    openAPI
                )
            )
            // Is this the case?
            is MultipleDataSchema -> throw IllegalArgumentException("MultipleDataSchema is not supported")
            else -> throw IllegalArgumentException("Should never happen as all other types are single data schema")
        }
    }

    private fun createObjectTypeStructure(
        wotObjectSchema: WotObjectSchema,
        openApiObjectSchema: ObjectSchema,
        propertyOrAction: String? = null,
        openAPI: OpenAPI?
    ) {
        openApiObjectSchema.title = wotObjectSchema.title.getOrNull()?.toString()
        openApiObjectSchema.description = wotObjectSchema.description.getOrNull()?.toString()
        wotObjectSchema.properties.forEach { (name, property) ->
            val parentTitle = wotObjectSchema.title.getOrNull()?.toString()
            val contextualPropertyOrAction = if (parentTitle != null && parentTitle.isNotBlank()) {
                "property_${asPropertyName(parentTitle)}"
            } else {
                "property"
            }
            openApiObjectSchema.addProperty(name,
                                            asOpenApiSchema(property, null,
                                                                                                        contextualPropertyOrAction,
                                                                                                        openAPI))
        }
        if (wotObjectSchema.isAdditionalPropertiesSchema()) {
            wotObjectSchema.toJson()
                .getValue(JsonPointer.of("additionalProperties"))
                .getOrNull()?.takeIf { it.isObject }?.asObject()?.let { additionalProperties ->
                    val openApiSchema = asOpenApiSchema(
                        WotObjectSchema.fromJson(additionalProperties),
                        null,
                        propertyOrAction,
                        openAPI
                    )
                    openApiObjectSchema.additionalProperties(openApiSchema)
                }
        } else if (wotObjectSchema.isPatternPropertiesSchema()) {
            wotObjectSchema.toJson()
                .getValue(JsonPointer.of("patternProperties"))
                .getOrNull()?.takeIf { it.isObject }?.asObject()?.let { patternProperties ->
                    patternProperties.keys.firstOrNull()?.let {
                        val patternProperty = patternProperties.getField(it).getOrNull()
                        if (patternProperty?.value is JsonObject) {
                            val patternOpenApiSchema = asOpenApiSchema(
                                WotObjectSchema.fromJson(patternProperty.value.asObject()),
                                null,
                                propertyOrAction,
                                openAPI
                            )
                            openApiObjectSchema.patternProperties(mapOf(it.toString() to patternOpenApiSchema))
                        } else {
                            null
                        }
                    }
                }
        } else null
    }

    /**
     * Extracts the Ditto category from a WoT property.
     *
     * @param property The WoT property to extract the category from
     * @return The category string if found, null otherwise
     */
    fun extractPropertyCategory(property: Property): String? {
        return property.toJson().getValue(JsonPointer.of("ditto:category"))
            .getOrNull()?.asString()
    }

    /**
     * Extracts the Ditto deprecation notice from a WoT property.
     */
    fun extractDeprecationNotice(property: Property): DeprecationNotice? {
        return extractDeprecationNotice(property.toJson())
    }

    /**
     * Extracts the Ditto deprecation notice from a WoT action.
     */
    fun extractDeprecationNotice(action: Action): DeprecationNotice? {
        return extractDeprecationNotice(action.toJson())
    }

    /**
     * Builds a human-readable Markdown deprecation notice string from a [DeprecationNotice].
     * Returns null if the notice is null or not deprecated.
     */
    fun buildDeprecationDescription(notice: DeprecationNotice?): String? {
        if (notice == null || !notice.deprecated) return null
        val parts = mutableListOf("**Deprecated.**")
        notice.supersededBy?.let { parts.add("Superseded by `$it`.") }
        notice.removalVersion?.let { parts.add("Will be removed in version $it.") }
        return parts.joinToString(" ")
    }

    /**
     * Merges an existing description with a deprecation notice suffix.
     * If the notice is null or not deprecated, the original description is returned unchanged.
     */
    fun mergeWithDeprecationNotice(existingDescription: String?, notice: DeprecationNotice?): String? {
        val deprecationText = buildDeprecationDescription(notice) ?: return existingDescription
        return if (existingDescription.isNullOrBlank()) deprecationText
        else "$existingDescription\n\n$deprecationText"
    }

    /**
     * Marks an OpenAPI schema as deprecated.
     * For `$ref` schemas, looks up the referenced component schema and marks it.
     * For inline schemas, marks the schema object directly.
     */
    fun markSchemaDeprecated(schema: Schema<*>?, openAPI: OpenAPI) {
        if (schema == null) return
        val ref = schema.`$ref`
        if (ref != null) {
            val schemaName = ref.removePrefix("#/components/schemas/")
            openAPI.components?.schemas?.get(schemaName)?.deprecated(true)
        } else {
            schema.deprecated(true)
        }
    }

    /**
     * Extracts the Ditto deprecation notice from a generic interaction JSON object.
     * The notice is considered valid only if `deprecated` is present and boolean.
     */
    fun extractDeprecationNotice(interactionJson: JsonObject): DeprecationNotice? {
        val deprecationNotice = interactionJson.getValue(JsonPointer.of(DITTO_DEPRECATION_NOTICE))
            .getOrNull()
            ?.takeIf(JsonValue::isObject)
            ?.asObject()
            ?: return null

        val deprecatedValue = deprecationNotice.getValue(JsonPointer.of(DEPRECATED))
            .getOrNull()
            ?: return null

        if (!deprecatedValue.isBoolean) {
            return null
        }

        val supersededBy = deprecationNotice.getValue(JsonPointer.of(SUPERSEDED_BY))
            .getOrNull()
            ?.takeIf(JsonValue::isString)
            ?.asString()

        val removalVersion = deprecationNotice.getValue(JsonPointer.of(REMOVAL_VERSION))
            .getOrNull()
            ?.takeIf(JsonValue::isString)
            ?.asString()

        return DeprecationNotice(
            deprecated = deprecatedValue.asBoolean(),
            supersededBy = supersededBy,
            removalVersion = removalVersion
        )
    }
}
