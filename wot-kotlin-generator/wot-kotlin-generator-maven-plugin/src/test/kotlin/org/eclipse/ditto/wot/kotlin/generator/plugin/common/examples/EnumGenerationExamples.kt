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
package org.eclipse.ditto.wot.kotlin.generator.plugin.common.examples

import org.eclipse.ditto.wot.kotlin.generator.plugin.config.ConfigurationBuilder
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration

/**
 * Examples demonstrating the enum generation configuration.
 *
 * This class shows various ways to configure enum generation for different
 * use cases and requirements.
 *
 * @since 1.0.0
 */
object EnumGenerationExamples {

    /**
     * Example 1: Default configuration with inline enums (backward compatible).
     *
     * This maintains the current behavior where enums are nested within
     * the classes that use them.
     */
    fun defaultInlineConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .inlineEnums() // Explicitly set inline strategy
            .build()
    }

    /**
     * Example 2: Separate class enum configuration for better IDE support.
     *
     * This generates enums as standalone classes in separate files,
     * providing better IDE support and standalone usage.
     */
    fun separateClassConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .separateEnumClasses() // Generate separate enum classes
            .build()
    }

    /**
     * Example 3: IDE-optimized configuration with separate enum classes.
     *
     * This configuration is optimized for development environments
     * where IDE support and code navigation are important.
     */
    fun ideOptimizedConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .ideOptimized() // Uses separate enum classes
            .build()
    }

    /**
     * Example 4: Minimal configuration with inline enums.
     *
     * Simple configuration suitable for basic code generation.
     */
    fun minimalConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .minimal() // Uses inline enums by default
            .build()
    }

    /**
     * Example 5: Comprehensive configuration with separate enum classes.
     *
     * Full-featured configuration with all options enabled.
     */
    fun comprehensiveConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .comprehensive() // Uses separate enum classes
            .build()
    }

    /**
     * Example 6: Custom configuration with specific enum strategy.
     *
     * Shows how to create a custom configuration with specific
     * enum generation preferences.
     */
    fun customConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .enumGenerationStrategy(EnumGenerationStrategy.SEPARATE_CLASS)
            .generateDsl(true)
            .generateEnums(true)
            .generateInterfaces(false)
            .build()
    }

    /**
     * Example 7: Configuration for production use with separate enum classes.
     *
     * Production-ready configuration that prioritizes maintainability
     * and IDE support.
     */
    fun productionConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .separateEnumClasses()
            .generateDsl(true)
            .generateEnums(true)
            .generateInterfaces(true)
            .build()
    }

    /**
     * Example 8: Configuration for development with inline enums.
     *
     * Development configuration that maintains the current behavior
     * for easier debugging and testing.
     */
    fun developmentConfiguration(): GeneratorConfiguration {
        return ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .inlineEnums()
            .generateDsl(true)
            .generateEnums(true)
            .generateInterfaces(false)
            .build()
    }

    /**
     * Example 9: Configuration builder with incremental updates.
     *
     * Shows how to build configurations incrementally and update
     * existing configurations.
     */
    fun incrementalConfiguration(): GeneratorConfiguration {
        // Start with a base configuration
        val baseConfig = ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .build()

        // Update with specific enum strategy
        return baseConfig.copyWith(
            enumGenerationStrategy = EnumGenerationStrategy.SEPARATE_CLASS,
            generateInterfaces = true
        )
    }

    /**
     * Example 10: Configuration validation example.
     *
     * Shows how to validate configurations before use.
     */
    fun validatedConfiguration(): GeneratorConfiguration {
        val config = ConfigurationBuilder.builder()
            .thingModelUrl("https://example.org/thing-model.jsonld")
            .outputPackage("org.example.generated")
            .outputDirectory("src/main/kotlin")
            .separateEnumClasses()
            .build()

        // Validate the configuration
        config.validate()

        return config
    }
}
