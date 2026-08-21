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

class NestedPropertyPathsTest {

    @Test
    fun `nested property with ditto desired enabled gets GET on properties and GET PUT PATCH on desiredProperties`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "system-settings": {
                  "type": "object",
                  "title": "System Settings",
                  "readOnly": false,
                  "additionalProperties": true,
                  "properties": {
                    "CONF_UNIT_NAME": {
                      "type": "string",
                      "title": "Unit Name",
                      "readOnly": false,
                      "ditto:desired": {
                        "enabled": true
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val nestedPropertyPath = paths["/{thingId}/features/RCP/properties/system-settings/CONF_UNIT_NAME"]
        assertNotNull(nestedPropertyPath?.get)
        assertNull(nestedPropertyPath?.put)
        assertNull(nestedPropertyPath?.patch)

        val nestedDesiredPath = paths["/{thingId}/features/RCP/desiredProperties/system-settings/CONF_UNIT_NAME"]
        assertNotNull(nestedDesiredPath?.get)
        assertNotNull(nestedDesiredPath?.put)
        assertNotNull(nestedDesiredPath?.patch)

        // the parent's own whole-object path must remain completely unaffected
        val parentPath = paths["/{thingId}/features/RCP/properties/system-settings"]
        assertNotNull(parentPath?.get)
        assertNotNull(parentPath?.put)
        assertNotNull(parentPath?.patch)
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/system-settings"])
    }

    @Test
    fun `nested property without ditto desired still gets its own GET and keeps writes on the properties path`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "system-settings": {
                  "type": "object",
                  "title": "System Settings",
                  "readOnly": false,
                  "properties": {
                    "CONF_UNIT_ID": {
                      "type": "string",
                      "title": "Unit Id",
                      "readOnly": false
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val nestedPropertyPath = paths["/{thingId}/features/RCP/properties/system-settings/CONF_UNIT_ID"]
        assertNotNull(nestedPropertyPath?.get)
        assertNotNull(nestedPropertyPath?.put)
        assertNotNull(nestedPropertyPath?.patch)
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/system-settings/CONF_UNIT_ID"])
    }

    @Test
    fun `readOnly nested property with ditto desired enabled generates no writes at all`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "system-settings": {
                  "type": "object",
                  "title": "System Settings",
                  "readOnly": false,
                  "properties": {
                    "CONF_SERIAL_NUMBER": {
                      "type": "string",
                      "title": "Serial Number",
                      "readOnly": true,
                      "ditto:desired": {
                        "enabled": true
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val nestedPropertyPath = paths["/{thingId}/features/RCP/properties/system-settings/CONF_SERIAL_NUMBER"]
        assertNotNull(nestedPropertyPath?.get)
        assertNull(nestedPropertyPath?.put)
        assertNull(nestedPropertyPath?.patch)
        assertNull(paths["/{thingId}/features/RCP/desiredProperties/system-settings/CONF_SERIAL_NUMBER"])
    }

    @Test
    fun `arbitrary depth recursion exposes doubly nested object properties`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "RCP",
              "properties": {
                "system-settings": {
                  "type": "object",
                  "title": "System Settings",
                  "readOnly": false,
                  "properties": {
                    "network": {
                      "type": "object",
                      "title": "Network",
                      "readOnly": false,
                      "properties": {
                        "hostName": {
                          "type": "string",
                          "title": "Host Name",
                          "readOnly": false,
                          "ditto:desired": {
                            "enabled": true
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        FeaturesPathsGenerator.generateFeaturesPaths("RCP", model, paths, openApi())

        val doublyNestedPropertyPath = paths["/{thingId}/features/RCP/properties/system-settings/network/hostName"]
        assertNotNull(doublyNestedPropertyPath?.get)
        assertNull(doublyNestedPropertyPath?.put)

        val doublyNestedDesiredPath = paths["/{thingId}/features/RCP/desiredProperties/system-settings/network/hostName"]
        assertNotNull(doublyNestedDesiredPath?.get)
        assertNotNull(doublyNestedDesiredPath?.put)
        assertNotNull(doublyNestedDesiredPath?.patch)

        // the intermediate "network" object itself gets no dedicated path of its own beyond its GET/PUT/PATCH
        val networkPath = paths["/{thingId}/features/RCP/properties/system-settings/network"]
        assertNotNull(networkPath?.get)
        assertNotNull(networkPath?.put)
        assertNotNull(networkPath?.patch)
    }

    private fun thingModelFromJson(json: String): ThingModel =
        ThingModel.fromJson(JsonObject.of(json))

    private fun openApi() = OpenAPI().components(Components().schemas(mutableMapOf()))
}
