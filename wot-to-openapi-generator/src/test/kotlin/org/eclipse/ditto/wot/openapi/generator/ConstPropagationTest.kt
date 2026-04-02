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
package org.eclipse.ditto.wot.openapi.generator

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.StringSchema
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.thing.AttributeSchemaResolver
import org.eclipse.ditto.wot.openapi.generator.thing.AttributesPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.features.FeaturesPathsGenerator
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConstPropagationTest {

    // --- Utils.convert() tests via asOpenApiSchema ---

    @Test
    fun `string const is propagated via Utils asOpenApiSchema`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "deviceType": {
                  "title": "Device Type",
                  "type": "string",
                  "const": "sensor"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("test", model, paths, api)

        val propertyPath = paths["/{thingId}/features/test/properties/deviceType"]
        assertNotNull(propertyPath)
        val schema = propertyPath.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertNotNull(schema)

        val resolved = resolveSchema(schema, api)
        assertEquals("sensor", resolved.const)
        assertNull(resolved.enum)
    }

    @Test
    fun `integer const is propagated via Utils asOpenApiSchema`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "version": {
                  "title": "Version",
                  "type": "integer",
                  "const": 42
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("test", model, paths, api)

        val propertyPath = paths["/{thingId}/features/test/properties/version"]
        assertNotNull(propertyPath)
        val schema = propertyPath.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertNotNull(schema)

        val resolved = resolveSchema(schema, api)
        assertEquals(42, resolved.const)
        assertNull(resolved.enum)
    }

    @Test
    fun `number const is propagated via Utils asOpenApiSchema`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "fixedRate": {
                  "title": "Fixed Rate",
                  "type": "number",
                  "const": 3.14
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("test", model, paths, api)

        val propertyPath = paths["/{thingId}/features/test/properties/fixedRate"]
        assertNotNull(propertyPath)
        val schema = propertyPath.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertNotNull(schema)

        val resolved = resolveSchema(schema, api)
        assertEquals(BigDecimal(3.14), resolved.const)
    }

    @Test
    fun `boolean const is propagated via Utils asOpenApiSchema`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "alwaysOn": {
                  "title": "Always On",
                  "type": "boolean",
                  "const": true
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("test", model, paths, api)

        val propertyPath = paths["/{thingId}/features/test/properties/alwaysOn"]
        assertNotNull(propertyPath)
        val schema = propertyPath.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertNotNull(schema)

        val resolved = resolveSchema(schema, api)
        assertEquals(true, resolved.const)
    }

    // --- AttributeSchemaResolver.injectSchemaOptions() tests ---

    @Test
    fun `injectSchemaOptions propagates string const`() {
        val json = JsonObject.of("""{"type": "string", "const": "8"}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, StringSchema())

        assertNotNull(schema)
        assertEquals("8", schema.const)
        assertNull(schema.enum)
    }

    @Test
    fun `injectSchemaOptions propagates integer const`() {
        val json = JsonObject.of("""{"type": "integer", "const": 42}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, IntegerSchema())

        assertNotNull(schema)
        assertEquals(42, schema.const)
        assertNull(schema.enum)
    }

    @Test
    fun `injectSchemaOptions propagates number const`() {
        val json = JsonObject.of("""{"type": "number", "const": 9.81}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, NumberSchema())

        assertNotNull(schema)
        assertEquals(BigDecimal(9.81), schema.const)
        assertNull(schema.enum)
    }

    @Test
    fun `injectSchemaOptions propagates boolean const`() {
        val json = JsonObject.of("""{"type": "boolean", "const": false}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, BooleanSchema())

        assertNotNull(schema)
        assertEquals(false, schema.const)
        assertNull(schema.enum)
    }

    @Test
    fun `string const coexists with existing enum`() {
        val json = JsonObject.of("""{"type": "string", "enum": ["a", "b"], "const": "a"}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, StringSchema())

        assertNotNull(schema)
        assertEquals("a", schema.const)
        assertEquals(listOf("a", "b"), schema.enum)
    }

    @Test
    fun `schema without const does not set const`() {
        val json = JsonObject.of("""{"type": "string"}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, StringSchema())

        assertNotNull(schema)
        assertNull(schema.const)
    }

    @Test
    fun `type-mismatched const is ignored for integer schema`() {
        val json = JsonObject.of("""{"type": "integer", "const": "not_a_number"}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, IntegerSchema())

        assertNotNull(schema)
        assertNull(schema.const)
    }

    @Test
    fun `type-mismatched const is ignored for string schema`() {
        val json = JsonObject.of("""{"type": "string", "const": 42}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, StringSchema())

        assertNotNull(schema)
        assertNull(schema.const)
    }

    @Test
    fun `type-mismatched const is ignored for boolean schema`() {
        val json = JsonObject.of("""{"type": "boolean", "const": "true"}""")
        val schema = AttributeSchemaResolver.injectSchemaOptions(json, BooleanSchema())

        assertNotNull(schema)
        assertNull(schema.const)
    }

    @Test
    fun `const-only property with no type returns null from getSchema`() {
        val json = JsonObject.of("""{"const": "sensor"}""")
        val schema = AttributeSchemaResolver.getSchema(json)

        assertNull(schema)
    }

    @Test
    fun `boolean false const is propagated via Utils asOpenApiSchema`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "disabled": {
                  "title": "Disabled",
                  "type": "boolean",
                  "const": false
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("test", model, paths, api)

        val propertyPath = paths["/{thingId}/features/test/properties/disabled"]
        assertNotNull(propertyPath)
        val schema = propertyPath.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertNotNull(schema)

        val resolved = resolveSchema(schema, api)
        assertEquals(false, resolved.const)
    }

    @Test
    fun `string const coexists with existing enum via Utils path`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "status": {
                  "title": "Status",
                  "type": "string",
                  "enum": ["active", "inactive"],
                  "const": "active"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("test", model, paths, api)

        val propertyPath = paths["/{thingId}/features/test/properties/status"]
        assertNotNull(propertyPath)
        val schema = propertyPath.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertNotNull(schema)

        val resolved = resolveSchema(schema, api)
        assertEquals("active", resolved.const)
        assertEquals(listOf("active", "inactive"), resolved.enum)
    }

    // --- Attribute-level end-to-end test ---

    @Test
    fun `thing attribute with string const is propagated end to end`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat",
              "properties": {
                "type": {
                  "title": "Type",
                  "type": "string",
                  "const": "8"
                }
              }
            }
            """.trimIndent()
        )

        val api = openApi()
        val attributesSchema = AttributeSchemaResolver.provideAttributesSchema(model, api)
        assertNotNull(attributesSchema)

        val typeSchema = api.components.schemas["attribute_type"]
        assertNotNull(typeSchema, "Expected attribute_type to be registered as component schema")
        assertEquals("8", typeSchema.const)
    }

    // --- Schema equivalence tests ---

    @Test
    fun `schemas with different const values get unique refs`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "deviceType": {
                  "title": "Device Type",
                  "type": "object",
                  "properties": {
                    "kind": { "type": "string", "const": "sensor" },
                    "category": { "type": "string", "const": "temperature" }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val api = openApi()
        AttributeSchemaResolver.provideAttributesSchema(model, api)

        val kindSchema = api.components.schemas["attribute_deviceType_kind"]
        val categorySchema = api.components.schemas["attribute_deviceType_category"]
        assertNotNull(kindSchema)
        assertNotNull(categorySchema)
        assertEquals("sensor", kindSchema.const)
        assertEquals("temperature", categorySchema.const)
    }

    // --- Nested object with const properties ---

    @Test
    fun `nested object properties with const are propagated`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "TestModel",
              "properties": {
                "config": {
                  "title": "Configuration",
                  "type": "object",
                  "properties": {
                    "protocol": { "type": "string", "const": "MQTT" },
                    "port": { "type": "integer", "const": 1883 }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val api = openApi()
        AttributeSchemaResolver.provideAttributesSchema(model, api)

        val protocolSchema = api.components.schemas["attribute_config_protocol"]
        assertNotNull(protocolSchema)
        assertEquals("MQTT", protocolSchema.const)

        val portSchema = api.components.schemas["attribute_config_port"]
        assertNotNull(portSchema)
        assertEquals(1883, portSchema.const)
    }

    // --- Real-world model tests (simulating resolved TMs) ---

    @Test
    fun `heat cost allocator model - string const type and integer const mbusType`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Heat cost allocator",
              "properties": {
                "type": {
                  "title": "Type",
                  "type": "string",
                  "const": "8"
                },
                "mbusType": {
                  "title": "Mbus Type",
                  "type": "integer",
                  "const": 8
                },
                "manufacturer": {
                  "title": "Manufacturer",
                  "type": "string",
                  "const": "QDS"
                }
              }
            }
            """.trimIndent()
        )

        val api = openApi()
        AttributeSchemaResolver.provideAttributesSchema(model, api)

        val typeSchema = api.components.schemas["attribute_type"]
        assertNotNull(typeSchema, "Expected attribute_type schema")
        assertEquals("8", typeSchema.const)

        val mbusTypeSchema = api.components.schemas["attribute_mbusType"]
        assertNotNull(mbusTypeSchema, "Expected attribute_mbusType schema")
        assertEquals(8, mbusTypeSchema.const)

        val manufacturerSchema = api.components.schemas["attribute_manufacturer"]
        assertNotNull(manufacturerSchema, "Expected attribute_manufacturer schema")
        assertEquals("QDS", manufacturerSchema.const)
    }

    @Test
    fun `smoke detector model - string const type and integer const mbusType`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "EIE - Mbus Smoke detector",
              "properties": {
                "type": {
                  "title": "Type",
                  "type": "string",
                  "const": "26"
                },
                "mbusType": {
                  "title": "Mbus Type",
                  "type": "integer",
                  "const": 26
                },
                "manufacturer": {
                  "title": "Manufacturer",
                  "type": "string",
                  "const": "EIE"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("smokeDetector", model, paths, api)

        val typeSchema = resolveSchema(
            paths["/{thingId}/features/smokeDetector/properties/type"]!!.get.responses["200"]!!.content["application/json"]!!.schema, api)
        assertEquals("26", typeSchema.const)

        val mbusTypeSchema = resolveSchema(
            paths["/{thingId}/features/smokeDetector/properties/mbusType"]!!.get.responses["200"]!!.content["application/json"]!!.schema, api)
        assertEquals(26, mbusTypeSchema.const)

        val mfgSchema = resolveSchema(
            paths["/{thingId}/features/smokeDetector/properties/manufacturer"]!!.get.responses["200"]!!.content["application/json"]!!.schema, api)
        assertEquals("EIE", mfgSchema.const)
    }

    // --- Helper functions ---

    private fun resolveSchema(schema: io.swagger.v3.oas.models.media.Schema<*>, api: OpenAPI): io.swagger.v3.oas.models.media.Schema<*> {
        val ref = schema.`$ref`
        if (ref != null) {
            val schemaName = ref.removePrefix("#/components/schemas/")
            return api.components?.schemas?.get(schemaName) ?: schema
        }
        return schema
    }

    private fun thingModelFromJson(json: String): ThingModel =
        ThingModel.fromJson(JsonObject.of(json))

    private fun openApi() = OpenAPI().components(Components().schemas(mutableMapOf()))
}
