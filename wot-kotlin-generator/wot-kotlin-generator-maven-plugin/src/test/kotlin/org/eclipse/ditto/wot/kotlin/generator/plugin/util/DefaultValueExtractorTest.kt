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
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import org.eclipse.ditto.json.JsonArray
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultValueExtractor].
 *
 * Tests the extraction of `"default"` values from WoT Thing Model JSON-LD schemas
 * and generation of corresponding `DEFAULT_*` companion object constants.
 *
 * @since 1.1.0
 */
class DefaultValueExtractorTest {

    companion object {
        private const val TEST_PACKAGE = "org.example.generated"
    }

    @Test
    fun `should generate const val for boolean true default`() {
        val constantName = "IS_ENABLED"
        val value = true

        val result = DefaultValueExtractor.createBooleanDefault(constantName, value)

        assertEquals("DEFAULT_IS_ENABLED", result.name)
        assertEquals(BOOLEAN, result.type)
        assertTrue(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("true"))
    }

    @Test
    fun `should generate const val for boolean false default`() {
        val constantName = "IS_DISABLED"
        val value = false

        val result = DefaultValueExtractor.createBooleanDefault(constantName, value)

        assertEquals("DEFAULT_IS_DISABLED", result.name)
        assertEquals(BOOLEAN, result.type)
        assertTrue(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("false"))
    }

    @Test
    fun `should generate const val for integer default`() {
        val constantName = "MAX_RETRIES"
        val value = 3L

        val result = DefaultValueExtractor.createIntegerDefault(constantName, value)

        assertEquals("DEFAULT_MAX_RETRIES", result.name)
        assertEquals(LONG, result.type)
        assertTrue(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("3"))
    }

    @Test
    fun `should generate const val for integer zero default`() {
        val constantName = "RETRY_COUNT"
        val value = 0L

        val result = DefaultValueExtractor.createIntegerDefault(constantName, value)

        assertEquals("DEFAULT_RETRY_COUNT", result.name)
        assertEquals(LONG, result.type)
        assertTrue(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("0"))
    }

    @Test
    fun `should generate const val for negative integer default`() {
        val constantName = "MIN_VALUE"
        val value = -100L

        val result = DefaultValueExtractor.createIntegerDefault(constantName, value)

        assertEquals("DEFAULT_MIN_VALUE", result.name)
        assertTrue(result.initializer.toString().contains("-100"))
    }

    @Test
    fun `should generate const val for number default`() {
        val constantName = "TEMPERATURE"
        val value = 25.5

        val result = DefaultValueExtractor.createNumberDefault(constantName, value)

        assertEquals("DEFAULT_TEMPERATURE", result.name)
        assertEquals(DOUBLE, result.type)
        assertTrue(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("25.5"))
    }

    @Test
    fun `should generate const val for number zero default`() {
        val constantName = "OFFSET"
        val value = 0.0

        val result = DefaultValueExtractor.createNumberDefault(constantName, value)

        assertEquals("DEFAULT_OFFSET", result.name)
        assertTrue(result.initializer.toString().contains("0.0"))
    }

    @Test
    fun `should generate const val for string default`() {
        val constantName = "DEFAULT_NAME"
        val value = "unknown"

        val result = DefaultValueExtractor.createStringDefault(constantName, value)

        assertEquals("DEFAULT_DEFAULT_NAME", result.name)
        assertEquals(STRING, result.type)
        assertTrue(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("unknown"))
    }

    @Test
    fun `should generate const val for empty string default`() {
        val constantName = "LABEL"
        val value = ""

        val result = DefaultValueExtractor.createStringDefault(constantName, value)

        assertEquals("DEFAULT_LABEL", result.name)
        assertEquals(STRING, result.type)
    }

    @Test
    fun `should generate val for instant default`() {
        val constantName = "CREATED_AT"
        val value = "2024-01-01T00:00:00Z"

        val result = DefaultValueExtractor.createInstantDefault(constantName, value)

        assertEquals("DEFAULT_CREATED_AT", result.name)
        assertEquals(ClassName("java.time", "Instant"), result.type)
        // Instant is a reference type, so no const modifier
        assertFalse(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("Instant.parse"))
        assertTrue(result.initializer.toString().contains("2024-01-01T00:00:00Z"))
    }

    @Test
    fun `should generate val for enum default`() {
        val constantName = "STATUS"
        val enumTypeName = ClassName("", "Status")
        val enumConstantValue = "active"
        val enumName = "Status"

        val result = DefaultValueExtractor.createEnumDefault(constantName, enumTypeName, enumConstantValue, enumName)

        assertEquals("DEFAULT_STATUS", result.name)
        assertEquals(ClassName("", "Status"), result.type)
        // Enum is a reference type, so no const modifier
        assertFalse(result.modifiers.contains(KModifier.CONST))
        // Should use asEnumConstantName() to convert the value
        assertTrue(result.initializer.toString().contains("Status.ACTIVE"))
    }

    @Test
    fun `should generate val for enum default with special characters`() {
        val constantName = "MODE"
        val enumTypeName = ClassName("", "Mode")
        val enumConstantValue = "low-power"
        val enumName = "Mode"

        val result = DefaultValueExtractor.createEnumDefault(constantName, enumTypeName, enumConstantValue, enumName)

        assertEquals("DEFAULT_MODE", result.name)
        // asEnumConstantName should handle the hyphen
        assertTrue(result.initializer.toString().contains("Mode."))
    }

    @Test
    fun `should generate val for object enum default`() {
        val constantName = "BEHAVIOR"
        val sealedTypeName = ClassName("", "LowBatteryBehavior")
        val objectInstanceName = "SHUTDOWN"

        val result = DefaultValueExtractor.createObjectEnumDefault(constantName, sealedTypeName, objectInstanceName)

        assertEquals("DEFAULT_BEHAVIOR", result.name)
        assertEquals(ClassName("", "LowBatteryBehavior"), result.type)
        assertFalse(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("LowBatteryBehavior.SHUTDOWN"))
    }

    @Test
    fun `should generate val for empty list default`() {
        val constantName = "TAGS"
        val elementType = STRING

        val result = DefaultValueExtractor.createEmptyListDefault(constantName, elementType)

        assertEquals("DEFAULT_TAGS", result.name)
        assertEquals(LIST.parameterizedBy(STRING), result.type)
        assertFalse(result.modifiers.contains(KModifier.CONST))
        assertTrue(result.initializer.toString().contains("emptyList"))
    }

    @Test
    fun `should generate val for empty list with object type`() {
        val constantName = "ITEMS"
        val elementType = ClassName("org.example", "Item")

        val result = DefaultValueExtractor.createEmptyListDefault(constantName, elementType)

        assertEquals("DEFAULT_ITEMS", result.name)
        assertEquals(LIST.parameterizedBy(ClassName("org.example", "Item")), result.type)
    }

    @Test
    fun `should detect string enum schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string",
                "enum": ["active", "inactive"]
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.isStringEnum(schema)

        assertTrue(result)
    }

    @Test
    fun `should not detect non-enum string schema as string enum`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.isStringEnum(schema)

        assertFalse(result)
    }

    @Test
    fun `should detect object enum schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "object",
                "enum": [{"name": "Option1"}, {"name": "Option2"}]
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.isObjectEnum(schema)

        assertTrue(result)
    }

    @Test
    fun `should detect date-time format schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string",
                "format": "date-time"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.isDateTimeFormat(schema)

        assertTrue(result)
    }

    @Test
    fun `should not detect non-datetime string as date-time format`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.isDateTimeFormat(schema)

        assertFalse(result)
    }

    @Test
    fun `should extract boolean default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "boolean",
                "default": true
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("isEnabled", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_IS_ENABLED", result!!.name)
        assertTrue(result.modifiers.contains(KModifier.CONST))
    }

    @Test
    fun `should extract integer default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "integer",
                "default": 5
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("maxRetries", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_MAX_RETRIES", result!!.name)
        assertTrue(result.modifiers.contains(KModifier.CONST))
    }

    @Test
    fun `should extract number default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "number",
                "default": 22.5
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("temperature", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_TEMPERATURE", result!!.name)
        assertTrue(result.modifiers.contains(KModifier.CONST))
    }

    @Test
    fun `should extract string default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string",
                "default": "unknown"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("status", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_STATUS", result!!.name)
        assertTrue(result.modifiers.contains(KModifier.CONST))
    }

    @Test
    fun `should extract datetime default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string",
                "format": "date-time",
                "default": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("createdAt", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_CREATED_AT", result!!.name)
        assertFalse(result.modifiers.contains(KModifier.CONST)) // Instant is reference type
    }

    @Test
    fun `should extract enum default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string",
                "enum": ["active", "inactive", "pending"],
                "default": "active"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("state", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_STATE", result!!.name)
        assertFalse(result.modifiers.contains(KModifier.CONST)) // Enum is reference type
    }

    @Test
    fun `should extract empty array default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "array",
                "items": { "type": "string" },
                "default": []
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("tags", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_TAGS", result!!.name)
        assertFalse(result.modifiers.contains(KModifier.CONST)) // List is reference type
    }

    @Test
    fun `should return null for schema without default`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("status", schema, TEST_PACKAGE)

        assertNull(result)
    }

    @Test
    fun `should generate listOf for non-empty string array default`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "array",
                "items": { "type": "string" },
                "default": ["tag1", "tag2"]
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("tags", schema, TEST_PACKAGE)

        assertNotNull(result)
        val spec = result!!
        assertFalse(spec.modifiers.contains(KModifier.CONST))
        assertEquals("DEFAULT_TAGS", spec.name)
        assertTrue(spec.initializer.toString().contains("listOf"))
        assertTrue(spec.initializer.toString().contains("tag1"))
        assertTrue(spec.initializer.toString().contains("tag2"))
    }

    @Test
    fun `should skip nested object default silently`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "object",
                "properties": {
                    "name": { "type": "string" }
                },
                "default": { "name": "test" }
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("config", schema, TEST_PACKAGE)

        assertNull(result)
    }

    @Test
    fun `should skip object enum default missing name field`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "object",
                "enum": [{"name": "Option1"}, {"name": "Option2"}],
                "default": { "value": "something" }
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("behavior", schema, TEST_PACKAGE)

        assertNull(result)
    }

    @Test
    fun `should extract object enum default with name field`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "object",
                "enum": [{"name": "Shutdown"}, {"name": "LowPower"}],
                "default": { "name": "Shutdown" }
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("lowBatteryBehavior", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_LOW_BATTERY_BEHAVIOR", result!!.name)
        assertFalse(result.modifiers.contains(KModifier.CONST))
    }

    @Test
    fun `should extract default from schema with boolean type via extractDefaultFromSchema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "boolean",
                "default": false
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("isEnabled", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_IS_ENABLED", result!!.name)
        assertTrue(result.modifiers.contains(KModifier.CONST))
    }

    @Test
    fun `should extract default from schema with string type via extractDefaultFromSchema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "string",
                "default": "default_value"
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("name", schema, TEST_PACKAGE)

        assertNotNull(result)
        assertEquals("DEFAULT_NAME", result!!.name)
    }

    @Test
    fun `should extract multiple default constants from schema fields`() {
        val fields = mapOf(
            "isEnabled" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "boolean",
                    "default": true
                }
            """.trimIndent())),
            "maxRetries" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "integer",
                    "default": 3
                }
            """.trimIndent())),
            "name" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "string"
                }
            """.trimIndent())) // No default
        )

        val result = DefaultValueExtractor.extractDefaultConstants(fields, TEST_PACKAGE)

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "DEFAULT_IS_ENABLED" })
        assertTrue(result.any { it.name == "DEFAULT_MAX_RETRIES" })
    }

    @Test
    fun `should return empty list when no fields have defaults`() {
        val fields = mapOf(
            "name" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "string"
                }
            """.trimIndent())),
            "count" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "integer"
                }
            """.trimIndent()))
        )

        val result = DefaultValueExtractor.extractDefaultConstants(fields, TEST_PACKAGE)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return empty list for empty fields map`() {
        val fields = emptyMap<String, SingleDataSchema>()

        val result = DefaultValueExtractor.extractDefaultConstants(fields, TEST_PACKAGE)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should extract default constants from number schema fields`() {
        val fields = mapOf(
            "temperature" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "number",
                    "default": 22.5
                }
            """.trimIndent())),
            "humidity" to SingleDataSchema.fromJson(JsonObject.of("""
                {
                    "type": "number",
                    "default": 50.0
                }
            """.trimIndent()))
        )

        val result = DefaultValueExtractor.extractDefaultConstants(fields, TEST_PACKAGE)

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "DEFAULT_TEMPERATURE" })
        assertTrue(result.any { it.name == "DEFAULT_HUMIDITY" })
    }

    @Test
    fun `should use unqualified ClassName for inline enum types`() {
        val constantName = "STATUS"
        val enumTypeName = ClassName("", "Status") // Unqualified - empty package
        val enumConstantValue = "ACTIVE"
        val enumName = "Status"

        val result = DefaultValueExtractor.createEnumDefault(constantName, enumTypeName, enumConstantValue, enumName)

        assertEquals(ClassName("", "Status"), result.type)
        // The generated code should work with both inline and standalone enums
    }

    @Test
    fun `should create listOf for boolean array`() {
        val array = JsonArray.of(true, false)

        val result = DefaultValueExtractor.createListDefault("FLAGS", BOOLEAN, array, null)

        assertNotNull(result)
        assertEquals("DEFAULT_FLAGS", result!!.name)
        assertEquals(LIST.parameterizedBy(BOOLEAN), result.type)
        assertTrue(result.initializer.toString().contains("listOf"))
        assertTrue(result.initializer.toString().contains("true"))
        assertTrue(result.initializer.toString().contains("false"))
    }

    @Test
    fun `should create listOf for integer array`() {
        val array = JsonArray.of(1, 2, 3)

        val result = DefaultValueExtractor.createListDefault("COUNTS", LONG, array, null)

        assertNotNull(result)
        assertEquals("DEFAULT_COUNTS", result!!.name)
        assertEquals(LIST.parameterizedBy(LONG), result.type)
        assertTrue(result.initializer.toString().contains("listOf"))
    }

    @Test
    fun `should create listOf for double array`() {
        val array = JsonArray.of(1.5, 2.0)

        val result = DefaultValueExtractor.createListDefault("VALUES", DOUBLE, array, null)

        assertNotNull(result)
        assertEquals("DEFAULT_VALUES", result!!.name)
        assertEquals(LIST.parameterizedBy(DOUBLE), result.type)
        assertTrue(result.initializer.toString().contains("listOf"))
        assertTrue(result.initializer.toString().contains("1.5"))
    }

    @Test
    fun `should create listOf for string array`() {
        val array = JsonArray.of("a", "b")

        val result = DefaultValueExtractor.createListDefault("TAGS", STRING, array, null)

        assertNotNull(result)
        assertEquals("DEFAULT_TAGS", result!!.name)
        assertEquals(LIST.parameterizedBy(STRING), result.type)
        assertTrue(result.initializer.toString().contains("listOf"))
    }

    @Test
    fun `should create listOf for enum array`() {
        val array = JsonArray.of("OK", "WARNING")
        val enumType = ClassName("", "SeverityItem")
        val enumValues = listOf("OK", "WARNING", "CRITICAL")

        val result = DefaultValueExtractor.createListDefault("SEVERITIES", enumType, array, enumValues)

        assertNotNull(result)
        assertEquals("DEFAULT_SEVERITIES", result!!.name)
        assertEquals(LIST.parameterizedBy(enumType), result.type)
        assertTrue(result.initializer.toString().contains("SeverityItem"))
    }

    @Test
    fun `should return null for unsupported element type in array`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "array",
                "items": { "type": "object" },
                "default": [{"key": "value"}]
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("items", schema, TEST_PACKAGE)

        assertNull(result)
    }

    @Test
    fun `should extract enum array default from schema`() {
        val schema = SingleDataSchema.fromJson(JsonObject.of("""
            {
                "type": "array",
                "items": {
                    "type": "string",
                    "enum": ["OK", "WARNING", "CRITICAL"]
                },
                "default": ["OK", "WARNING"]
            }
        """.trimIndent()))

        val result = DefaultValueExtractor.extractDefaultFromSchema("enabledSeverities", schema, TEST_PACKAGE)

        assertNotNull(result)
        val spec = result!!
        assertEquals("DEFAULT_ENABLED_SEVERITIES", spec.name)
        assertFalse(spec.modifiers.contains(KModifier.CONST))
        assertTrue(spec.initializer.toString().contains("listOf"))
        assertTrue(spec.initializer.toString().contains("EnabledSeveritiesItem"))
    }
}
