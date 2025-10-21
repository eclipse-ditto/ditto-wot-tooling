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
 * Configuration for the WoT Kotlin code generator.
 *
 * This class contains all the parameters that control how Kotlin code
 * is generated from WoT Thing Models.
 *
 */
data class GeneratorConfiguration(
    /** The URL or path to the WoT Thing Model to process */
    val thingModelUrl: String,

    /** The base package for generated classes */
    val outputPackage: String,

    /** The directory where generated files will be written */
    val outputDirectory: File,

    /** Strategy for generating enums */
    val enumGenerationStrategy: EnumGenerationStrategy = EnumGenerationStrategy.INLINE,

    /** Strategy for naming generated classes */
    val classNamingStrategy: ClassNamingStrategy = ClassNamingStrategy.COMPOUND_ALL,

    /** Whether to generate DSL code (maintains backward compatibility) */
    val generateDsl: Boolean = true,

    /** Whether DSL builder functions should be suspend functions */
    val generateSuspendDsl: Boolean = false,

    /** Whether to generate enums (maintains backward compatibility) */
    val generateEnums: Boolean = true,

    /** Whether to generate interfaces (maintains backward compatibility) */
    val generateInterfaces: Boolean = true
) {
    /**
     * Creates a copy with updated values, useful for building configurations incrementally.
     */
    fun copyWith(
        thingModelUrl: String? = null,
        outputPackage: String? = null,
        outputDirectory: File? = null,
        enumGenerationStrategy: EnumGenerationStrategy? = null,
        classNamingStrategy: ClassNamingStrategy? = null,
        generateDsl: Boolean? = null,
        generateSuspendDsl: Boolean? = null,
        generateEnums: Boolean? = null,
        generateInterfaces: Boolean? = null
    ): GeneratorConfiguration {
        return copy(
            thingModelUrl = thingModelUrl ?: this.thingModelUrl,
            outputPackage = outputPackage ?: this.outputPackage,
            outputDirectory = outputDirectory ?: this.outputDirectory,
            enumGenerationStrategy = enumGenerationStrategy ?: this.enumGenerationStrategy,
            classNamingStrategy = classNamingStrategy ?: this.classNamingStrategy,
            generateDsl = generateDsl ?: this.generateDsl,
            generateSuspendDsl = generateSuspendDsl ?: this.generateSuspendDsl,
            generateEnums = generateEnums ?: this.generateEnums,
            generateInterfaces = generateInterfaces ?: this.generateInterfaces
        )
    }

    /**
     * Validates the configuration and throws an exception if invalid.
     */
    fun validate() {
        require(thingModelUrl.isNotBlank()) { "thingModelUrl cannot be blank" }
        require(outputPackage.isNotBlank()) { "outputPackage cannot be blank" }
        require(outputDirectory.exists() || outputDirectory.mkdirs()) {
            "Cannot create or access output directory: $outputDirectory"
        }
    }

    companion object {
        /**
         * Creates a default configuration with inline enum generation (backward compatible).
         */
        fun default(): GeneratorConfiguration {
            return GeneratorConfiguration(
                thingModelUrl = "",
                outputPackage = "",
                outputDirectory = File(""),
                enumGenerationStrategy = EnumGenerationStrategy.INLINE
            )
        }
    }
}
