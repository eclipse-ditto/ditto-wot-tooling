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
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.model.ThingModel
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmodelOnlyGenerationTest {

    @Test
    fun `submodel-only generation creates standalone feature package with configured feature name`() = runBlocking {
        val outputDir = Files.createTempDirectory("wot-kotlin-submodel-test")
        try {
            val configuredFeatureName = "temp-thermostat"
            val generatedClassName = "TempThermostat"
            val thingModel = ThingModel.fromJson(
                JsonObject.of(
                    """
                    {
                      "@context": "https://www.w3.org/2022/wot/td/v1.1",
                      "@type": "tm:ThingModel",
                      "title": "Room Thermostat",
                      "version": { "model": "1.0.0" },
                      "properties": {
                        "status": {
                          "title": "Status",
                          "type": "object",
                          "ditto:category": "status",
                          "properties": {
                            "enabled": { "type": "boolean" }
                          }
                        }
                      },
                      "actions": {
                        "reboot": {
                          "title": "Reboot",
                          "input": { "type": "boolean" }
                        }
                      }
                    }
                    """.trimIndent()
                )
            )
            val outputPackage = "org.eclipse.ditto.wot.kotlin.generator.plugin.submodeltest"

            ThingModelGenerator.generate(
                thingModel,
                GeneratorConfiguration(
                    thingModelUrl = "in-memory",
                    outputPackage = outputPackage,
                    outputDirectory = outputDir.toFile(),
                    classNamingStrategy = ClassNamingStrategy.ORIGINAL_THEN_COMPOUND,
                    submodelOnly = true,
                    featureName = configuredFeatureName
                )
            )

            val packagePath = outputPackage.replace('.', '/')
            val thingWrapper = outputDir.resolve("$packagePath/${generatedClassName}Thing.kt")
            val featuresWrapper = outputDir.resolve("$packagePath/features/${generatedClassName}Features.kt")
            val categoryMarker = outputDir.resolve("$packagePath/features/StatusCategory.kt")
            val featureClass = outputDir.resolve(
                "$packagePath/features/$configuredFeatureName/$generatedClassName.kt"
            )
            val propertiesClass = outputDir.resolve(
                "$packagePath/features/$configuredFeatureName/${generatedClassName}Properties.kt"
            )
            val actionClass = outputDir.resolve(
                "$packagePath/features/$configuredFeatureName/actions/Reboot.kt"
            )

            assertTrue(Files.exists(thingWrapper), "Expected standalone Thing wrapper to be generated")
            assertTrue(Files.exists(featuresWrapper), "Expected single-feature Features wrapper to be generated")
            assertTrue(Files.exists(categoryMarker), "Expected category marker class to be generated")
            assertTrue(Files.exists(featureClass), "Expected feature class to be generated")
            assertTrue(Files.exists(propertiesClass), "Expected feature properties class to be generated")
            assertTrue(Files.exists(actionClass), "Expected action class to be generated")

            val thingWrapperContent = Files.readString(thingWrapper)
            val featuresWrapperContent = Files.readString(featuresWrapper)
            val featureClassContent = Files.readString(featureClass)
            val propertiesClassContent = Files.readString(propertiesClass)

            assertTrue(
                thingWrapperContent.contains("class ${generatedClassName}Thing"),
                "Expected standalone Thing wrapper class name"
            )
            assertTrue(
                featuresWrapperContent.contains("@JsonSetter(\"$configuredFeatureName\")"),
                "Expected Features wrapper to use configured featureName as JSON key"
            )
            assertTrue(
                featuresWrapperContent.contains("override fun startPath(): String = \"features\""),
                "Expected single-feature wrapper to expose the features path"
            )
            assertTrue(
                featureClassContent.contains("const val FEATURE_NAME: String = \"$configuredFeatureName\""),
                "Expected feature class to use configured featureName"
            )
            assertTrue(
                featureClassContent.contains("override fun startPath(): String = \"$configuredFeatureName\""),
                "Expected feature class path to use configured featureName"
            )
            assertTrue(
                propertiesClassContent.contains("override fun startPath(): String = \"$configuredFeatureName\""),
                "Expected properties wrapper path to use configured featureName"
            )
            assertFalse(
                thingWrapperContent.contains("fun startPath()"),
                "Thing wrapper intentionally mirrors full-model Thing generation and should not expose a path"
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
