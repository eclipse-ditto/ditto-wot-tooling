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
package org.eclipse.ditto.wot.kotlin.generator.strategy

import org.eclipse.ditto.wot.kotlin.generator.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.config.GeneratorConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for the enum generation strategies.
 *
 * @since 1.0.0
 */
class EnumGenerationStrategyTest {

    @Test
    fun `test factory creates inline strategy for inline configuration`() {
        val config = GeneratorConfiguration(
            thingModelUrl = "https://example.org/thing-model.jsonld",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output"),
            enumGenerationStrategy = EnumGenerationStrategy.INLINE
        )

        val strategy = EnumGenerationStrategyFactory.createStrategy(config)
        assertTrue(strategy is InlineEnumGenerationStrategy)
    }

    @Test
    fun `test factory creates separate class strategy for separate class configuration`() {
        val config = GeneratorConfiguration(
            thingModelUrl = "https://example.org/thing-model.jsonld",
            outputPackage = "org.example.generated",
            outputDirectory = File("target/test-output"),
            enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS
        )

        val strategy = EnumGenerationStrategyFactory.createStrategy(config)
        assertTrue(strategy is SeparateClassEnumGenerationStrategy)
    }

    @Test
    fun `test inline strategy adds enums to type spec`() {
        val strategy = InlineEnumGenerationStrategy()
        val typeSpecBuilder = com.squareup.kotlinpoet.TypeSpec.classBuilder("TestClass")

        // Create a mock field with enum type
        val field = "testField" to com.squareup.kotlinpoet.PropertySpec.builder(
            "testField",
            com.squareup.kotlinpoet.ClassName("", "TestEnum")
        ).build()

        val fields = listOf(field)
        val result = strategy.addEnumsToTypeSpec(fields, typeSpecBuilder)

        // Should return the same fields since no actual enum is registered
        assertEquals(fields, result)
    }

    @Test
    fun `test separate class strategy does not add enums to type spec`() {
        val strategy = SeparateClassEnumGenerationStrategy()
        val typeSpecBuilder = com.squareup.kotlinpoet.TypeSpec.classBuilder("TestClass")

        val field = "testField" to com.squareup.kotlinpoet.PropertySpec.builder(
            "testField",
            com.squareup.kotlinpoet.ClassName("", "TestEnum")
        ).build()

        val fields = listOf(field)
        val result = strategy.addEnumsToTypeSpec(fields, typeSpecBuilder)

        // Should return the same fields without modification
        assertEquals(fields, result)
    }

    @Test
    fun `test both strategies implement the interface`() {
        val inlineStrategy = InlineEnumGenerationStrategy()
        val separateClassStrategy = SeparateClassEnumGenerationStrategy()

        assertTrue(inlineStrategy is IEnumGenerationStrategy)
        assertTrue(separateClassStrategy is IEnumGenerationStrategy)
    }
}
