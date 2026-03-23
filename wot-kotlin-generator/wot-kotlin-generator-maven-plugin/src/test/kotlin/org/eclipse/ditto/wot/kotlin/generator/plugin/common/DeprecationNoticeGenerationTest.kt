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

class DeprecationNoticeGenerationTest {

    @Test
    fun `deprecated notice generates kotlin deprecated annotations`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-deprecation-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Deprecation Demo Thing",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "legacyLocation": {
                          "title": "Legacy Location",
                          "type": "object",
                          "properties": {
                            "street": { "type": "string" }
                          },
                          "ditto:deprecationNotice": {
                            "deprecated": true,
                            "supersededBy": "#/properties/newLocation",
                            "removalVersion": "2.0.0"
                          }
                        },
                        "newLocation": {
                          "title": "New Location",
                          "type": "object",
                          "properties": {
                            "street": { "type": "string" }
                          }
                        }
                      },
                      "actions": {
                        "legacyAction": {
                          "title": "Legacy Action",
                          "input": { "type": "string" },
                          "ditto:deprecationNotice": {
                            "deprecated": true,
                            "supersededBy": "#/actions/newAction",
                            "removalVersion": "2.0.0"
                          }
                        },
                        "newAction": {
                          "title": "New Action",
                          "input": { "type": "string" }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.deprecationtest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val attributesFile = outputDir.resolve("$packagePath/attributes/Attributes.kt")
            val actionFile = outputDir.resolve("$packagePath/actions/LegacyAction.kt")

            assertTrue(Files.exists(attributesFile), "Expected generated attributes file at: $attributesFile")
            assertTrue(Files.exists(actionFile), "Expected generated action file at: $actionFile")

            val attributesContent = Files.readString(attributesFile)
            val actionContent = Files.readString(actionFile)

            assertTrue(
                Regex("(?s)@Deprecated\\(.*newLocation\\(\\).*\\)\\s*(public\\s+)?(override\\s+)?var legacyLocation")
                    .containsMatchIn(attributesContent),
                "Expected legacyLocation property to be annotated as deprecated"
            )
            assertTrue(
                Regex("(?s)@Deprecated\\(.*newLocation\\(block\\).*\\)\\s*(public\\s+)?(suspend\\s+)?fun legacyLocation\\(")
                    .containsMatchIn(attributesContent),
                "Expected legacyLocation DSL builder function to be annotated as deprecated"
            )
            assertTrue(
                actionContent.contains("@Deprecated(") &&
                    actionContent.contains("legacyAction(") &&
                    actionContent.contains("Will be removed in version 2.0.0."),
                "Expected legacyAction function to be annotated as deprecated"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `deprecated submodel link generates kotlin deprecated annotations on feature class and DSL`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-deprecated-submodel-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": [
                        "https://www.w3.org/2022/wot/td/v1.1",
                        {
                          "ditto": "https://ditto.eclipseprojects.io/wot/ditto-extension#"
                        }
                      ],
                      "@type": "tm:ThingModel",
                      "title": "Deprecated Submodel Demo",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "name": {
                          "title": "Name",
                          "type": "string"
                        }
                      },
                      "links": [
                        {
                          "rel": "tm:submodel",
                          "href": "https://eclipse-ditto.github.io/ditto-examples/wot/models/dimmable-colored-lamp-1.0.0.tm.jsonld",
                          "type": "application/tm+json",
                          "instanceName": "Spot1",
                          "ditto:deprecationNotice": {
                            "deprecated": true,
                            "removalVersion": "2.0.0"
                          }
                        },
                        {
                          "rel": "tm:submodel",
                          "href": "https://eclipse-ditto.github.io/ditto-examples/wot/models/connection-status-1.0.0.tm.jsonld",
                          "type": "application/tm+json",
                          "instanceName": "ConnectionStatus"
                        }
                      ]
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.deprecatedsubmodeltest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val featuresFile = outputDir.resolve("$packagePath/features/Features.kt")
            val spot1FeatureFile = outputDir.resolve("$packagePath/features/spot1/Spot1.kt")
            val connectionStatusFeatureFile = outputDir.resolve("$packagePath/features/connectionstatus/ConnectionStatus.kt")

            assertTrue(Files.exists(featuresFile), "Expected generated Features file at: $featuresFile")
            assertTrue(Files.exists(spot1FeatureFile), "Expected generated Spot1 feature file at: $spot1FeatureFile")
            assertTrue(Files.exists(connectionStatusFeatureFile), "Expected generated ConnectionStatus feature file at: $connectionStatusFeatureFile")

            val featuresContent = Files.readString(featuresFile)
            val spot1Content = Files.readString(spot1FeatureFile)
            val connectionStatusContent = Files.readString(connectionStatusFeatureFile)

            // The deprecated submodel feature class should have @Deprecated annotation
            assertTrue(
                spot1Content.contains("@Deprecated("),
                "Expected Spot1 feature class to have @Deprecated annotation. Generated content:\n$spot1Content"
            )
            assertTrue(
                spot1Content.contains("Will be removed in version 2.0.0."),
                "Expected Spot1 @Deprecated annotation to contain removal version. Generated content:\n$spot1Content"
            )

            // The deprecated feature property in Features class should have @Deprecated annotation
            assertTrue(
                Regex("(?s)@Deprecated\\(.*2\\.0\\.0.*\\)\\s*(public\\s+)?(override\\s+)?var spot1")
                    .containsMatchIn(featuresContent),
                "Expected spot1 property in Features to be annotated as deprecated. Generated content:\n$featuresContent"
            )

            // The deprecated feature DSL function in Features class should have @Deprecated annotation
            assertTrue(
                Regex("(?s)@Deprecated\\(.*2\\.0\\.0.*\\)\\s*(public\\s+)?(suspend\\s+)?fun spot1\\(")
                    .containsMatchIn(featuresContent),
                "Expected spot1 DSL builder function in Features to be annotated as deprecated. Generated content:\n$featuresContent"
            )

            // The non-deprecated feature should NOT have @Deprecated annotation
            assertTrue(
                !connectionStatusContent.contains("@Deprecated("),
                "Expected ConnectionStatus feature class to NOT have @Deprecated annotation. Generated content:\n$connectionStatusContent"
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
