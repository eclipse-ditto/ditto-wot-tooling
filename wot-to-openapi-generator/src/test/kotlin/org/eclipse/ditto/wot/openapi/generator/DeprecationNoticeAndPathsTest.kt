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
import org.eclipse.ditto.wot.openapi.generator.features.FeatureActionsPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.features.FeaturesPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.thing.AttributeSchemaResolver
import org.eclipse.ditto.wot.openapi.generator.thing.ActionsPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.thing.AttributesPathsGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeprecationNoticeAndPathsTest {

    @Test
    fun `extract valid deprecation notice and ignore invalid one`() {
        val validJson = JsonObject.of(
            """
            {
              "ditto:deprecationNotice": {
                "deprecated": true,
                "supersededBy": "#/properties/targetTemperature",
                "removalVersion": "2.0.0"
              }
            }
            """.trimIndent()
        )
        val invalidJson = JsonObject.of(
            """
            {
              "ditto:deprecationNotice": {
                "supersededBy": "#/properties/targetTemperature",
                "removalVersion": "2.0.0"
              }
            }
            """.trimIndent()
        )
        val notDeprecatedJson = JsonObject.of(
            """
            {
              "ditto:deprecationNotice": {
                "deprecated": false,
                "supersededBy": "#/properties/targetTemperature",
                "removalVersion": "2.0.0"
              }
            }
            """.trimIndent()
        )

        val validNotice = Utils.extractDeprecationNotice(validJson)
        assertNotNull(validNotice)
        assertTrue(validNotice.deprecated)
        assertEquals("#/properties/targetTemperature", validNotice.supersededBy)
        assertEquals("2.0.0", validNotice.removalVersion)

        val invalidNotice = Utils.extractDeprecationNotice(invalidJson)
        assertNull(invalidNotice)

        val notDeprecatedNotice = Utils.extractDeprecationNotice(notDeprecatedJson)
        assertNull(notDeprecatedNotice)
    }

    @Test
    fun `mark thing attribute and action operations as deprecated when extension says deprecated true`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat",
              "properties": {
                "tempSetpoint": {
                  "title": "Temperature Setpoint",
                  "type": "number",
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/properties/targetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              },
              "actions": {
                "setTemp": {
                  "title": "Set Temperature",
                  "input": { "type": "number" },
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/actions/setTargetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        AttributesPathsGenerator.generateThingAttributesPaths(model, paths, api)
        ActionsPathsGenerator.generateThingActionsPaths(model, paths, api)

        val attributePath = paths["/{thingId}/attributes/tempSetpoint"]
        assertTrue(attributePath?.get?.deprecated == true)
        assertTrue(attributePath?.put?.deprecated == true)
        assertTrue(attributePath?.patch?.deprecated == true)

        val actionPost = paths["/{thingId}/inbox/messages/setTemp"]?.post
        assertTrue(actionPost?.deprecated == true)
    }

    @Test
    fun `mark feature property and action operations as deprecated when extension says deprecated true`() {
        val featureModel = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat Feature",
              "properties": {
                "tempSetpoint": {
                  "title": "Temperature Setpoint",
                  "type": "number",
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/properties/targetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              },
              "actions": {
                "setTemp": {
                  "title": "Set Temperature",
                  "input": { "type": "number" },
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/actions/setTargetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("thermostat", featureModel, paths, api)
        FeatureActionsPathsGenerator.generateFeatureActionsPaths("thermostat", featureModel, paths, api)

        val propertyPath = paths["/{thingId}/features/thermostat/properties/tempSetpoint"]
        assertTrue(propertyPath?.get?.deprecated == true)
        assertTrue(propertyPath?.put?.deprecated == true)
        assertTrue(propertyPath?.patch?.deprecated == true)

        val actionPost = paths["/{thingId}/features/thermostat/inbox/messages/setTemp"]?.post
        assertTrue(actionPost?.deprecated == true)
    }

    @Test
    fun `deprecated field is absent (null) for non-deprecated thing attribute operations`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat",
              "properties": {
                "targetTemperature": {
                  "title": "Target Temperature",
                  "type": "number"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        AttributesPathsGenerator.generateThingAttributesPaths(model, paths, openApi())

        val path = paths["/{thingId}/attributes/targetTemperature"]
        assertNull(path?.get?.deprecated)
        assertNull(path?.put?.deprecated)
        assertNull(path?.patch?.deprecated)
    }

    @Test
    fun `deprecated thing attribute marks response and component schemas deprecated`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat",
              "properties": {
                "tempSetpoint": {
                  "title": "Temperature Setpoint",
                  "type": "number",
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/properties/targetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        AttributesPathsGenerator.generateThingAttributesPaths(model, paths, api)

        val responseSchema = paths["/{thingId}/attributes/tempSetpoint"]
            ?.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertTrue(responseSchema?.deprecated == true)
        api.schema("attributes", AttributeSchemaResolver.provideAttributesSchema(model, api))

        val componentSchema = api.components.schemas["attribute_tempSetpoint"]
        assertNotNull(componentSchema)
        assertTrue(componentSchema.deprecated == true)

        val attributesSchema = api.components.schemas["attributes"]
        val attributeEntrySchema = attributesSchema?.properties?.get("tempSetpoint")
        assertNotNull(attributeEntrySchema)
        assertTrue(attributeEntrySchema.deprecated == true)
    }

    @Test
    fun `response schema is marked deprecated for deprecated feature property`() {
        val featureModel = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat Feature",
              "properties": {
                "tempSetpoint": {
                  "title": "Temperature Setpoint",
                  "type": "number",
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/properties/targetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        FeaturesPathsGenerator.generateFeaturesPaths("thermostat", featureModel, paths, api)

        val responseSchema = paths["/{thingId}/features/thermostat/properties/tempSetpoint"]
            ?.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertTrue(responseSchema?.deprecated == true)
    }

    @Test
    fun `build deprecation description with and without optional fields`() {
        val notice = Utils.DeprecationNotice(
            deprecated = true,
            supersededBy = "#/properties/targetTemperature",
            removalVersion = "2.0.0"
        )
        val text = Utils.buildDeprecationDescription(notice)
        assertNotNull(text)
        assertContains(text, "**Deprecated.**")
        assertContains(text, "`#/properties/targetTemperature`")
        assertContains(text, "2.0.0")

        val minimalText = Utils.buildDeprecationDescription(Utils.DeprecationNotice(deprecated = true))
        assertNotNull(minimalText)
        assertContains(minimalText, "**Deprecated.**")
    }

    @Test
    fun `operation descriptions contain deprecation notice text for deprecated thing interactions`() {
        val model = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Thermostat",
              "properties": {
                "tempSetpoint": {
                  "title": "Temperature Setpoint",
                  "description": "The current setpoint.",
                  "type": "number",
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/properties/targetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              },
              "actions": {
                "setTemp": {
                  "title": "Set Temperature",
                  "description": "Sets the temperature.",
                  "input": { "type": "number" },
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/actions/setTargetTemperature",
                    "removalVersion": "2.0.0"
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        AttributesPathsGenerator.generateThingAttributesPaths(model, paths, api)
        ActionsPathsGenerator.generateThingActionsPaths(model, paths, api)

        val attributeDescription = paths["/{thingId}/attributes/tempSetpoint"]?.get?.description
        assertNotNull(attributeDescription)
        assertContains(attributeDescription, "The current setpoint.")
        assertContains(attributeDescription, "**Deprecated.**")
        assertContains(attributeDescription, "`#/properties/targetTemperature`")
        assertContains(attributeDescription, "2.0.0")

        val actionDescription = paths["/{thingId}/inbox/messages/setTemp"]?.post?.description
        assertNotNull(actionDescription)
        assertContains(actionDescription, "Sets the temperature.")
        assertContains(actionDescription, "**Deprecated.**")
        assertContains(actionDescription, "`#/actions/setTargetTemperature`")
        assertContains(actionDescription, "2.0.0")
    }

    @Test
    fun `deprecated submodel link marks all feature operations as deprecated`() {
        val featureModel = thingModelFromJson(
            """
            {
              "@context": [
                "https://www.w3.org/2022/wot/td/v1.1",
                { "ditto": "https://ditto.eclipseprojects.io/wot/ditto-extension#" }
              ],
              "@type": "tm:ThingModel",
              "title": "Battery",
              "properties": {
                "voltage": {
                  "title": "Voltage",
                  "type": "number",
                  "ditto:category": "status"
                }
              },
              "actions": {
                "calibrate": {
                  "title": "Calibrate",
                  "input": { "type": "string" }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        val submodelDeprecation = Utils.DeprecationNotice(deprecated = true, removalVersion = "2.0.0")

        FeaturesPathsGenerator.generateFeaturesPaths("battery", featureModel, paths, api, submodelDeprecation)
        FeatureActionsPathsGenerator.generateFeatureActionsPaths("battery", featureModel, paths, api, submodelDeprecation)

        // Feature-level GET should be deprecated
        val featurePath = paths["/{thingId}/features/battery"]
        assertTrue(featurePath?.get?.deprecated == true)
        assertNotNull(featurePath?.get?.description)
        assertContains(featurePath!!.get.description, "**Deprecated.**")
        assertContains(featurePath.get.description, "2.0.0")

        // Category path should inherit submodel deprecation
        val categoryPath = paths["/{thingId}/features/battery/properties/status"]
        assertTrue(categoryPath?.get?.deprecated == true)
        assertNotNull(categoryPath?.get?.description)
        assertContains(categoryPath!!.get.description, "**Deprecated.**")
        assertContains(categoryPath.get.description, "2.0.0")

        // Property operations should inherit submodel deprecation
        val propertyPath = paths["/{thingId}/features/battery/properties/status/voltage"]
        assertTrue(propertyPath?.get?.deprecated == true)
        assertContains(propertyPath!!.get.description!!, "**Deprecated.**")

        // Action operations should inherit submodel deprecation
        val actionPost = paths["/{thingId}/features/battery/inbox/messages/calibrate"]?.post
        assertTrue(actionPost?.deprecated == true)
        assertContains(actionPost!!.description!!, "**Deprecated.**")
    }

    @Test
    fun `property-level deprecation takes precedence over submodel-level deprecation`() {
        val featureModel = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Battery",
              "properties": {
                "voltage": {
                  "title": "Voltage",
                  "type": "number",
                  "ditto:deprecationNotice": {
                    "deprecated": true,
                    "supersededBy": "#/properties/batteryLevel",
                    "removalVersion": "3.0.0"
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()
        val submodelDeprecation = Utils.DeprecationNotice(deprecated = true, removalVersion = "2.0.0")

        FeaturesPathsGenerator.generateFeaturesPaths("battery", featureModel, paths, api, submodelDeprecation)

        // Property has its own deprecation notice - should use that (3.0.0), not the submodel one (2.0.0)
        val propertyPath = paths["/{thingId}/features/battery/properties/voltage"]
        assertTrue(propertyPath?.get?.deprecated == true)
        assertContains(propertyPath!!.get.description!!, "3.0.0")
        assertContains(propertyPath.get.description!!, "#/properties/batteryLevel")
    }

    @Test
    fun `non-deprecated submodel does not mark feature operations as deprecated`() {
        val featureModel = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Battery",
              "properties": {
                "voltage": {
                  "title": "Voltage",
                  "type": "number"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val api = openApi()

        // No submodel deprecation notice (null)
        FeaturesPathsGenerator.generateFeaturesPaths("battery", featureModel, paths, api)

        val featurePath = paths["/{thingId}/features/battery"]
        assertNull(featurePath?.get?.deprecated)

        val propertyPath = paths["/{thingId}/features/battery/properties/voltage"]
        assertNull(propertyPath?.get?.deprecated)
    }

    private fun thingModelFromJson(json: String): ThingModel =
        ThingModel.fromJson(JsonObject.of(json))

    private fun openApi() = OpenAPI().components(Components().schemas(mutableMapOf()))
}
