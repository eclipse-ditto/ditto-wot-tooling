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
package org.eclipse.ditto.wot.kotlin.generator.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for the enum generation configuration.
 *
 * @since 1.0.0
 */
class EnumGenerationConfigurationTest {

    @Test
    fun `test default configuration uses inline strategy`() {
        val config = GeneratorConfiguration(
            thingModelUrl = "https://example.org/thing-model.jsonld",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output")
        )

        assertEquals(EnumGenerationStrategy.INLINE, config.enumGenerationStrategy)
        assertTrue(config.generateEnums)
        assertTrue(config.generateDsl)
    }

    @Test
    fun `test separate class strategy configuration`() {
        val config = GeneratorConfiguration(
            thingModelUrl = "https://example.org/thing-model.jsonld",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output"),
            enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS
        )

        assertEquals(EnumGenerationStrategy.SEPARATE_CLASS, config.enumGenerationStrategy)
        assertTrue(config.generateEnums)
    }

    @Test
    fun `test configuration builder fluent API`() {
        val config = ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("target/test-output")
            .separateEnumClasses()
            .build()

        assertEquals(EnumGenerationStrategy.SEPARATE_CLASS, config.enumGenerationStrategy)
        assertEquals("https://example.org/thing-model.jsonld", config.thingModelUrl)
        assertEquals("org.example.generated", config.outputPackage)
    }

    @Test
    fun `test configuration builder preset configurations`() {
        val minimalConfig = ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("target/test-output")
            .minimal()
            .build()

        assertEquals(EnumGenerationStrategy.INLINE, minimalConfig.enumGenerationStrategy)
        assertTrue(minimalConfig.generateEnums)
        assertTrue(minimalConfig.generateDsl)
        assertFalse(minimalConfig.generateInterfaces)

        val comprehensiveConfig = ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("target/test-output")
            .comprehensive()
            .build()

        assertEquals(EnumGenerationStrategy.SEPARATE_CLASS, comprehensiveConfig.enumGenerationStrategy)
        assertTrue(comprehensiveConfig.generateEnums)
        assertTrue(comprehensiveConfig.generateDsl)
        assertTrue(comprehensiveConfig.generateInterfaces)
    }

    @Test
    fun `test configuration validation`() {
        // Valid configuration
        val validConfig = GeneratorConfiguration(
            thingModelUrl = "https://example.org/thing-model.jsonld",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output")
        )

        assertDoesNotThrow { validConfig.validate() }

        // Invalid configuration - blank thingModelUrl
        val invalidConfig = GeneratorConfiguration(
            thingModelUrl = "",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output")
        )

        assertThrows(IllegalArgumentException::class.java) { invalidConfig.validate() }
    }

    @Test
    fun `test configuration copy with updates`() {
        val originalConfig = GeneratorConfiguration(
            thingModelUrl = "https://example.org/thing-model.jsonld",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output")
        )

        val updatedConfig = originalConfig.copyWith(
            enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS,
            generateInterfaces = false
        )

        assertEquals(EnumGenerationStrategy.SEPARATE_CLASS, updatedConfig.enumGenerationStrategy)
        assertFalse(updatedConfig.generateInterfaces)
        assertEquals(originalConfig.thingModelUrl, updatedConfig.thingModelUrl)
        assertEquals(originalConfig.outputPackage, updatedConfig.outputPackage)
    }
}
