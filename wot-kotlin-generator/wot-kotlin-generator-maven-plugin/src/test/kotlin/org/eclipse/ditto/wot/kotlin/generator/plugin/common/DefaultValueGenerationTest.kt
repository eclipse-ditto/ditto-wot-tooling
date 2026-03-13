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
package org.eclipse.ditto.wot.kotlin.generator.plugin.common

import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.kotlin.generator.plugin.ThingModelGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.model.ThingModel
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
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
 * @since 1.0.0
 */
class DefaultValueGenerationTest {

    @Test
    fun `should generate const val for boolean default in companion object`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Boolean Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "isEnabled": {
                          "title": "Is Enabled",
                          "type": "boolean",
                          "default": true
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.boolean"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            assertTrue(Files.exists(attributesFile), "Expected generated attributes file at: $attributesFile")

            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("const val DEFAULT_IS_ENABLED: Boolean = true"),
                "Expected DEFAULT_IS_ENABLED constant in companion object"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate const val for boolean false default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Boolean False Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "isDisabled": {
                          "title": "Is Disabled",
                          "type": "boolean",
                          "default": false
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.booleanfalse"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("const val DEFAULT_IS_DISABLED: Boolean = false"),
                "Expected DEFAULT_IS_DISABLED constant for false value"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate const val for integer default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Integer Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "maxRetries": {
                          "title": "Max Retries",
                          "type": "integer",
                          "default": 5
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.integer"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("const val DEFAULT_MAX_RETRIES: Long = 5"),
                "Expected DEFAULT_MAX_RETRIES constant"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate const val for zero integer default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Zero Integer Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "retryCount": {
                          "title": "Retry Count",
                          "type": "integer",
                          "default": 0
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.zeroint"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("const val DEFAULT_RETRY_COUNT: Long = 0"),
                "Expected DEFAULT_RETRY_COUNT constant for zero value"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate const val for number default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Number Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "temperature": {
                          "title": "Temperature",
                          "type": "number",
                          "default": 22.5
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.number"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("const val DEFAULT_TEMPERATURE: Double = 22.5"),
                "Expected DEFAULT_TEMPERATURE constant"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate const val for string default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "String Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "name": {
                          "title": "Name",
                          "type": "string",
                          "default": "unknown"
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.string"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                Regex("""const val DEFAULT_NAME: String = "unknown"""").containsMatchIn(attributesContent),
                "Expected DEFAULT_NAME constant with string value"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate val for datetime default with Instant parse`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "DateTime Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "createdAt": {
                          "title": "Created At",
                          "type": "string",
                          "format": "date-time",
                          "default": "2024-01-01T00:00:00Z"
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.datetime"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                Regex("""val DEFAULT_CREATED_AT: Instant = Instant\.parse\("2024-01-01T00:00:00Z"\)""")
                    .containsMatchIn(attributesContent),
                "Expected DEFAULT_CREATED_AT with Instant.parse initializer"
            )
            assertFalse(
                attributesContent.contains("const val DEFAULT_CREATED_AT"),
                "Instant default should not use const val"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate val for string enum default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Enum Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "status": {
                          "title": "Status",
                          "type": "string",
                          "enum": ["active", "inactive", "pending"],
                          "default": "active"
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.stringenum"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                Regex("""val DEFAULT_STATUS: Status = Status\.ACTIVE""").containsMatchIn(attributesContent),
                "Expected DEFAULT_STATUS with enum constant reference"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate val for object enum default with name field`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Object Enum Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "lowBatteryBehavior": {
                          "title": "Low Battery Behavior",
                          "type": "object",
                          "enum": [
                            { "name": "shutdown", "description": "Shut down the device" },
                            { "name": "lowPower", "description": "Enter low power mode" }
                          ],
                          "default": { "name": "shutdown" }
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.objectenum"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                Regex("""val DEFAULT_LOW_BATTERY_BEHAVIOR: LowBatteryBehavior = LowBatteryBehavior\.\w+""")
                    .containsMatchIn(attributesContent),
                "Expected DEFAULT_LOW_BATTERY_BEHAVIOR with sealed class instance"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate val for empty array default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Empty Array Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "tags": {
                          "title": "Tags",
                          "type": "array",
                          "items": { "type": "string" },
                          "default": []
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.emptyarray"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                Regex("""val DEFAULT_TAGS: List<\w+> = emptyList\(\)""").containsMatchIn(attributesContent),
                "Expected DEFAULT_TAGS with emptyList() initializer"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate listOf for non-empty string array default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Non-Empty Array Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "defaultTags": {
                          "title": "Default Tags",
                          "type": "array",
                          "items": { "type": "string" },
                          "default": ["tag1", "tag2"]
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.nonemptyarray"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("DEFAULT_DEFAULT_TAGS"),
                "Non-empty string array default should generate DEFAULT_ constant"
            )
            assertTrue(
                attributesContent.contains("listOf("),
                "Non-empty string array default should use listOf()"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate listOf with enum values for non-empty enum array default`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Enum Array Default Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "enabledSeverities": {
                          "title": "Enabled Severities",
                          "type": "array",
                          "items": {
                            "type": "string",
                            "enum": ["OK", "WARNING", "CRITICAL"]
                          },
                          "default": ["OK", "WARNING"]
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.enumarray"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("DEFAULT_ENABLED_SEVERITIES"),
                "Enum array default should generate DEFAULT_ constant"
            )
            assertTrue(
                attributesContent.contains("EnabledSeveritiesItem"),
                "Enum array default should reference the enum type"
            )
            assertTrue(
                attributesContent.contains("listOf("),
                "Enum array default should use listOf()"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should skip object enum default without name field`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Object Enum Missing Name Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "behavior": {
                          "title": "Behavior",
                          "type": "object",
                          "enum": [
                            { "name": "option1" },
                            { "name": "option2" }
                          ],
                          "default": { "value": "something" }
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.missingname"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertFalse(
                attributesContent.contains("DEFAULT_BEHAVIOR"),
                "Object enum default without 'name' field should be skipped"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should convert camelCase property name to SCREAMING_SNAKE_CASE constant`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "CamelCase Property Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "maxRetryCount": {
                          "title": "Max Retry Count",
                          "type": "integer",
                          "default": 3
                        }
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.camelcase"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)
            assertTrue(
                attributesContent.contains("DEFAULT_MAX_RETRY_COUNT"),
                "Expected property name converted to SCREAMING_SNAKE_CASE"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `should generate multiple default constants in same companion object`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-default-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Multiple Defaults Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
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
                      }
                    }
                """.trimIndent())
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.defaulttest.multiple"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val attributesContent = Files.readString(attributesFile)

            assertTrue(attributesContent.contains("DEFAULT_IS_ENABLED"), "Expected DEFAULT_IS_ENABLED")
            assertTrue(attributesContent.contains("DEFAULT_MAX_RETRIES"), "Expected DEFAULT_MAX_RETRIES")
            assertTrue(attributesContent.contains("DEFAULT_TEMPERATURE"), "Expected DEFAULT_TEMPERATURE")
            assertFalse(attributesContent.contains("DEFAULT_NO_DEFAULT"), "Should not have constant for property without default")
        } finally {
            deleteRecursively(outputDir)
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }
}
