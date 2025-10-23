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
package org.eclipse.ditto.wot.kotlin.generator

import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.wot.kotlin.generator.config.GeneratorConfiguration
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Entry point for the WoT Kotlin code generator.
 *
 * This object provides the main entry points for running the generator either
 * programmatically or from the command line. It handles both command-line arguments
 * and configuration-based execution.
 *
 * The generator can be used in two ways:
 * 1. Command-line execution with arguments: `modelUrl packageName outputDir`
 * 2. Programmatic execution with a [org.eclipse.ditto.wot.kotlin.generator.config.GeneratorConfiguration] object
  *
 */
object GeneratorStarter {

    private val thingModelGenerator = ThingModelGenerator
    private val logger = LoggerFactory.getLogger(ThingModelGenerator::class.java)

    /**
     * Runs the generator with command-line arguments.
     *
     * This method parses command-line arguments and creates a default configuration
     * for backward compatibility. The expected arguments are:
     * - `modelUrl`: URL or path to the WoT Thing Model
     * - `packageName`: Package name for generated classes
     * - `outputDir`: Output directory for generated files
     *
     * @param args Command-line arguments array
     * @return Exit code (0 for success, 1 for failure)
     * @throws Exception if the generation process fails
     */
    @Throws(Exception::class)
    fun run(args: Array<String>): Int {
        val modelUrl: String
        val packageName: String
        val outputDir: String
        if (args.isEmpty()) {
            logger.error("No arguments provided")
            return 1
        } else if (args.size == 3) {
            modelUrl = args[0]
            packageName = args[1]
            outputDir = args[2]
        } else {
            logger.error("Too few or many arguments provided")
            return 1
        }

        logger.info("-----> Starting generator with args: $modelUrl, $packageName, $outputDir")

        // Create default configuration for backward compatibility
        val defaultConfig = GeneratorConfiguration(
            thingModelUrl = modelUrl,
            outputPackage = packageName,
            outputDirectory = java.io.File(outputDir)
        )

        return run(defaultConfig)
    }

    /**
     * Runs the generator with a specific configuration.
     *
     * This is the main execution method that:
     * 1. Loads the WoT Thing Model from the configured URL
     * 2. Generates Kotlin code using the specified configuration
     * 3. Handles any exceptions and returns appropriate exit codes
     *
     * @param config The configuration controlling the generation process
     * @return Exit code (0 for success, 1 for failure)
     * @throws Exception if the generation process fails
     */
    @Throws(Exception::class)
    fun run(config: GeneratorConfiguration): Int {
        logger.info("-----> Starting generator with configuration: ${config.enumGenerationStrategy}")
        return try {
            runBlocking {
                val thingModel = thingModelGenerator.loadModel(config.thingModelUrl)
                thingModelGenerator.generate(thingModel, config)
            }
            0
        } catch (e: Exception) {
            logger.error("An exception occurred in the generator", e)
            1
        }
    }
}

/**
 * Main function for command-line execution.
 *
 * This function serves as the entry point when the generator is run as a standalone
 * application. It delegates to [GeneratorStarter.run] and exits with the appropriate
 * exit code.
 *
 * @param args Command-line arguments
 */
fun main(args: Array<String>) {
    val exitCode = GeneratorStarter.run(args)
    exitProcess(exitCode)
}
