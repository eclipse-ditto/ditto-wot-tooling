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


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.wot.kotlin.generator.config.GeneratorConfiguration
import org.eclipse.ditto.wot.kotlin.generator.serialize.ExplicitNullAwareBeanSerializerModifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test

class GeneratorManualRunner {

    private val modelUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld"
    private val outputDir = "generated-test-sources"
    private val packageName = "org.eclipse.ditto.wot.kotlin.gen.floorlamp"

    private val logger: Logger = LoggerFactory.getLogger(GeneratorManualRunner::class.java)

    @Test
    fun `test generator execution for lamp model with suspend DSL succeeds`() {
        val outputDirPath = javaClass.getResource(".").path + outputDir
        logger.info("-----> Starting generator with suspend DSL: $modelUrl, $packageName, $outputDirPath")

        val thingModelGenerator = ThingModelGenerator

        // Create configuration with suspend DSL enabled
        val config = GeneratorConfiguration(
            thingModelUrl = modelUrl,
            outputPackage = packageName,
            outputDirectory = java.io.File(outputDirPath),
            generateSuspendDsl = true
        )

        runBlocking {
            val lampModel = thingModelGenerator.loadModel(modelUrl)
            thingModelGenerator.generate(lampModel, config)
        }
    }

    @Test
    fun `test generator execution for lamp model succeeds`() {
        val outputDirPath = javaClass.getResource(".").path + outputDir + "2"
        logger.info("-----> Starting generator with args: $modelUrl, $packageName, $outputDirPath")

        val thingModelGenerator = ThingModelGenerator

        runBlocking {
            val lampModel = thingModelGenerator.loadModel(modelUrl)
            thingModelGenerator.generate(lampModel, packageName, outputDirPath)
        }
    }

    @Test
    fun testExplicitlyGeneratedNullValues() {
        val objectWriter = ObjectMapper()
            .registerModule(JavaTimeModule()) // be able to serialize java.time.Instant
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(
                SimpleModule()
                    .setSerializerModifier(ExplicitNullAwareBeanSerializerModifier())
            )
            .writerWithDefaultPrettyPrinter()
    }

}
