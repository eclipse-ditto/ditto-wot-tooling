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
package org.eclipse.ditto.wot.kotlin.generator.plugin

import kotlinx.coroutines.runBlocking
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.Const
import org.slf4j.impl.StaticLoggerBinder
import java.io.File

/**
 * Maven plugin for generating Kotlin code from WoT (Web of Things) Thing Models.
 *
 * This plugin integrates the WoT Kotlin code generator into Maven build processes.
 * It can be configured in a Maven pom.xml file to automatically generate Kotlin classes
 * from WoT Thing Models during the build lifecycle.
 *
 * The plugin supports various configuration options including:
 * - Enum generation strategies (INLINE or SEPARATE_CLASS)
 * - DSL generation control
 * - Output package and directory configuration
 * - Backward compatibility options
 *
 */
@Mojo(name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class WotKotlinCodegenMojo: AbstractMojo() {

    companion object {
        /** Default package name for generated classes */
        const val DEFAULT_PACKAGE_NAME = Const.COMMON_PACKAGE
        /** Default output directory for generated sources */
        const val DEFAULT_OUTPUT_DIR = "target/generated-sources"
    }

    /** The current Maven project, injected by Maven */
    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    var project: MavenProject? = null

    /** URL or path to the WoT Thing Model to process */
    @Parameter(property = "thingModelUrl", required = true)
    var thingModelUrl: String? = null

    /** Package name for generated classes */
    @Parameter(property = "packageName", defaultValue = DEFAULT_PACKAGE_NAME)
    var packageName: String? = null

    /** Output directory for generated source files */
    @Parameter(property = "outputDir", defaultValue = DEFAULT_OUTPUT_DIR)
    var outputDir: File? = null

    /** Strategy for generating enums: INLINE or SEPARATE_CLASS */
    @Parameter(property = "enumGenerationStrategy", defaultValue = "INLINE")
    var enumGenerationStrategy: String? = null

    /** Strategy for naming classes: COMPOUND_ALL or ORIGINAL_THEN_COMPOUND */
    @Parameter(property = "classNamingStrategy", defaultValue = "COMPOUND_ALL")
    var classNamingStrategy: String? = null

    /** Whether to generate DSL code (maintains backward compatibility) */
    @Parameter(property = "generateDsl", defaultValue = "true")
    var generateDsl: Boolean = true

    /** Whether DSL builder functions should be suspend functions */
    @Parameter(property = "generateSuspendDsl", defaultValue = "false")
    var generateSuspendDsl: Boolean = false

    /** Whether to generate enums (maintains backward compatibility) */
    @Parameter(property = "generateEnums", defaultValue = "true")
    var generateEnums: Boolean = true

    /** Whether to generate interfaces (maintains backward compatibility) */
    @Parameter(property = "generateInterfaces", defaultValue = "true")
    var generateInterfaces: Boolean = true

    /**
     * Main execution method called by Maven during the build lifecycle.
     *
     * This method:
     * 1. Validates required parameters
     * 2. Resolves output directory and package name
     * 3. Creates a [GeneratorConfiguration] with all parameters
     * 4. Executes the code generation process
     * 5. Adds the generated sources to the Maven project's compile source roots
     *
     * @throws MojoExecutionException if the generation process fails
     */
    override fun execute() {
        StaticLoggerBinder.getSingleton().mavenLog = log

        if (thingModelUrl == null) {
            throw MojoExecutionException("The required parameter 'thingModelUrl' was not configured")
        }

        val output = resolveOutputDir()
        val packageN = resolvePackageName()
        log.info("---> Starting WoT kotlin code generator, loading model from <$thingModelUrl>, generating " +
                     "into package <$packageN> and outputDir <$output>")

        try {
            // Create configuration with new parameters
            val config = createConfiguration(thingModelUrl!!, packageN, output)
            log.info("---> Using enum generation strategy: ${config.enumGenerationStrategy}")
            log.info("---> Using class naming strategy: ${config.classNamingStrategy}")
            log.info("---> Generate DSL: ${config.generateDsl}")
            log.info("---> Generate Suspend DSL: ${config.generateSuspendDsl}")
            log.info("---> Generate Enums: ${config.generateEnums}")
            log.info("---> Generate Interfaces: ${config.generateInterfaces}")

            runBlocking {
                GeneratorStarter.run(config)
            }
            log.info("---> Adding path to compile source root: ${output.path}")
            project!!.addCompileSourceRoot(output.path)

            // Add common model directory to compile source roots
            val commonModelDir = File(output.parent, "common-model")
            if (commonModelDir.exists()) {
                log.info("---> Adding common model path to compile source root: ${commonModelDir.path}")
                project!!.addCompileSourceRoot(commonModelDir.path)
            }
        } catch (e: Exception) {
            throw MojoExecutionException("Exception during generation", e)
        }
    }

    /**
     * Resolves the package name, using the configured value or default.
     *
     * @return The resolved package name for generated classes
     */
    private fun resolvePackageName() = packageName ?: DEFAULT_PACKAGE_NAME

    /**
     * Resolves the output directory, using the configured value or default.
     *
     * @return The resolved output directory for generated files
     */
    private fun resolveOutputDir() = outputDir ?: File(DEFAULT_OUTPUT_DIR)

    /**
     * Creates a [GeneratorConfiguration] from the Maven plugin parameters.
     *
     * This method maps the Maven plugin parameters to the internal configuration
     * object used by the generator. It handles enum generation strategy parsing
     * and provides backward compatibility for legacy parameters.
     *
     * @param modelUrl The URL to the WoT Thing Model
     * @param packageName The package name for generated classes
     * @param outputDir The output directory for generated files
     * @return A configured [GeneratorConfiguration] object
     */
    private fun createConfiguration(modelUrl: String, packageName: String, outputDir: File): GeneratorConfiguration {
        val enumStrategy = when (enumGenerationStrategy?.uppercase()) {
            "SEPARATE_CLASS" -> EnumGenerationStrategy.SEPARATE_CLASS
            "INLINE", null -> EnumGenerationStrategy.INLINE
            else -> {
                log.warn("Unknown enum generation strategy: $enumGenerationStrategy, using INLINE")
                EnumGenerationStrategy.INLINE
            }
        }

        val namingStrategy = when (classNamingStrategy?.uppercase()) {
            "ORIGINAL_THEN_COMPOUND" -> ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
            "COMPOUND_ALL", null -> ClassNamingStrategy.COMPOUND_ALL
            else -> {
                log.warn("Unknown class naming strategy: $classNamingStrategy, using COMPOUND_ALL")
                ClassNamingStrategy.COMPOUND_ALL
            }
        }

        return GeneratorConfiguration(
            thingModelUrl = modelUrl,
            outputPackage = packageName,
            outputDirectory = outputDir,
            enumGenerationStrategy = enumStrategy,
            classNamingStrategy = namingStrategy,
            generateDsl = generateDsl,
            generateSuspendDsl = generateSuspendDsl,
            generateEnums = generateEnums,
            generateInterfaces = generateInterfaces
        )
    }
}
