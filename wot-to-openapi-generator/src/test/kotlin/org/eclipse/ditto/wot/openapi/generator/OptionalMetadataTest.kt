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
import io.swagger.v3.oas.models.media.ObjectSchema
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.features.FeaturesPathsGenerator
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OptionalMetadataTest {

    @Test
    fun `feature generation succeeds when property has no title and description`() {
        val featureModel = thingModelFromJson(
            """
            {
              "@context": "https://www.w3.org/2022/wot/td/v1.1",
              "@type": "tm:ThingModel",
              "title": "Connectivity",
              "properties": {
                "readySince": {
                  "type": "string",
                  "format": "date-time"
                }
              }
            }
            """.trimIndent()
        )

        val paths = Paths()
        val openAPI = openApi()

        FeaturesPathsGenerator.generateFeaturesPaths("connectivity", featureModel, paths, openAPI)
        OpenApiGeneratorImpl.generateFeatureOpenApi("connectivity", featureModel, openAPI)

        val getPropertyPath = paths["/{thingId}/features/connectivity/properties/readySince"]?.get
        assertNotNull(getPropertyPath)

        val featureSchema = openAPI.components.schemas["feature_connectivity"]
        assertNotNull(featureSchema)

        val propertiesSchema = openAPI.components.schemas["connectivity_properties"] as? ObjectSchema
        assertNotNull(propertiesSchema)
        assertTrue(propertiesSchema.properties.containsKey("readySince"))
    }

    private fun thingModelFromJson(json: String): ThingModel =
        ThingModel.fromJson(JsonObject.of(json))

    private fun openApi() = OpenAPI().components(Components().schemas(mutableMapOf()))
}
