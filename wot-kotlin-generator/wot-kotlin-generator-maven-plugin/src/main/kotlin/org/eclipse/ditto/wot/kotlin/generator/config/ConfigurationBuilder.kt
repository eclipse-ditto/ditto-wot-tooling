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

import java.io.File

/**
 * Builder class for creating GeneratorConfiguration instances with a fluent API.
 *
 * This builder provides a convenient way to configure the WoT Kotlin generator
 * with sensible defaults and easy customization.
 *
 */
class ConfigurationBuilder {

    // Core parameters
    private var thingModelUrl: String? = null
    private var outputPackage: String? = null
    private var outputDirectory: File? = null

    // Enum generation strategy
    private var enumGenerationStrategy: EnumGenerationStrategy = EnumGenerationStrategy.INLINE

    // Backward compatibility parameters
    private var generateDsl: Boolean = true
    private var generateEnums: Boolean = true
    private var generateInterfaces: Boolean = true

    /**
     * Sets the URL or path to the WoT Thing Model to process.
     */
    fun thingModelUrl(url: String): ConfigurationBuilder {
        this.thingModelUrl = url
        return this
    }

    /**
     * Sets the base package for generated classes.
     */
    fun outputPackage(pkg: String): ConfigurationBuilder {
        this.outputPackage = pkg
        return this
    }

    /**
     * Sets the directory where generated files will be written.
     */
    fun outputDirectory(dir: File): ConfigurationBuilder {
        this.outputDirectory = dir
        return this
    }

    /**
     * Sets the directory where generated files will be written (string version).
     */
    fun outputDirectory(dir: String): ConfigurationBuilder {
        this.outputDirectory = File(dir)
        return this
    }

    /**
     * Sets the strategy for generating enums.
     */
    fun enumGenerationStrategy(strategy: EnumGenerationStrategy): ConfigurationBuilder {
        this.enumGenerationStrategy = strategy
        return this
    }

    /**
     * Sets enum generation to inline classes (current behavior).
     */
    fun inlineEnums(): ConfigurationBuilder {
        this.enumGenerationStrategy = EnumGenerationStrategy.INLINE
        return this
    }

    /**
     * Sets enum generation to separate enum classes.
     */
    fun separateEnumClasses(): ConfigurationBuilder {
        this.enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS
        return this
    }

    /**
     * Sets whether to generate DSL code.
     */
    fun generateDsl(generate: Boolean): ConfigurationBuilder {
        this.generateDsl = generate
        return this
    }

    /**
     * Sets whether to generate enums.
     */
    fun generateEnums(generate: Boolean): ConfigurationBuilder {
        this.generateEnums = generate
        return this
    }

    /**
     * Sets whether to generate interfaces.
     */
    fun generateInterfaces(generate: Boolean): ConfigurationBuilder {
        this.generateInterfaces = generate
        return this
    }

    /**
     * Applies a minimal configuration suitable for simple code generation.
     */
    fun minimal(): ConfigurationBuilder {
        this.generateDsl = true
        this.generateEnums = true
        this.generateInterfaces = false
        this.enumGenerationStrategy = EnumGenerationStrategy.INLINE
        return this
    }

    /**
     * Applies a comprehensive configuration with all features enabled.
     */
    fun comprehensive(): ConfigurationBuilder {
        this.generateDsl = true
        this.generateEnums = true
        this.generateInterfaces = true
        this.enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS
        return this
    }

    /**
     * Applies a configuration optimized for IDE support.
     */
    fun ideOptimized(): ConfigurationBuilder {
        this.generateDsl = true
        this.generateEnums = true
        this.generateInterfaces = true
        this.enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS
        return this
    }

    /**
     * Builds the GeneratorConfiguration instance.
     *
     * @throws IllegalStateException if required parameters are missing
     */
    fun build(): GeneratorConfiguration {
        requireNotNull(thingModelUrl) { "thingModelUrl is required" }
        requireNotNull(outputPackage) { "outputPackage is required" }
        requireNotNull(outputDirectory) { "outputDirectory is required" }

        return GeneratorConfiguration(
            thingModelUrl = thingModelUrl!!,
            outputPackage = outputPackage!!,
            outputDirectory = outputDirectory!!,
            enumGenerationStrategy = enumGenerationStrategy,
            generateDsl = generateDsl,
            generateEnums = generateEnums,
            generateInterfaces = generateInterfaces
        )
    }

    companion object {
        /**
         * Creates a new ConfigurationBuilder instance.
         */
        fun builder(): ConfigurationBuilder = ConfigurationBuilder()
    }
}
