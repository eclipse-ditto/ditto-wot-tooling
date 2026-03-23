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
import kotlin.test.assertFalse

class ActionOutputEnumGenerationTest {

    @Test
    fun `string enums in action output are generated as enum classes not strings`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-action-enum-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Weather Station",
                      "version": { "model": "1.0.0" },
                      "properties": {},
                      "actions": {
                        "evaluateConditions": {
                          "title": "Evaluate conditions",
                          "description": "Evaluate current weather conditions",
                          "output": {
                            "title": "Conditions evaluation result",
                            "type": "object",
                            "properties": {
                              "severity": {
                                "title": "Severity",
                                "type": "string",
                                "enum": ["LOW", "MODERATE", "HIGH"]
                              },
                              "windCategory": {
                                "title": "Wind category",
                                "type": "string",
                                "enum": ["CALM", "BREEZY", "WINDY", "STORMY"]
                              },
                              "summary": {
                                "title": "Summary",
                                "type": "string"
                              }
                            },
                            "required": ["severity", "windCategory"]
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.actionenumtest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val outputFile = outputDir.resolve("$packagePath/actions/EvaluateConditionsOutput.kt")

            assertTrue(Files.exists(outputFile), "Expected generated output file at: $outputFile")

            val content = Files.readString(outputFile)

            // The severity field should NOT be of type String — it should be an enum
            assertFalse(
                Regex("""val severity:\s*String""").containsMatchIn(content),
                "severity should NOT be of type String, it should be an enum type. Generated content:\n$content"
            )

            // The windCategory field should NOT be of type String — it should be an enum
            assertFalse(
                Regex("""val windCategory:\s*String""").containsMatchIn(content),
                "windCategory should NOT be of type String, it should be an enum type. Generated content:\n$content"
            )

            // The summary field SHOULD still be of type String (no enum)
            assertTrue(
                Regex("""val summary:\s*String\?""").containsMatchIn(content),
                "summary should be of type String?. Generated content:\n$content"
            )

            // Verify enum classes are present (inline strategy nests them in the data class)
            assertTrue(
                content.contains("enum class Severity"),
                "Expected Severity enum class to be nested in data class. Generated content:\n$content"
            )

            assertTrue(
                content.contains("enum class WindCategory"),
                "Expected WindCategory enum class to be nested in data class. Generated content:\n$content"
            )

            // Verify enum constants are present
            assertTrue(content.contains("LOW"), "Expected LOW enum constant. Generated content:\n$content")
            assertTrue(content.contains("MODERATE"), "Expected MODERATE enum constant. Generated content:\n$content")
            assertTrue(content.contains("HIGH"), "Expected HIGH enum constant. Generated content:\n$content")
            assertTrue(content.contains("CALM"), "Expected CALM enum constant. Generated content:\n$content")
            assertTrue(content.contains("BREEZY"), "Expected BREEZY enum constant. Generated content:\n$content")
            assertTrue(content.contains("WINDY"), "Expected WINDY enum constant. Generated content:\n$content")
            assertTrue(content.contains("STORMY"), "Expected STORMY enum constant. Generated content:\n$content")

        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `enums in action input are generated as enum classes not strings`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-action-input-enum-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Traffic Light Controller",
                      "version": { "model": "1.0.0" },
                      "properties": {},
                      "actions": {
                        "setSignal": {
                          "title": "Set signal",
                          "input": {
                            "title": "Signal input",
                            "type": "object",
                            "properties": {
                              "color": {
                                "title": "Color",
                                "type": "string",
                                "enum": ["RED", "YELLOW", "GREEN"]
                              },
                              "note": {
                                "title": "Note",
                                "type": "string"
                              }
                            },
                            "required": ["color"]
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.actioninputenumtest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val inputFile = outputDir.resolve("$packagePath/actions/SetSignalInput.kt")

            assertTrue(Files.exists(inputFile), "Expected generated input file at: $inputFile")

            val content = Files.readString(inputFile)

            // color should NOT be String
            assertFalse(
                Regex("""val color:\s*String""").containsMatchIn(content),
                "color should NOT be of type String, it should be an enum type. Generated content:\n$content"
            )

            // note SHOULD be String
            assertTrue(
                Regex("""val note:\s*String\?""").containsMatchIn(content),
                "note should be of type String?. Generated content:\n$content"
            )

            // Verify enum class is nested
            assertTrue(
                content.contains("enum class Color"),
                "Expected Color enum class. Generated content:\n$content"
            )

            // Verify enum constants
            assertTrue(content.contains("RED"), "Expected RED enum constant")
            assertTrue(content.contains("YELLOW"), "Expected YELLOW enum constant")
            assertTrue(content.contains("GREEN"), "Expected GREEN enum constant")

        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `integer enums in action output are generated as enum classes`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-action-int-enum-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Robot Arm Controller",
                      "version": { "model": "1.0.0" },
                      "properties": {},
                      "actions": {
                        "diagnose": {
                          "title": "Diagnose",
                          "output": {
                            "title": "Diagnostic result",
                            "type": "object",
                            "properties": {
                              "faultCode": {
                                "title": "Fault code",
                                "type": "integer",
                                "enum": [0, 1, 2, 3]
                              },
                              "description": {
                                "title": "Description",
                                "type": "string"
                              }
                            },
                            "required": ["faultCode"]
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.actionintenumtest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val outputFile = outputDir.resolve("$packagePath/actions/DiagnoseOutput.kt")

            assertTrue(Files.exists(outputFile), "Expected generated output file at: $outputFile")

            val content = Files.readString(outputFile)

            // faultCode should NOT be Long — it should be an enum
            assertFalse(
                Regex("""val faultCode:\s*Long""").containsMatchIn(content),
                "faultCode should NOT be of type Long, it should be an enum type. Generated content:\n$content"
            )

            // description SHOULD be String
            assertTrue(
                Regex("""val description:\s*String\?""").containsMatchIn(content),
                "description should be of type String?. Generated content:\n$content"
            )

            // Verify enum class is generated
            assertTrue(
                content.contains("enum class FaultCode"),
                "Expected FaultCode enum class. Generated content:\n$content"
            )

        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `enum items in arrays within action output are generated as enum classes`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-action-array-enum-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Smart Oven",
                      "version": { "model": "1.0.0" },
                      "properties": {},
                      "actions": {
                        "queryPrograms": {
                          "title": "Query programs",
                          "output": {
                            "title": "Programs response",
                            "type": "object",
                            "properties": {
                              "availablePrograms": {
                                "title": "Available programs",
                                "type": "array",
                                "items": {
                                  "type": "string",
                                  "enum": ["BAKE", "BROIL", "CONVECTION", "DEFROST"]
                                }
                              },
                              "labels": {
                                "title": "Labels",
                                "type": "array",
                                "items": {
                                  "type": "string"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.actionarrayenumtest"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile()
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val outputFile = outputDir.resolve("$packagePath/actions/QueryProgramsOutput.kt")

            assertTrue(Files.exists(outputFile), "Expected generated output file at: $outputFile")

            val content = Files.readString(outputFile)

            // availablePrograms should NOT be List<String> — items are enums
            assertFalse(
                Regex("""availablePrograms:\s*List<String>""").containsMatchIn(content),
                "availablePrograms should NOT be List<String>, items should be enum types. Generated content:\n$content"
            )

            // availablePrograms should be List<AvailableProgramsItem> (enum type)
            assertTrue(
                Regex("""availablePrograms:\s*List<AvailableProgramsItem>""").containsMatchIn(content),
                "availablePrograms should be List<AvailableProgramsItem>. Generated content:\n$content"
            )

            // labels SHOULD be List<String> (no enum on items)
            assertTrue(
                Regex("""labels:\s*List<String>""").containsMatchIn(content),
                "labels should be List<String>. Generated content:\n$content"
            )

            // Verify enum class is generated for the array items
            assertTrue(
                content.contains("enum class AvailableProgramsItem"),
                "Expected AvailableProgramsItem enum class. Generated content:\n$content"
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
