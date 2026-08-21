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
package org.eclipse.ditto.wot.openapi.generator.features

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
import org.eclipse.ditto.wot.openapi.generator.Utils
import org.eclipse.ditto.wot.openapi.generator.Utils.DeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.Utils.asOpenApiSchema
import org.eclipse.ditto.wot.openapi.generator.Utils.asPropertyName
import org.eclipse.ditto.wot.openapi.generator.Utils.buildDeprecationDescription
import org.eclipse.ditto.wot.openapi.generator.Utils.extractDeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.Utils.extractDesiredEnabled
import org.eclipse.ditto.wot.openapi.generator.Utils.extractPropertyCategory
import org.eclipse.ditto.wot.openapi.generator.Utils.isPrimitive
import org.eclipse.ditto.wot.openapi.generator.Utils.markSchemaDeprecated
import org.eclipse.ditto.wot.openapi.generator.Utils.mergeWithDeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.providers.ApiResponsesProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.providers.addApiResponse
import kotlin.jvm.optionals.getOrNull

object FeaturesPathsGenerator {

    private const val APPLICATION_JSON = "application/json"

    var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateFeaturesPaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI, submodelDeprecationNotice: DeprecationNotice? = null) {
        val featurePropertiesModels = featureModel.properties.getOrNull()
        val featureTitle = featureModel.title.getOrNull()?.toString() ?: featureName
        val submodelDeprecated = submodelDeprecationNotice?.deprecated == true
        paths.putAll(providePathItemsForFeature(featureName, featureTitle, submodelDeprecated, submodelDeprecationNotice))

        featurePropertiesModels?.entries
            ?.sortedBy { extractPropertyCategory(it.value) + it.key }
            ?.map {
                val dittoCategory = extractPropertyCategory(it.value)
                if (dittoCategory != null && !paths.containsKey("/{thingId}/features/$featureName/properties/$dittoCategory")) {
                    paths.addPathItem(
                        "/{thingId}/features/$featureName/properties/$dittoCategory",
                        providePathItemFeaturePropertiesCategory(featureName, featureTitle, dittoCategory, submodelDeprecationNotice)
                    )
                }
                paths.addPathItem("/{thingId}/features/$featureName/properties/${dittoCategory?.let { "$it/" } ?: ""}${it.key}",
                    providePathItemFeatureProperty(featureName, featureTitle, it.value, openAPI, submodelDeprecationNotice))

                // a readOnly property can never be desired, regardless of a ditto:desired declaration
                if (extractDesiredEnabled(it.value) && !it.value.isReadOnly) {
                    if (dittoCategory != null && !paths.containsKey("/{thingId}/features/$featureName/desiredProperties/$dittoCategory")) {
                        paths.addPathItem(
                            "/{thingId}/features/$featureName/desiredProperties/$dittoCategory",
                            providePathItemFeatureDesiredPropertiesCategory(featureName, featureTitle, dittoCategory, submodelDeprecationNotice)
                        )
                    }
                    paths.addPathItem("/{thingId}/features/$featureName/desiredProperties/${dittoCategory?.let { "$it/" } ?: ""}${it.key}",
                        providePathItemFeatureDesiredProperty(featureName, featureTitle, it.value, openAPI, submodelDeprecationNotice))
                }
            }
    }

    private fun providePathItemFeaturePropertiesCategory(
        featureName: String,
        featureTitle: String,
        category: String,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {
        val deprecated = submodelDeprecationNotice?.deprecated == true
        val deprecationDescription = buildDeprecationDescription(submodelDeprecationNotice)
        return PathItem()
        .get(
            Operation()
                .also { if (deprecated) it.deprecated(true) }
                .summary("Retrieves all '$category' categorized properties of feature $featureTitle")
                .description(deprecationDescription)
                .tags(listOf("Feature: $featureTitle"))
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
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
    }

    private fun providePathItemFeatureDesiredPropertiesCategory(
        featureName: String,
        featureTitle: String,
        category: String,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {
        val deprecated = submodelDeprecationNotice?.deprecated == true
        val deprecationDescription = buildDeprecationDescription(submodelDeprecationNotice)
        return PathItem()
        .get(
            Operation()
                .also { if (deprecated) it.deprecated(true) }
                .summary("Retrieves all '$category' categorized desired properties of feature $featureTitle")
                .description(deprecationDescription)
                .tags(listOf("Feature: $featureTitle"))
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                .responses(
                    ApiResponses()
                        .addApiResponse(
                            "200", ApiResponse()
                                .description("Returns the desired feature category '$category'")
                                .content(
                                    Content().addMediaType(
                                        APPLICATION_JSON, MediaType()
                                            .schema(Schema<Any>().apply {
                                                `$ref`("#/components/schemas/${asPropertyName(featureName)}_${category}_properties")
                                            })
                                    )
                                )
                        )
                        .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName/desiredProperties/$category"))
                )
        )
    }

    private fun providePathItemsForFeature(featureName: String, featureTitle: String, deprecated: Boolean = false, deprecationNotice: DeprecationNotice? = null): Map<String, PathItem> {
        val deprecationDescription = buildDeprecationDescription(deprecationNotice)
        return mapOf(
            "/{thingId}/features/$featureName" to PathItem()
                .get(
                    Operation()
                        .also { if (deprecated) it.deprecated(true) }
                        .summary("Retrieves the feature $featureTitle")
                        .description(deprecationDescription)
                        .tags(listOf("Feature: $featureTitle"))
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
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
        openAPI: OpenAPI,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {

        val dittoCategory = extractPropertyCategory(property)
        val propertyDeprecationNotice = extractDeprecationNotice(property)
        val deprecationNotice = propertyDeprecationNotice ?: submodelDeprecationNotice
        val deprecated = deprecationNotice?.deprecated == true
        val description = mergeWithDeprecationNotice(property.description.getOrNull()?.toString(), deprecationNotice)
        val responseSchema = provideSchema(property, featureName, openAPI)
        if (deprecated) markSchemaDeprecated(responseSchema, openAPI)
        val path = provideFeaturePropertyPath(featureName, dittoCategory, property)
        val pathItem = PathItem()
            .get(
                Operation()
                    .also { if (deprecated) it.deprecated(true) }
                    .summary("Retrieves the '${property.title.getOrNull()?.toString()}' property")
                    .description(description)
                    .tags(listOf("Feature: $featureTitle"))
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The feature property '${property.propertyName}' is returned")
                                    .content(
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(responseSchema)
                                        )
                                    )
                            )
                            .addApiResponse(apiResponsesProvider.provide400ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide401ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide404ApiResponse(path))
                    )
            )
        // writes move to the desiredProperties path instead when ditto:desired is enabled for this property
        if (!property.isReadOnly && !extractDesiredEnabled(property)) {
            pathItem
                .put(providePropertyWriteOperation("Replaces", "modified", property, featureName, featureTitle, openAPI, deprecated, description, path, desired = false))
                .patch(providePropertyWriteOperation("Merges", "merged", property, featureName, featureTitle, openAPI, deprecated, description, path, desired = false))
        }
        return pathItem
    }

    private fun providePathItemFeatureDesiredProperty(
        featureName: String,
        featureTitle: String,
        property: Property,
        openAPI: OpenAPI,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {

        val dittoCategory = extractPropertyCategory(property)
        val propertyDeprecationNotice = extractDeprecationNotice(property)
        val deprecationNotice = propertyDeprecationNotice ?: submodelDeprecationNotice
        val deprecated = deprecationNotice?.deprecated == true
        val description = mergeWithDeprecationNotice(property.description.getOrNull()?.toString(), deprecationNotice)
        val responseSchema = provideSchema(property, featureName, openAPI)
        if (deprecated) markSchemaDeprecated(responseSchema, openAPI)
        val path = provideFeaturePropertyPath(featureName, dittoCategory, property, "desiredProperties")
        return PathItem()
            .get(
                Operation()
                    .also { if (deprecated) it.deprecated(true) }
                    .summary("Retrieves the desired '${property.title.getOrNull()?.toString()}' property")
                    .description(description)
                    .tags(listOf("Feature: $featureTitle"))
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The desired feature property '${property.propertyName}' is returned")
                                    .content(
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(responseSchema)
                                        )
                                    )
                            )
                            .addApiResponse(apiResponsesProvider.provide400ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide401ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide404ApiResponse(path))
                    )
            )
            .put(providePropertyWriteOperation("Replaces", "modified", property, featureName, featureTitle, openAPI, deprecated, description, path, desired = true))
            .patch(providePropertyWriteOperation("Merges", "merged", property, featureName, featureTitle, openAPI, deprecated, description, path, desired = true))
    }

    /**
     * Builds the PUT ("Replaces") or PATCH ("Merges") operation for a feature property, shared between the
     * regular `properties` path and the `desiredProperties` path (when `ditto:desired` is enabled).
     */
    private fun providePropertyWriteOperation(
        verb: String,
        successfullyWord: String,
        property: Property,
        featureName: String,
        featureTitle: String,
        openAPI: OpenAPI,
        deprecated: Boolean,
        description: String?,
        path: String,
        desired: Boolean
    ): Operation {
        val titleLabel = property.title.getOrNull()?.toString()
        val summary = if (desired) "$verb the desired '$titleLabel' property" else "$verb the '$titleLabel' property"
        val propertyLabel = if (desired) "desired feature property" else "feature property"
        return Operation()
            .also { if (deprecated) it.deprecated(true) }
            .summary(summary)
            .description(description)
            .tags(listOf("Feature: $featureTitle"))
            .responses(
                ApiResponses()
                    .addApiResponse(
                        "201", ApiResponse()
                            .description("The $propertyLabel '${property.propertyName}' was successfully created")
                            .content(
                                Content().addMediaType(
                                    APPLICATION_JSON,
                                    MediaType().schema(asOpenApiSchema(property, featureName, "property", openAPI))
                                )
                            )
                    )
                    .addApiResponse(
                        "204", ApiResponse()
                            .description("The $propertyLabel '${property.propertyName}' was successfully $successfullyWord")
                    )
                    .addApiResponse(apiResponsesProvider.provide400ApiResponse(path))
                    .addApiResponse(apiResponsesProvider.provide401ApiResponse(path))
                    .addApiResponse(apiResponsesProvider.provide403ApiResponse(path))
                    .addApiResponse(apiResponsesProvider.provide404ApiResponse(path))
            )
    }

    private fun provideFeaturePropertyPath(
        featureName: String,
        dittoCategory: String?,
        property: Property,
        segment: String = "properties"
    ) = "features/$featureName/$segment/${dittoCategory?.let { "$it/" } ?: ""}${property.propertyName}"

    private fun provideSchema(property: Property, featureName: String, openAPI: OpenAPI) =
        if (isPrimitive(property.type.getOrNull())) {
            asOpenApiSchema(property, null, "property", openAPI)
        } else {
            Schema<Any>().apply { `$ref`("#/components/schemas/feature_${asPropertyName(featureName)}_property_${asPropertyName(property.propertyName)}") }
        }

}
