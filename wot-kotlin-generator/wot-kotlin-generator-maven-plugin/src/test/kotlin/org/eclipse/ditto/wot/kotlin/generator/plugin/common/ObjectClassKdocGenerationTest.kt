/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.wot.kotlin.generator.plugin.common

import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.kotlin.generator.plugin.ThingModelGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.model.ThingModel
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies that data classes generated from a WoT object schema carry class-level KDoc derived from the
 * schema's title and description (e.g. an `Location` attribute object), not only their individual fields.
 */
class ObjectClassKdocGenerationTest {

    @Test
    fun `object property data class has class-level kdoc from schema title and description`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-object-class-kdoc-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Asset",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "location": {
                          "title": "Location",
                          "description": "The location of the asset",
                          "type": "object",
                          "properties": {
                            "label": { "title": "Label", "type": "string" }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.objectclasskdoctest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val locationFile = outputDir.resolve("$packagePath/attributes/Location.kt")
            assertTrue(Files.exists(locationFile), "Expected generated Location class at: $locationFile")

            val content = Files.readString(locationFile)

            // Class-level KDoc must precede the data class declaration (before the annotations).
            val kdocBeforeClass = Regex(
                """/\*\*[\s\S]*?Location[\s\S]*?The location of the asset[\s\S]*?\*/[\s\S]*?public data class Location"""
            )
            assertTrue(
                kdocBeforeClass.containsMatchIn(content),
                "Expected class-level KDoc with title and description on Location. Generated content:\n$content"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }
}
