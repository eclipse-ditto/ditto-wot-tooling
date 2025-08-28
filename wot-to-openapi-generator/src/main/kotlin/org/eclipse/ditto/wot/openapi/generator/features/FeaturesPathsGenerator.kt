/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.features

import org.eclipse.ditto.wot.openapi.generator.Utils.asOpenApiSchema
import org.eclipse.ditto.wot.openapi.generator.Utils.asPropertyName
import org.eclipse.ditto.wot.openapi.generator.Utils.extractPropertyCategory
import org.eclipse.ditto.wot.openapi.generator.Utils.isPrimitive
import org.eclipse.ditto.wot.openapi.generator.providers.ApiResponsesProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.providers.addApiResponse
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.model.ThingModel
import kotlin.jvm.optionals.getOrNull

object FeaturesPathsGenerator {

    private const val APPLICATION_JSON = "application/json"

    var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateFeaturesPaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        val featurePropertiesModels = featureModel.properties.getOrNull()
        val featureTitle = featureModel.title.getOrNull()?.toString() ?: featureName
        paths.putAll(providePathItemsForFeature(featureName, featureTitle))

        featurePropertiesModels?.entries
            ?.sortedBy { extractPropertyCategory(it.value) + it.key }
            ?.map {
                val dittoCategory = extractPropertyCategory(it.value)
                if (dittoCategory != null && !paths.containsKey("/{thingId}/features/$featureName/properties/$dittoCategory")) {
                    paths.addPathItem(
                        "/{thingId}/features/$featureName/properties/$dittoCategory",
                        providePathItemFeaturePropertiesCategory(featureName, featureTitle, dittoCategory)
                    )
                }
                paths.addPathItem("/{thingId}/features/$featureName/properties/${dittoCategory?.let { "$it/" } ?: ""}${it.key}",
                    providePathItemFeatureProperty(featureName, featureTitle, it.value, openAPI))
            }
    }

    private fun providePathItemFeaturePropertiesCategory(
        featureName: String,
        featureTitle: String,
        category: String
    ): PathItem = PathItem()
        .get(
            Operation()
                .summary("Retrieves all '$category' categorized properties of feature $featureTitle")
                .tags(listOf("Feature: $featureTitle"))
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                .responses(
                    ApiResponses()
                        .addApiResponse(
                            "200", ApiResponse()
                                .description("Returns the full feature category '$category'")
                                .content(
                                    Content().addMediaType(
                                        APPLICATION_JSON, MediaType()
                                            .schema(Schema<Any>().apply {
                                                `$ref`("#/components/schemas/${asPropertyName(featureName)}_${category}_properties")
                                            })
                                    )
                                )
                        )
                        .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName/properties/$category"))
                )
        )

    private fun providePathItemsForFeature(featureName: String, featureTitle: String): Map<String, PathItem> {
        return mapOf(
            "/{thingId}/features/$featureName" to PathItem()
                .get(
                    Operation()
                        .summary("Retrieves the feature $featureTitle")
                        .tags(listOf("Feature: $featureTitle"))
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "200", ApiResponse()
                                        .description("Returns the full feature")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON, MediaType()
                                                    .schema(Schema<Any>().apply {
                                                        `$ref`("feature_$featureName")
                                                    })
                                            )
                                        )
                                )
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName"))
                        )
                )
        )
    }

    private fun providePathItemFeatureProperty(
        featureName: String,
        featureTitle: String,
        property: Property,
        openAPI: OpenAPI
    ): PathItem {

        val dittoCategory = extractPropertyCategory(property)
        val pathItem = PathItem()
            .get(
                Operation()
                    .summary("Retrieves the '${property.title.getOrNull()?.toString()}' property")
                    .description(property.description.getOrNull()?.toString())
                    .tags(listOf("Feature: $featureTitle"))
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The feature property '${property.propertyName}' is returned")
                                    .content(
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(provideSchema(property, featureName, openAPI))
                                        )
                                    )
                            )
                            .addApiResponse(
                                apiResponsesProvider.provide400ApiResponse(
                                    provideFeaturePropertyPath(
                                        featureName,
                                        dittoCategory,
                                        property
                                    )
                                )
                            )
                            .addApiResponse(
                                apiResponsesProvider.provide401ApiResponse(
                                    provideFeaturePropertyPath(
                                        featureName,
                                        dittoCategory,
                                        property
                                    )
                                )
                            )
                            .addApiResponse(
                                apiResponsesProvider.provide404ApiResponse(
                                    provideFeaturePropertyPath(
                                        featureName,
                                        dittoCategory,
                                        property
                                    )
                                )
                            )
                    )
            )
        if (!property.isReadOnly) {
            pathItem
                .put(
                    Operation()
                        .summary("Replaces the '${property.title.getOrNull()?.toString()}' property")
                        .description(property.description.getOrNull()?.toString())
                        .tags(listOf("Feature: $featureTitle"))
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "201", ApiResponse()
                                        .description("The feature property '${property.propertyName}' was successfully created")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON,
                                                MediaType().schema(asOpenApiSchema(property, featureName, "property", openAPI))
                                            )
                                        )
                                )
                                .addApiResponse(
                                    "204", ApiResponse()
                                        .description("The feature property '${property.propertyName}' was successfully modified")
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide400ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide401ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide403ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide404ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                        )
                )
                .patch(
                    Operation()
                        .summary("Merges the '${property.title.getOrNull()?.toString()}' property")
                        .description(property.description.getOrNull()?.toString())
                        .tags(listOf("Feature: $featureTitle"))
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "201", ApiResponse()
                                        .description("The feature property '${property.propertyName}' was successfully created")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON,
                                                MediaType().schema(asOpenApiSchema(property, featureName, "property", openAPI))
                                            )
                                        )
                                )
                                .addApiResponse(
                                    "204", ApiResponse()
                                        .description("The feature property '${property.propertyName}' was successfully merged")
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide400ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide401ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide403ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                                .addApiResponse(
                                    apiResponsesProvider.provide404ApiResponse(
                                        provideFeaturePropertyPath(
                                            featureName,
                                            dittoCategory,
                                            property
                                        )
                                    )
                                )
                        )
                )
        }
        return pathItem
    }

    private fun provideFeaturePropertyPath(
        featureName: String,
        dittoCategory: String?,
        property: Property
    ) = "features/$featureName/properties/${dittoCategory?.let { "$it/" } ?: ""}${property.propertyName}"

    private fun provideSchema(property: Property, featureName: String, openAPI: OpenAPI) =
        if (isPrimitive(property.type.getOrNull())) {
            asOpenApiSchema(property, null, "property", openAPI)
        } else {
            Schema<Any>().apply { `$ref`("#/components/schemas/feature_${asPropertyName(featureName)}_property_${asPropertyName(property.propertyName)}") }
        }

}