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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Main entry point for the WoT to OpenAPI generator.
 * This object provides the command-line interface and orchestration logic
 * for generating OpenAPI specifications from WoT Thing models.
 */
object GeneratorStarter {

    private val logger: Logger = LoggerFactory.getLogger(GeneratorStarter::class.java)

    /** WoT model loader instance */
    private var wotLoader: WotLoader = WotLoader

    /**
     * Runs the generator with the provided command-line arguments.
     * 
     * @param args Command-line arguments array containing:
     *             - modelVersion: Version of the WoT model
     *             - modelName: Name of the WoT model
     *             - dittoBaseUrl: Base URL for Ditto API (e.g., "https://ditto.example.com")
     *             - baseUrl: Base URL for model loading (optional)
     * @return Exit code (0 for success, 1 for failure)
     * @throws Exception if an error occurs during generation
     */
    @Throws(Exception::class)
    fun run(args: Array<String>): Int {
        logger.info("Run function started with args: ${args.joinToString(", ")}")

        val modelVersion: String
        val modelName: String
        var dittoBaseUrl = "https://ditto.example.com"
        var baseUrl = ""
        when {
            args.size < 2 -> {
                logger.error("Please provide (i) model version, (ii) model name, and optionally (iii) ditto base URL and (iv) base URL for model loading")
                return 1
            }
            args.size == 2 -> {
                modelVersion = args[0]
                modelName = args[1]
            }
            args.size == 3 -> {
                modelVersion = args[0]
                modelName = args[1]
                dittoBaseUrl = args[2]
            }
            args.size == 4 -> {
                modelVersion = args[0]
                modelName = args[1]
                dittoBaseUrl = args[2]
                baseUrl = args[3]
            }
            else -> {
                logger.error("Too many arguments provided")
                return 1
            }
        }

        logger.info("-------------------------------------> Starting generator with args: $modelVersion, $modelName, dittoBaseUrl: $dittoBaseUrl")
        try {
            runBlocking {
                val rootModel = wotLoader.loadModel(modelVersion, modelName, dittoBaseUrl, baseUrl)
                wotLoader.generate(rootModel, modelVersion, modelName, dittoBaseUrl)
            }
            logger.info("-------------------------------------> Generator finished")
            return 0
        } catch (e: Exception) {
            logger.error("An exception occurred in the generator", e)
            e.printStackTrace()
            return 1
        }
    }

    /**
     * Main entry point for the application.
     * Parses command-line arguments and executes the generator.
     * 
     * @param args Command-line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Main function started...")
        try {
            val exitCode = run(args)
            logger.info("Main function completed with exit code: $exitCode")
            exitProcess(exitCode)
        } catch (e: Exception) {
            logger.error("An exception occurred: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
