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
package org.eclipse.ditto.wot.openapi.generator.thing

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.*
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.ThingModel
import java.math.BigDecimal
import kotlin.jvm.optionals.getOrNull

/**
 * Resolver for WoT Thing attribute schemas to OpenAPI schemas.
 * This object handles the conversion of WoT Thing properties to OpenAPI schema definitions,
 * including nested properties, enum values, and schema registration.
 */
object AttributeSchemaResolver {

    /**
     * Provides the complete attributes schema for a WoT Thing model.
     * Converts all properties from the Thing model into OpenAPI schema definitions.
     *
     * @param thingModel The WoT Thing model containing properties to convert
     * @param openAPI The OpenAPI instance to register schemas with
     * @return The OpenAPI schema representing all attributes, or null if no properties exist
     */
    fun provideAttributesSchema(thingModel: ThingModel, openAPI: OpenAPI): Schema<*>? {
        val mainSchema = ObjectSchema()
        val schemaProperties: MutableMap<String, Schema<*>> = mutableMapOf()
        thingModel.properties.getOrNull()?.entries?.sortedBy { it.key }?.forEach { prop ->
            val propertyName = prop.value.propertyName
            val schema = getSchema(prop.value.asObject()) ?: return@forEach

            schema.title = prop.value.title.getOrNull()?.toString()
            schema.description = prop.value.description.getOrNull()?.toString()

            schemaProperties[propertyName] = handleSchemaAndGetReference(schema, prop.value.asObject(), propertyName, openAPI, null)
        }

        return mainSchema
            .properties(schemaProperties)
    }

    /**
     * Creates an OpenAPI schema from a JSON object representing a WoT property.
     *
     * @param jsonObject The JSON object containing the property definition
     * @return The corresponding OpenAPI schema, or null if the type is not supported
     */
    fun getSchema(jsonObject: JsonObject): Schema<*>? {
        return when (jsonObject.getValue("type")?.getOrNull()?.asString()?.uppercase()) {
            DataSchemaType.BOOLEAN.name -> injectSchemaOptions(jsonObject, BooleanSchema())
            DataSchemaType.INTEGER.name -> injectSchemaOptions(jsonObject, IntegerSchema())
            DataSchemaType.NUMBER.name -> injectSchemaOptions(jsonObject, NumberSchema())
            DataSchemaType.STRING.name -> injectSchemaOptions(jsonObject, StringSchema())
            DataSchemaType.OBJECT.name -> injectSchemaOptions(jsonObject, ObjectSchema())
            DataSchemaType.ARRAY.name -> injectSchemaOptions(jsonObject, ArraySchema())
            else -> null
        }
    }

    /**
     * Injects schema options and metadata into an OpenAPI schema.
     * Handles readOnly, writeOnly, minimum, maximum, format, pattern, and enum values.
     *
     * @param jsonObject The JSON object containing schema options
     * @param schema The OpenAPI schema to inject options into
     * @return The schema with injected options, or null if injection failed
     */
    fun injectSchemaOptions(jsonObject: JsonObject, schema: Schema<*>): Schema<*>? {
        schema.readOnly = jsonObject.getValue("readOnly")?.getOrNull()?.asBoolean() ?: false
        schema.writeOnly = jsonObject.getValue("writeOnly")?.getOrNull()?.asBoolean() ?: false
        when (schema) {
            is IntegerSchema, is NumberSchema -> {
                jsonObject.getValue("minimum")?.getOrNull()?.let {
                    schema.minimum = when {
                        it.isNumber && schema is IntegerSchema -> BigDecimal(it.asInt())
                        it.isNumber && schema is NumberSchema -> BigDecimal(it.asDouble())
                        else -> null
                    }
                }
                jsonObject.getValue("maximum")?.getOrNull()?.let {
                    schema.maximum = when {
                        it.isNumber && schema is IntegerSchema -> BigDecimal(it.asInt())
                        it.isNumber && schema is NumberSchema -> BigDecimal(it.asDouble())
                        else -> null
                    }
                }

                jsonObject.getValue("enum")?.getOrNull()?.asArray()?.let { enumValues ->
                    if (enumValues.isEmpty.not()) {
                        when (schema) {
                            is IntegerSchema -> schema.enum = enumValues.map { it.asInt() }
                            is NumberSchema -> schema.enum = enumValues.map { BigDecimal(it.asDouble()) }
                        }
                    }
                }
            }

            is StringSchema -> {
                schema.format = jsonObject.getValue("format")?.getOrNull()?.asString()
                schema.pattern = jsonObject.getValue("pattern")?.getOrNull()?.asString()

                // Handle enums for string schemas
                jsonObject.getValue("enum")?.getOrNull()?.asArray()?.let { enumValues ->
                    if (enumValues.isEmpty.not()) {
                        schema.enum = enumValues.map { it.asString() }
                    }
                }
            }
        }
        return schema
    }

    /**
     * Creates sub-schema properties for nested object structures.
     *
     * @param jsonObject The JSON object containing nested properties
     * @param openAPI The OpenAPI instance to register schemas with
     * @param parentPath The parent path for schema naming
     * @return A map of property names to their corresponding OpenAPI schemas
     */
    fun createSubSchemaProperties(
        jsonObject: JsonObject?,
        openAPI: OpenAPI,
        parentPath: String?
    ): MutableMap<String, Schema<Any>> {
        val schemaProperties = mutableMapOf<String, Schema<Any>>()

        jsonObject?.forEach { prop ->
            val propertyName = prop.key.toString()
            val propertyObject = prop.value.asObject()
            val schema = getSchema(propertyObject) ?: return@forEach
            schema.title = propertyObject.getValue("title")?.getOrNull()?.asString()
            schema.description = propertyObject.getValue("description")?.getOrNull()?.asString()

            schemaProperties[propertyName] =
                handleSchemaAndGetReference(schema, propertyObject, propertyName, openAPI, parentPath)
        }

        return schemaProperties
    }

    private fun handleSchemaAndGetReference(
        schema: Schema<*>,
        propertyObject: JsonObject,
        propertyName: String,
        openAPI: OpenAPI,
        parentPath: String? = null
    ): Schema<Any> {
        val refName = registerSchemaAndGetRefName(schema, propertyName, openAPI, parentPath)
        val fullPath = if (parentPath.isNullOrEmpty()) propertyName else "${parentPath}_${propertyName}"

        when (schema) {
            is ObjectSchema -> {
                val properties = createSubSchemaProperties(
                    propertyObject.getValue("properties")?.getOrNull()?.asObject(),
                    openAPI,
                    fullPath
                )
                schema.properties(properties)

                val requiredProps = propertyObject.getValue("required")?.getOrNull()?.asArray()?.map { it.asString() }
                if (!requiredProps.isNullOrEmpty()) {
                    schema.required(requiredProps)
                }
            }
            is ArraySchema -> {
                val itemsObject = propertyObject.getValue("items")?.getOrNull()?.asObject()
                val itemSchema = getSchema(itemsObject ?: JsonObject.empty()) ?: schema as Schema<Any>
                itemSchema.title = itemsObject?.getValue("title")?.getOrNull()?.asString()
                itemSchema.description = itemsObject?.getValue("description")?.getOrNull()?.asString()

                if (itemSchema is ObjectSchema) {
                    val properties = createSubSchemaProperties(
                        itemsObject?.getValue("properties")?.getOrNull()?.asObject(),
                        openAPI,
                        fullPath
                    )
                    itemSchema.properties(properties)

                    val requiredProps = itemsObject?.getValue("required")?.getOrNull()?.asArray()?.map { it.asString() }
                    if (!requiredProps.isNullOrEmpty()) {
                        itemSchema.required(requiredProps)
                    }

                    val itemsRefName = registerSchemaAndGetRefName(itemSchema, "${propertyName}_item", openAPI, parentPath)
                    schema.items(Schema<Any>().apply { `$ref` = "#/components/schemas/$itemsRefName" })
                } else {
                    schema.items(itemSchema)
                }
            }
        }

        return Schema<Any>().apply { `$ref` = "#/components/schemas/$refName" }
    }

    private fun registerSchemaAndGetRefName(
        schema: Schema<*>,
        propertyName: String,
        openAPI: OpenAPI,
        parentPath: String? = null
    ): String {
        val refName = createRefName(propertyName, parentPath)

        var uniqueRefName = refName
        var counter = 1
        while (openAPI.components?.schemas?.containsKey(uniqueRefName) == true) {
            val existingSchema = openAPI.components?.schemas?.get(uniqueRefName)
            if (schemasAreEquivalent(existingSchema, schema)) {
                return uniqueRefName // Return existing name if schemas are equivalent
            }
            // Create a new unique name if schemas are different
            uniqueRefName = "${refName}_${counter++}"
        }

        openAPI.schema(uniqueRefName, schema)
        return uniqueRefName
    }

    /**
     * Creates a reference name for a schema based on property name and parent path.
     *
     * @param propertyName The name of the property
     * @param parentPath Optional parent path for nested properties
     * @return A unique reference name for the schema
     */
    fun createRefName(propertyName: String, parentPath: String? = null): String {
        return if (parentPath.isNullOrEmpty()) {
            "attribute_${propertyName.replace(" ", "_")}"
        } else {
            "attribute_${parentPath.replace(".", "_")}_${propertyName.replace(" ", "_")}"
        }
    }

    private fun schemasAreEquivalent(schema1: Schema<*>?, schema2: Schema<*>?): Boolean {
        if (schema1 == null || schema2 == null) return false

        if (schema1.type != schema2.type ||
            schema1.format != schema2.format ||
            schema1.description != schema2.description ||
            schema1.pattern != schema2.pattern ||
            schema1.readOnly != schema2.readOnly ||
            schema1.writeOnly != schema2.writeOnly) {
            return false
        }

        if (schema1 is ObjectSchema && schema2 is ObjectSchema) {
            val props1 = schema1.properties ?: emptyMap()
            val props2 = schema2.properties ?: emptyMap()
            if (props1.keys != props2.keys) return false

            return props1.all { (key, value) ->
                schemasAreEquivalent(value, props2[key])
            }
        }

        if (schema1 is ArraySchema && schema2 is ArraySchema) {
            return schemasAreEquivalent(schema1.items, schema2.items)
        }

        return true
    }
}