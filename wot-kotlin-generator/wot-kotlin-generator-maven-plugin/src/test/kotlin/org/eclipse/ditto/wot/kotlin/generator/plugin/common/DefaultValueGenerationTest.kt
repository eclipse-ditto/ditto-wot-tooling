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
package org.eclipse.ditto.wot.kotlin.generator.plugin.common

import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.kotlin.generator.plugin.ThingModelGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.model.ThingModel
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration tests for default value constant generation in generated Kotlin code.
 *
 * Tests the full code generation pipeline to verify that DEFAULT_* constants
 * are properly generated from WoT Thing Model schema default values and placed
 * on the correct companion objects.
 *
 * @since 1.1.0
 */
class DefaultValueGenerationTest {

    @TempDir
    lateinit var outputDir: Path

    private fun generateAndReadAttributes(propertiesJson: String, pkg: String): String = runBlocking {
        val thingModel = ThingModel.fromJson(
            JsonObject.of("""
                {
                  "@context": "https://www.w3.org/2022/wot/td/v1.1",
                  "@type": "tm:ThingModel",
                  "title": "Test",
                  "version": { "model": "1.0.0" },
                  "properties": { $propertiesJson }
                }
            """.trimIndent())
        )

        ThingModelGenerator.generate(
            thingModel,
            GeneratorConfiguration(
                thingModelUrl = "in-memory",
                outputPackage = pkg,
                outputDirectory = outputDir.toFile()
            )
        )

        val attributesFile = outputDir.resolve("${pkg.replace('.', '/')}/attributes/Attributes.kt")
        assertTrue(Files.exists(attributesFile), "Expected generated attributes file at: $attributesFile")
        Files.readString(attributesFile)
    }

    @Test
    fun `should generate const val for boolean default in companion object`() {
        val content = generateAndReadAttributes("""
            "isEnabled": {
              "title": "Is Enabled",
              "type": "boolean",
              "default": true
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.boolean")

        assertTrue(
            content.contains("const val DEFAULT_IS_ENABLED: Boolean = true"),
            "Expected DEFAULT_IS_ENABLED constant in companion object"
        )
    }

    @Test
    fun `should generate const val for boolean false default`() {
        val content = generateAndReadAttributes("""
            "isDisabled": {
              "title": "Is Disabled",
              "type": "boolean",
              "default": false
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.booleanfalse")

        assertTrue(
            content.contains("const val DEFAULT_IS_DISABLED: Boolean = false"),
            "Expected DEFAULT_IS_DISABLED constant for false value"
        )
    }

    @Test
    fun `should generate const val for integer default`() {
        val content = generateAndReadAttributes("""
            "maxRetries": {
              "title": "Max Retries",
              "type": "integer",
              "default": 5
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.integer")

        assertTrue(
            content.contains("const val DEFAULT_MAX_RETRIES: Long = 5"),
            "Expected DEFAULT_MAX_RETRIES constant"
        )
    }

    @Test
    fun `should generate const val for zero integer default`() {
        val content = generateAndReadAttributes("""
            "retryCount": {
              "title": "Retry Count",
              "type": "integer",
              "default": 0
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.zeroint")

        assertTrue(
            content.contains("const val DEFAULT_RETRY_COUNT: Long = 0"),
            "Expected DEFAULT_RETRY_COUNT constant for zero value"
        )
    }

    @Test
    fun `should generate const val for number default`() {
        val content = generateAndReadAttributes("""
            "temperature": {
              "title": "Temperature",
              "type": "number",
              "default": 22.5
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.number")

        assertTrue(
            content.contains("const val DEFAULT_TEMPERATURE: Double = 22.5"),
            "Expected DEFAULT_TEMPERATURE constant"
        )
    }

    @Test
    fun `should generate const val for string default`() {
        val content = generateAndReadAttributes("""
            "name": {
              "title": "Name",
              "type": "string",
              "default": "unknown"
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.string")

        assertTrue(
            Regex("""const val DEFAULT_NAME: String = "unknown"""").containsMatchIn(content),
            "Expected DEFAULT_NAME constant with string value"
        )
    }

    @Test
    fun `should generate val for datetime default with Instant parse`() {
        val content = generateAndReadAttributes("""
            "createdAt": {
              "title": "Created At",
              "type": "string",
              "format": "date-time",
              "default": "2024-01-01T00:00:00Z"
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.datetime")

        assertTrue(
            Regex("""val DEFAULT_CREATED_AT: Instant = Instant\.parse\("2024-01-01T00:00:00Z"\)""")
                .containsMatchIn(content),
            "Expected DEFAULT_CREATED_AT with Instant.parse initializer"
        )
        assertFalse(
            content.contains("const val DEFAULT_CREATED_AT"),
            "Instant default should not use const val"
        )
    }

    @Test
    fun `should generate val for string enum default`() {
        val content = generateAndReadAttributes("""
            "status": {
              "title": "Status",
              "type": "string",
              "enum": ["active", "inactive", "pending"],
              "default": "active"
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.stringenum")

        assertTrue(
            Regex("""val DEFAULT_STATUS: Status = Status\.ACTIVE""").containsMatchIn(content),
            "Expected DEFAULT_STATUS with enum constant reference"
        )
    }

    @Test
    fun `should generate val for object enum default with name field`() {
        val content = generateAndReadAttributes("""
            "lowBatteryBehavior": {
              "title": "Low Battery Behavior",
              "type": "object",
              "enum": [
                { "name": "shutdown", "description": "Shut down the device" },
                { "name": "lowPower", "description": "Enter low power mode" }
              ],
              "default": { "name": "shutdown" }
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.objectenum")

        assertTrue(
            Regex("""val DEFAULT_LOW_BATTERY_BEHAVIOR: LowBatteryBehavior = LowBatteryBehavior\.\w+""")
                .containsMatchIn(content),
            "Expected DEFAULT_LOW_BATTERY_BEHAVIOR with sealed class instance"
        )
    }

    @Test
    fun `should generate val for empty array default`() {
        val content = generateAndReadAttributes("""
            "tags": {
              "title": "Tags",
              "type": "array",
              "items": { "type": "string" },
              "default": []
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.emptyarray")

        assertTrue(
            Regex("""val DEFAULT_TAGS: List<\w+> = emptyList\(\)""").containsMatchIn(content),
            "Expected DEFAULT_TAGS with emptyList() initializer"
        )
    }

    @Test
    fun `should generate listOf for non-empty string array default`() {
        val content = generateAndReadAttributes("""
            "defaultTags": {
              "title": "Default Tags",
              "type": "array",
              "items": { "type": "string" },
              "default": ["tag1", "tag2"]
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.nonemptyarray")

        assertTrue(content.contains("DEFAULT_DEFAULT_TAGS"), "Non-empty string array default should generate DEFAULT_ constant")
        assertTrue(content.contains("listOf("), "Non-empty string array default should use listOf()")
    }

    @Test
    fun `should generate listOf with enum values for non-empty enum array default`() {
        val content = generateAndReadAttributes("""
            "enabledSeverities": {
              "title": "Enabled Severities",
              "type": "array",
              "items": {
                "type": "string",
                "enum": ["OK", "WARNING", "CRITICAL"]
              },
              "default": ["OK", "WARNING"]
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.enumarray")

        assertTrue(content.contains("DEFAULT_ENABLED_SEVERITIES"), "Enum array default should generate DEFAULT_ constant")
        assertTrue(content.contains("EnabledSeveritiesItem"), "Enum array default should reference the enum type")
        assertTrue(content.contains("listOf("), "Enum array default should use listOf()")
    }

    @Test
    fun `should skip object enum default without name field`() {
        val content = generateAndReadAttributes("""
            "behavior": {
              "title": "Behavior",
              "type": "object",
              "enum": [
                { "name": "option1" },
                { "name": "option2" }
              ],
              "default": { "value": "something" }
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.missingname")

        assertFalse(
            content.contains("DEFAULT_BEHAVIOR"),
            "Object enum default without 'name' field should be skipped"
        )
    }

    @Test
    fun `should convert camelCase property name to SCREAMING_SNAKE_CASE constant`() {
        val content = generateAndReadAttributes("""
            "maxRetryCount": {
              "title": "Max Retry Count",
              "type": "integer",
              "default": 3
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.camelcase")

        assertTrue(
            content.contains("DEFAULT_MAX_RETRY_COUNT"),
            "Expected property name converted to SCREAMING_SNAKE_CASE"
        )
    }

    @Test
    fun `should generate multiple default constants in same companion object`() {
        val content = generateAndReadAttributes("""
            "isEnabled": {
              "title": "Is Enabled",
              "type": "boolean",
              "default": true
            },
            "maxRetries": {
              "title": "Max Retries",
              "type": "integer",
              "default": 3
            },
            "temperature": {
              "title": "Temperature",
              "type": "number",
              "default": 22.5
            },
            "noDefault": {
              "title": "No Default",
              "type": "string"
            }
        """.trimIndent(), "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.multiple")

        assertTrue(content.contains("DEFAULT_IS_ENABLED"), "Expected DEFAULT_IS_ENABLED")
        assertTrue(content.contains("DEFAULT_MAX_RETRIES"), "Expected DEFAULT_MAX_RETRIES")
        assertTrue(content.contains("DEFAULT_TEMPERATURE"), "Expected DEFAULT_TEMPERATURE")
        assertFalse(content.contains("DEFAULT_NO_DEFAULT"), "Should not have constant for property without default")
    }
}
