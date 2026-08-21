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

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.features.FeaturesPathsGenerator
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesiredPropertiesPathsTest {

    @Test
    fun `property without ditto desired keeps PUT and PATCH on the properties path`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "audio": {
                  "title": "Audio",
                  "type": "number",
                  "readOnly": false
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val propertyPath = paths["/{thingId}/features/RCP/properties/audio"]
        assertNotNull(propertyPath?.get)
        assertNotNull(propertyPath?.put)
        assertNotNull(propertyPath?.patch)
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/audio"])
    }

    @Test
    fun `property with ditto desired enabled moves PUT and PATCH to the desiredProperties path`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "roomTemperature": {
                  "title": "Room Temperature",
                  "type": "number",
                  "readOnly": false,
                  "ditto:desired": {}
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val propertyPath = paths["/{thingId}/features/RCP/properties/roomTemperature"]
        assertNotNull(propertyPath?.get)
        assertNull(propertyPath?.put)
        assertNull(propertyPath?.patch)

        val desiredPath = paths["/{thingId}/features/RCP/desiredProperties/roomTemperature"]
        assertNotNull(desiredPath?.get)
        assertNotNull(desiredPath?.put)
        assertNotNull(desiredPath?.patch)
    }

    @Test
    fun `property with ditto desired enabled false behaves like absent`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "audio": {
                  "title": "Audio",
                  "type": "number",
                  "readOnly": false,
                  "ditto:desired": {
                    "enabled": false
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val propertyPath = paths["/{thingId}/features/RCP/properties/audio"]
        assertNotNull(propertyPath?.put)
        assertNotNull(propertyPath?.patch)
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/audio"])
    }

    @Test
    fun `readOnly property with ditto desired enabled generates no writes at all`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "targetTemp": {
                  "title": "Target Temperature",
                  "type": "number",
                  "readOnly": true,
                  "ditto:desired": {
                    "enabled": true
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val propertyPath = paths["/{thingId}/features/RCP/properties/targetTemp"]
        assertNotNull(propertyPath?.get)
        assertNull(propertyPath?.put)
        assertNull(propertyPath?.patch)
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/targetTemp"])
    }

    @Test
    fun `desiredProperties category rollup is created lazily only when a member property has desired enabled`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "roomTemperature": {
                  "title": "Room Temperature",
                  "type": "number",
                  "readOnly": false,
                  "ditto:category": "configuration",
                  "ditto:desired": {}
                },
                "roomHumidity": {
                  "title": "Room Humidity",
                  "type": "number",
                  "readOnly": false,
                  "ditto:category": "configuration"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        assertNotNull(paths["/{thingId}/features/RCP/properties/configuration"])
        assertNotNull(paths["/{thingId}/features/RCP/desiredProperties/configuration"])

        assertNotNull(paths["/{thingId}/features/RCP/desiredProperties/configuration/roomTemperature"])
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/configuration/roomHumidity"])

        // roomHumidity is not desired-enabled, so it keeps its writes on the properties path
        val humidityPropertyPath = paths["/{thingId}/features/RCP/properties/configuration/roomHumidity"]
        assertNotNull(humidityPropertyPath?.put)
        assertNotNull(humidityPropertyPath?.patch)
    }

    private fun thingModelFromJson(json: String): ThingModel =
        ThingModel.fromJson(JsonObject.of(json))

    private fun openApi() = OpenAPI().components(Components().schemas(mutableMapOf()))
}
