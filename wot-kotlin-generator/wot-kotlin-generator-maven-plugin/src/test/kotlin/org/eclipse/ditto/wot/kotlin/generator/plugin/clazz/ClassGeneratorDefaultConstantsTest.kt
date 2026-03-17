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
package org.eclipse.ditto.wot.kotlin.generator.plugin.clazz

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for ClassGenerator's default constant extraction functionality.
 *
 * These tests verify that ClassGenerator properly integrates with DefaultValueExtractor
 * for extracting and placing DEFAULT_* constants on companion objects.
 *
 * @since 1.0.0
 */
class ClassGeneratorDefaultConstantsTest {

    private lateinit var pathFunSpec: FunSpec

    @BeforeEach
    fun setUp() {
        pathFunSpec = FunSpec.builder("startPath")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", "test")
            .build()
    }

    @Test
    fun `should add default constants to new companion object`() {
        val typeSpecBuilder = TypeSpec.classBuilder("TestClass")
        val defaultConstants = listOf(
            PropertySpec.builder("DEFAULT_IS_ENABLED", BOOLEAN)
                .addModifiers(KModifier.CONST)
                .initializer("true")
                .build(),
            PropertySpec.builder("DEFAULT_MAX_RETRIES", LONG)
                .addModifiers(KModifier.CONST)
                .initializer("3L")
                .build()
        )

        ClassGenerator.mergeOrAddCompanionObject(typeSpecBuilder, pathFunSpec, defaultConstants)

        val typeSpec = typeSpecBuilder.build()
        val companion = typeSpec.typeSpecs.find { it.isCompanion }
        assertNotNull(companion)
        assertEquals(2, companion!!.propertySpecs.size)
        assertTrue(companion.propertySpecs.any { it.name == "DEFAULT_IS_ENABLED" })
        assertTrue(companion.propertySpecs.any { it.name == "DEFAULT_MAX_RETRIES" })
    }

    @Test
    fun `should merge default constants into existing companion object`() {
        val existingCompanion = TypeSpec.companionObjectBuilder()
            .addSuperinterface(ClassName("org.eclipse.ditto.wot.kotlin.generator.common.path", "HasPath"))
            .addFunction(pathFunSpec)
            .addProperty(
                PropertySpec.builder("EXISTING_CONSTANT", String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", "existing")
                    .build()
            )
            .build()

        val typeSpecBuilder = TypeSpec.classBuilder("TestClass")
            .addType(existingCompanion)

        val defaultConstants = listOf(
            PropertySpec.builder("DEFAULT_IS_ENABLED", BOOLEAN)
                .addModifiers(KModifier.CONST)
                .initializer("true")
                .build()
        )

        ClassGenerator.mergeOrAddCompanionObject(typeSpecBuilder, pathFunSpec, defaultConstants)

        val typeSpec = typeSpecBuilder.build()
        val companion = typeSpec.typeSpecs.find { it.isCompanion }
        assertNotNull(companion)
        // Should have both existing and new constants
        assertEquals(2, companion!!.propertySpecs.size)
        assertTrue(companion.propertySpecs.any { it.name == "EXISTING_CONSTANT" })
        assertTrue(companion.propertySpecs.any { it.name == "DEFAULT_IS_ENABLED" })
    }

    @Test
    fun `should not duplicate existing constants when merging`() {
        val existingCompanion = TypeSpec.companionObjectBuilder()
            .addSuperinterface(ClassName("org.eclipse.ditto.wot.kotlin.generator.common.path", "HasPath"))
            .addFunction(pathFunSpec)
            .addProperty(
                PropertySpec.builder("DEFAULT_IS_ENABLED", BOOLEAN)
                    .addModifiers(KModifier.CONST)
                    .initializer("true")
                    .build()
            )
            .build()

        val typeSpecBuilder = TypeSpec.classBuilder("TestClass")
            .addType(existingCompanion)

        val defaultConstants = listOf(
            PropertySpec.builder("DEFAULT_IS_ENABLED", BOOLEAN)
                .addModifiers(KModifier.CONST)
                .initializer("true")
                .build()
        )

        ClassGenerator.mergeOrAddCompanionObject(typeSpecBuilder, pathFunSpec, defaultConstants)

        val typeSpec = typeSpecBuilder.build()
        val companion = typeSpec.typeSpecs.find { it.isCompanion }
        assertNotNull(companion)
        // Should NOT duplicate the constant
        assertEquals(1, companion!!.propertySpecs.size)
    }

    @Test
    fun `should handle empty default constants list`() {
        val typeSpecBuilder = TypeSpec.classBuilder("TestClass")
        val defaultConstants = emptyList<PropertySpec>()

        ClassGenerator.mergeOrAddCompanionObject(typeSpecBuilder, pathFunSpec, defaultConstants)

        val typeSpec = typeSpecBuilder.build()
        val companion = typeSpec.typeSpecs.find { it.isCompanion }
        assertNotNull(companion)
        // Should still have companion with HasPath but no extra constants
        assertTrue(companion!!.propertySpecs.isEmpty())
    }

    @Test
    fun `should add default constants to companion builder`() {
        val companionBuilder = TypeSpec.companionObjectBuilder()
            .addSuperinterface(ClassName("org.eclipse.ditto.wot.kotlin.generator.common.path", "HasPath"))

        val defaultConstants = listOf(
            PropertySpec.builder("DEFAULT_TEMPERATURE", com.squareup.kotlinpoet.DOUBLE)
                .addModifiers(KModifier.CONST)
                .initializer("22.5")
                .build()
        )

        ClassGenerator.addDefaultsToCompanion(companionBuilder, defaultConstants)

        val companion = companionBuilder.build()
        assertEquals(1, companion.propertySpecs.size)
        assertTrue(companion.propertySpecs.any { it.name == "DEFAULT_TEMPERATURE" })
    }

    @Test
    fun `should support single-argument version for backward compatibility`() {
        val typeSpecBuilder = TypeSpec.classBuilder("TestClass")

        ClassGenerator.mergeOrAddCompanionObject(typeSpecBuilder, pathFunSpec)

        val typeSpec = typeSpecBuilder.build()
        val companion = typeSpec.typeSpecs.find { it.isCompanion }
        assertNotNull(companion)
        assertTrue(companion!!.funSpecs.any { it.name == "startPath" })
    }
}
