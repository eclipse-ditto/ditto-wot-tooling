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
package org.eclipse.ditto.wot.openapi.generator

import io.swagger.v3.core.util.ObjectMapperFactory
import io.swagger.v3.core.util.Yaml31
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.info.Info
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.features.FeaturesPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.thing.AttributeSchemaResolver
import org.eclipse.ditto.wot.openapi.generator.thing.AttributesPathsGenerator
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ConstPropagationYamlOutputTest {

    @Test
    fun `generate YAML for temperature sensor model and verify const in output`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Temperature Sensor",
              "description": "A generic temperature sensor device.",
              "properties": {
                "type": {
                  "title": "Type",
                  "type": "string",
                  "const": "8"
                },
                "sensorClass": {
                  "title": "Sensor Class",
                  "type": "integer",
                  "const": 8
                },
                "manufacturer": {
                  "title": "Manufacturer",
                  "type": "string",
                  "const": "ACME"
                }
              }
            }
            """.trimIndent()
        )

        val yaml = generateYaml(model, "temperatureSensor")

        assertContains(yaml, "const: \"8\"")
        assertContains(yaml, "const: 8")
        assertContains(yaml, "const: ACME")

        // Verify no redundant enum fallback is emitted alongside const
        val constSchemaBlocks = yaml.split("const:").drop(1)
        constSchemaBlocks.forEach { block ->
            val nextLines = block.lines().take(3).joinToString("\n")
            assertFalse(nextLines.contains("enum:"), "enum should not appear alongside const in YAML output")
        }
    }

    @Test
    fun `generate YAML for humidity sensor model and verify const in output`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Humidity Sensor",
              "description": "A generic humidity sensor device.",
              "properties": {
                "type": {
                  "title": "Type",
                  "type": "string",
                  "const": "26"
                },
                "sensorClass": {
                  "title": "Sensor Class",
                  "type": "integer",
                  "const": 26
                },
                "manufacturer": {
                  "title": "Manufacturer",
                  "type": "string",
                  "const": "ExampleCorp"
                }
              }
            }
            """.trimIndent()
        )

        val yaml = generateYaml(model, "humiditySensor")

        assertContains(yaml, "const: \"26\"")
        assertContains(yaml, "const: 26")
        assertContains(yaml, "const: ExampleCorp")

        // Verify no redundant enum fallback is emitted alongside const
        val constSchemaBlocks = yaml.split("const:").drop(1)
        constSchemaBlocks.forEach { block ->
            val nextLines = block.lines().take(3).joinToString("\n")
            assertFalse(nextLines.contains("enum:"), "enum should not appear alongside const in YAML output")
        }
    }

    private fun generateYaml(model: ThingModel, featureName: String): String {
        val openAPI = OpenAPI(SpecVersion.V31)
            .openapi("3.1.0")
            .info(
                Info()
                    .title(model.title.get().toString())
                    .description(model.description.orElse(null)?.toString())
            )
        openAPI.components(Components().schemas(mutableMapOf()))

        val paths = Paths()

        // Generate thing attribute paths
        AttributesPathsGenerator.generateThingAttributesPaths(model, paths, openAPI)
        openAPI.schema("attributes", AttributeSchemaResolver.provideAttributesSchema(model, openAPI))

        // Generate feature paths
        FeaturesPathsGenerator.generateFeaturesPaths(featureName, model, paths, openAPI)

        openAPI.paths(paths)

        val writer = StringWriter()
        val jsonGenerator = ObjectMapperFactory.createYaml31().createGenerator(writer)
        Yaml31.mapper().writeValue(jsonGenerator, openAPI)
        return writer.toString()
    }

    private fun thingModelFromJson(json: String): ThingModel =
        ThingModel.fromJson(JsonObject.of(json))
}
