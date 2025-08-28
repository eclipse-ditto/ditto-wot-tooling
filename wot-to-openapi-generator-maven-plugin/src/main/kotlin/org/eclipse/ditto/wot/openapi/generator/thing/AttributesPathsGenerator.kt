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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.thing

import org.eclipse.ditto.wot.openapi.generator.Utils.asOpenApiSchema
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

object AttributesPathsGenerator {

    private const val APPLICATION_JSON = "application/json"

    private var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateThingAttributesPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        val attributesModels = thingModel.properties.getOrNull()
        paths.putAll(providePathItemsForAllAttributes())
        attributesModels?.entries?.sortedBy { it.key }?.map {
            paths.addPathItem("/{thingId}/attributes/${it.key}", providePathItemGetAttribute(it.value, openAPI))
        }
    }

    private fun providePathItemsForAllAttributes(): Map<String, PathItem> {
        return mapOf(
            "/{thingId}/attributes" to PathItem()
                .get(
                    Operation()
                        .summary("Retrieves all attributes")
                        .tags(listOf("Attributes"))
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "200", ApiResponse()
                                        .description("Returns all attributes")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON, MediaType()
                                                    .schema(Schema<Any>().apply {
                                                        `$ref`("attributes")
                                                    })
                                            )
                                        )
                                )
                                .addApiResponse(apiResponsesProvider.provide400ApiResponse("attributes"))
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("attributes"))
                                .addApiResponse(apiResponsesProvider.provide404ApiResponse("attributes"))
                        )
                )
        )
    }

    private fun providePathItemGetAttribute(property: Property, openAPI: OpenAPI): PathItem {

        val pathItem = PathItem()
            .get(
                Operation()
                    .summary("Retrieves the '${property.title.getOrNull()?.toString()}'")
                    .description(property.description.getOrNull()?.toString())
                    .tags(listOf("Attributes"))
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The attribute '${property.propertyName}' is returned")
                                    .content(
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(provideSchema(property, openAPI))
                                        )
                                    )
                            )
                            .addApiResponse(apiResponsesProvider.provide400ApiResponse("attributes/${property.propertyName}"))
                            .addApiResponse(apiResponsesProvider.provide401ApiResponse("attributes/${property.propertyName}"))
                            .addApiResponse(apiResponsesProvider.provide404ApiResponse("attributes/${property.propertyName}"))
                    )
            )
        if (!property.isReadOnly) {
            pathItem
                .put(
                    Operation()
                        .summary("Replaces the '${property.title.getOrNull()?.toString()}'")
                        .description(property.description.getOrNull()?.toString())
                        .tags(listOf("Attributes"))
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "201", ApiResponse()
                                        .description("The attribute '${property.propertyName}' was successfully created")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON,
                                                MediaType().schema(asOpenApiSchema(property, openAPI = openAPI))
                                            )
                                        )
                                )
                                .addApiResponse(
                                    "204", ApiResponse()
                                        .description("The attribute '${property.propertyName}' was successfully modified")
                                )
                                .addApiResponse(apiResponsesProvider.provide400ApiResponse("attributes/${property.propertyName}"))
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("attributes/${property.propertyName}"))
                                .addApiResponse(apiResponsesProvider.provide403ApiResponse("attributes/${property.propertyName}"))
                                .addApiResponse(apiResponsesProvider.provide404ApiResponse("attributes/${property.propertyName}"))
                        )
                )
                .patch(
                    Operation()
                        .summary("Merges the '${property.title.getOrNull()?.toString()}'")
                        .description(property.description.getOrNull()?.toString())
                        .tags(listOf("Attributes"))
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "201", ApiResponse()
                                        .description("The attribute '${property.propertyName}' was successfully created")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON,
                                                MediaType().schema(asOpenApiSchema(property, openAPI = openAPI))
                                            )
                                        )
                                )
                                .addApiResponse(
                                    "204", ApiResponse()
                                        .description("The attribute '${property.propertyName}' was successfully merged")
                                )
                                .addApiResponse(apiResponsesProvider.provide400ApiResponse("attributes/${property.propertyName}"))
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("attributes/${property.propertyName}"))
                                .addApiResponse(apiResponsesProvider.provide403ApiResponse("attributes/${property.propertyName}"))
                                .addApiResponse(apiResponsesProvider.provide404ApiResponse("attributes/${property.propertyName}"))
                        )
                )
        }
        return pathItem
    }

    private fun provideSchema(property: Property, openAPI: OpenAPI) =
        if (isPrimitive(property.type.getOrNull())) {
            asOpenApiSchema(property, openAPI = openAPI)
        } else {
            Schema<Any>().apply { `$ref`("attribute_${property.propertyName}") }
        }

}