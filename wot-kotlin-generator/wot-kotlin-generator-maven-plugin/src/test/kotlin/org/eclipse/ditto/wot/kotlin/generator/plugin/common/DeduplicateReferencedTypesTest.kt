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
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.SharedTypeRegistry
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.kotlin.generator.plugin.property.TmRefScanner
import org.eclipse.ditto.wot.model.ThingModel
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeduplicateReferencedTypesTest {

    @BeforeTest
    fun resetState() {
        SharedTypeRegistry.clear()
        SharedTypeRegistry.clearRefMappings()
    }

    @AfterTest
    fun cleanupState() {
        SharedTypeRegistry.clear()
        SharedTypeRegistry.clearRefMappings()
    }

    @Test
    fun `dedup enabled - identical schemas with same name are generated once via schema JSON match`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-dedup-test")
        try {
            // Two top-level properties with identical "status" structure.
            // Without tm:ref, the fallback dedup matches by schema JSON + same class name.
            // Both properties have the same name "status" so the generated class name is the same.
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Dedup Test Device",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "sensorA": {
                          "title": "Sensor A",
                          "type": "object",
                          "properties": {
                            "value": { "type": "number" },
                            "status": {
                              "title": "Status",
                              "type": "object",
                              "properties": {
                                "severity": { "type": "string" },
                                "message": { "type": "string" }
                              },
                              "additionalProperties": false,
                              "required": ["severity"]
                            }
                          }
                        },
                        "sensorB": {
                          "title": "Sensor B",
                          "type": "object",
                          "properties": {
                            "reading": { "type": "integer" },
                            "status": {
                              "title": "Status",
                              "type": "object",
                              "properties": {
                                "severity": { "type": "string" },
                                "message": { "type": "string" }
                              },
                              "additionalProperties": false,
                              "required": ["severity"]
                            }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.deduptest"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile(),
                    deduplicateReferencedTypes = true,
                    classNamingStrategy = org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
                )
            )

            val allFiles = Files.walk(outputDir)
                .filter { it.toString().endsWith(".kt") }
                .map { outputDir.relativize(it).toString() }
                .toList()

            // "Status" class should exist once — both sensorA and sensorB have
            // identical "status" schemas with the same name "Status" (ORIGINAL_THEN_COMPOUND
            // uses the original title), so dedup matches by same name + same structure.
            val statusFiles = allFiles.filter { it.endsWith("Status.kt") }
            assertEquals(
                1, statusFiles.size,
                "Expected exactly one Status file with dedup enabled (same name + same structure). Found: $statusFiles"
            )

            // SensorB should reference SensorA's Status via import
            val sensorBFile = allFiles.first { it.contains("SensorB.kt") }
            val sensorBContent = Files.readString(outputDir.resolve(sensorBFile))
            assertTrue(
                sensorBContent.contains("Status"),
                "SensorB should reference Status type"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `dedup enabled - different schemas with same name are NOT deduplicated`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-dedup-diff-test")
        try {
            // Two features with DIFFERENT "info" objects (different properties)
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Different Schema Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "featureA": {
                          "title": "Feature A",
                          "type": "object",
                          "properties": {
                            "info": {
                              "title": "Info",
                              "type": "object",
                              "properties": {
                                "level": { "type": "string" },
                                "code": { "type": "integer" }
                              }
                            }
                          }
                        },
                        "featureB": {
                          "title": "Feature B",
                          "type": "object",
                          "properties": {
                            "info": {
                              "title": "Info",
                              "type": "object",
                              "properties": {
                                "level": { "type": "string" },
                                "description": { "type": "string" }
                              }
                            }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.deduptestdiff"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile(),
                    deduplicateReferencedTypes = true
                )
            )

            val allFiles = Files.walk(outputDir)
                .filter { it.toString().endsWith(".kt") }
                .map { outputDir.relativize(it).toString() }
                .toList()

            // Both should exist since they have different structures
            val infoFiles = allFiles.filter { it.contains("Info") && !it.contains("SeverityInfo") }
            assertTrue(
                infoFiles.size >= 2,
                "Expected at least 2 Info files for different schemas. Found: $infoFiles"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `dedup disabled - all types generated in their own packages even if identical`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-no-dedup-test")
        try {
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "No Dedup Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "featureA": {
                          "title": "Feature A",
                          "type": "object",
                          "properties": {
                            "info": {
                              "title": "Info",
                              "type": "object",
                              "properties": {
                                "level": { "type": "string" }
                              }
                            }
                          }
                        },
                        "featureB": {
                          "title": "Feature B",
                          "type": "object",
                          "properties": {
                            "info": {
                              "title": "Info",
                              "type": "object",
                              "properties": {
                                "level": { "type": "string" }
                              }
                            }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.nodeduptest"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile(),
                    deduplicateReferencedTypes = false
                )
            )

            val allFiles = Files.walk(outputDir)
                .filter { it.toString().endsWith(".kt") }
                .map { outputDir.relativize(it).toString() }
                .toList()

            // With dedup disabled, both features get their own Info class
            val infoFiles = allFiles.filter { it.endsWith("Info.kt") }
            assertEquals(
                2, infoFiles.size,
                "Expected 2 Info files without dedup (one per feature). Found: $infoFiles"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `dedup enabled - tm-ref fingerprint path deduplicates across different parent names`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-tmref-dedup-test")
        try {
            // Two features with identical "details" schemas but different parent contexts.
            // With ORIGINAL_THEN_COMPOUND, the class name stays "Details" for both.
            // We simulate the tm:ref path by pre-registering a fingerprint in SharedTypeRegistry.
            val detailsSchema = """
                {
                  "title": "Details",
                  "type": "object",
                  "properties": {
                    "code": { "type": "integer" },
                    "label": { "type": "string" }
                  },
                  "additionalProperties": false,
                  "required": ["code", "label"]
                }
            """.trimIndent()

            // Compute fingerprint and register it as if TmRefScanner found it
            val fingerprint = TmRefScanner.computeFingerprint(
                org.eclipse.ditto.json.JsonObject.of(detailsSchema)
            )
            val fakeRefUrl = "https://example.org/common/details-1.0.0.tm.jsonld#/properties/details"
            SharedTypeRegistry.registerRefMapping(fingerprint, fakeRefUrl)

            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "TmRef Dedup Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "sensorA": {
                          "title": "Sensor A",
                          "type": "object",
                          "properties": {
                            "details": $detailsSchema,
                            "reading": { "type": "number" }
                          }
                        },
                        "sensorB": {
                          "title": "Sensor B",
                          "type": "object",
                          "properties": {
                            "details": $detailsSchema,
                            "value": { "type": "integer" }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.tmrefdeduptest"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile(),
                    deduplicateReferencedTypes = true,
                    classNamingStrategy = org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
                )
            )

            val allFiles = Files.walk(outputDir)
                .filter { it.toString().endsWith(".kt") }
                .map { outputDir.relativize(it).toString() }
                .toList()

            val detailsFiles = allFiles.filter { it.endsWith("Details.kt") }

            // The tm:ref fingerprint match should cause dedup even though the schemas
            // appear in different parent contexts. Only ONE Details.kt should exist.
            assertEquals(
                1, detailsFiles.size,
                "Expected exactly one Details file via tm:ref fingerprint dedup. Found: $detailsFiles"
            )

            // Verify the tm:ref URL was registered in classRefRegistry
            val registeredClass = SharedTypeRegistry.findClassByRef(fakeRefUrl)
            assertTrue(
                registeredClass != null,
                "Expected the generated class to be registered against the tm:ref URL"
            )
            assertTrue(
                registeredClass!!.simpleName == "Details",
                "Expected registered class to be named 'Details', got: ${registeredClass.simpleName}"
            )
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `dedup preserves original JSON property name in startPath when class name differs`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-startpath-test")
        try {
            // Pre-register a multi-referenced tm:ref with title "Day" to trigger Option B naming.
            // The property name "monday" should still be used for startPath().
            val daySchema = """
                {
                  "title": "Day",
                  "type": "object",
                  "properties": {
                    "transitions": {
                      "type": "array",
                      "items": { "type": "string" }
                    }
                  },
                  "additionalProperties": false
                }
            """.trimIndent()

            val fingerprint = TmRefScanner.computeFingerprint(JsonObject.of(daySchema))
            val fakeRefUrl = "https://example.org/day.tm.jsonld#/properties/day"
            SharedTypeRegistry.registerRefMapping(fingerprint, fakeRefUrl)
            SharedTypeRegistry.registerRefTitle(fakeRefUrl, "Day")
            // Simulate multi-reference (7 days)
            repeat(7) { SharedTypeRegistry.incrementRefCount(fakeRefUrl) }
            SharedTypeRegistry.markConflictingTitles()

            val thingModel = ThingModel.fromJson(
                JsonObject.of("""
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "StartPath Test",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "schedule": {
                          "title": "Schedule",
                          "type": "object",
                          "properties": {
                            "monday": $daySchema,
                            "tuesday": $daySchema,
                            "wednesday": $daySchema
                          }
                        }
                      }
                    }
                """.trimIndent())
            )

            val outputPackage = "org.eclipse.ditto.wot.test.startpath"
            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile(),
                    deduplicateReferencedTypes = true,
                    classNamingStrategy = org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
                )
            )

            val allFiles = listAllKtFiles(outputDir)

            // Class should be named "Day" (from tm:ref title), not "Monday"
            val dayFiles = allFiles.filter { it.endsWith("Day.kt") }
            assertTrue(dayFiles.isNotEmpty(), "Expected Day.kt to be generated. Files: $allFiles")

            val mondayFiles = allFiles.filter { it.endsWith("Monday.kt") }
            assertTrue(mondayFiles.isEmpty(), "Monday.kt should NOT exist — dedup should use 'Day' from tm:ref title. Files: $allFiles")

            // startPath() should be "monday" (the original JSON property name), not "Day"
            val dayContent = Files.readString(outputDir.resolve(dayFiles.first()))
            assertTrue(
                dayContent.contains(""""monday""""),
                "Day.kt startPath should use original JSON property name 'monday', not class name. Content snippet: ${dayContent.take(500)}"
            )

            // Schedule should reference Day for all 3 properties
            val scheduleFile = allFiles.first { it.contains("Schedule.kt") }
            val scheduleContent = Files.readString(outputDir.resolve(scheduleFile))
            assertTrue(scheduleContent.contains("Day?"), "Schedule should reference Day type for day properties")
        } finally {
            deleteRecursively(outputDir)
        }
    }

    @Test
    fun `consecutive generate calls without manual clearing produce correct output`() = runBlocking {
        val outputDirA = Files.createTempDirectory("wot-kotlin-consecutive-a")
        val outputDirB = Files.createTempDirectory("wot-kotlin-consecutive-b")
        val outputDirBalone = Files.createTempDirectory("wot-kotlin-consecutive-b-alone")
        try {
            val modelA = ThingModel.fromJson(JsonObject.of("""
                {
                  "@context": "https://www.w3.org/2022/wot/td/v1.1",
                  "@type": "tm:ThingModel",
                  "title": "Device A",
                  "version": { "model": "1.0.0" },
                  "properties": {
                    "status": {
                      "title": "Status",
                      "type": "object",
                      "properties": {
                        "level": { "type": "string", "enum": ["OK", "ERROR"] },
                        "code": { "type": "integer" }
                      }
                    }
                  }
                }
            """.trimIndent()))

            val modelB = ThingModel.fromJson(JsonObject.of("""
                {
                  "@context": "https://www.w3.org/2022/wot/td/v1.1",
                  "@type": "tm:ThingModel",
                  "title": "Device B",
                  "version": { "model": "1.0.0" },
                  "properties": {
                    "status": {
                      "title": "Status",
                      "type": "object",
                      "properties": {
                        "mode": { "type": "string", "enum": ["FAST", "SLOW"] },
                        "active": { "type": "boolean" }
                      }
                    }
                  }
                }
            """.trimIndent()))

            val configA = GeneratorConfiguration(
                thingModelUrl = "in-memory-a",
                outputPackage = "org.eclipse.ditto.wot.test.consecutive.a",
                outputDirectory = outputDirA.toFile(),
                classNamingStrategy = org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
            )
            val configB = GeneratorConfiguration(
                thingModelUrl = "in-memory-b",
                outputPackage = "org.eclipse.ditto.wot.test.consecutive.b",
                outputDirectory = outputDirB.toFile(),
                classNamingStrategy = org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
            )
            val configBalone = GeneratorConfiguration(
                thingModelUrl = "in-memory-b",
                outputPackage = "org.eclipse.ditto.wot.test.consecutive.b",
                outputDirectory = outputDirBalone.toFile(),
                classNamingStrategy = org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy.ORIGINAL_THEN_COMPOUND
            )

            // Generate A then B consecutively (without manual clearing)
            ThingModelGenerator.generate(modelA, configA)
            ThingModelGenerator.generate(modelB, configB)

            // Generate B alone (fresh state)
            SharedTypeRegistry.clear()
            SharedTypeRegistry.clearRefMappings()
            ThingModelGenerator.generate(modelB, configBalone)

            // B consecutive output should match B alone output
            val filesB = listAllKtFiles(outputDirB).sorted()
            val filesBalone = listAllKtFiles(outputDirBalone).sorted()

            assertEquals(filesBalone.map { it.substringAfterLast("/") }, filesB.map { it.substringAfterLast("/") },
                "File names should be identical whether B is generated alone or after A")

            // Verify B's Status has the right enum (FAST/SLOW not OK/ERROR from A)
            val statusFileB = filesB.first { it.contains("Status.kt") }
            val statusContent = Files.readString(outputDirB.resolve(statusFileB))
            assertTrue(statusContent.contains("FAST"), "Model B Status should have FAST enum, not leaked from model A")
            assertTrue(statusContent.contains("SLOW"), "Model B Status should have SLOW enum")
        } finally {
            deleteRecursively(outputDirA)
            deleteRecursively(outputDirB)
            deleteRecursively(outputDirBalone)
        }
    }

    private fun listAllKtFiles(dir: Path): List<String> {
        return Files.walk(dir)
            .filter { it.toString().endsWith(".kt") }
            .map { dir.relativize(it).toString() }
            .toList()
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }
}
