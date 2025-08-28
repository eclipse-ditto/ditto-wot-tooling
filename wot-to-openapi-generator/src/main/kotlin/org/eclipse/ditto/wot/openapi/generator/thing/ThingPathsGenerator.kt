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

import org.eclipse.ditto.wot.openapi.generator.providers.ApiResponsesProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.providers.addApiResponse
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.eclipse.ditto.wot.model.ThingModel
import kotlin.jvm.optionals.getOrNull

object ThingPathsGenerator {

    private const val APPLICATION_JSON = "application/json"


    var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateThingPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        openAPI.schema("thing", provideCompleteThingSchema())
        paths.putAll(providePathItemsForCompleteThing(thingModel.title.getOrNull()?.toString()))
    }

    private fun provideCompleteThingSchema() = ObjectSchema()
        .properties(
            mapOf(
                "thingId" to StringSchema(),
                "policyId" to StringSchema(),
                "definition" to StringSchema(),
                "attributes" to Schema<Any>().apply {
                    `$ref`("attributes")
                },
                "features" to Schema<Any>().apply {
                    `$ref`("features")
                }
            )
        )

    private fun providePathItemsForCompleteThing(thingModelTitle: String?): Map<String, PathItem> {
        return mapOf(
            "/{thingId}" to PathItem()
                .get(
                    Operation()
                        .summary("Retrieves the complete ${thingModelTitle?.let { "'$it' " } ?: ""}thing")
                        .tags(listOf("Thing"))
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "200", ApiResponse()
                                        .description("Returns complete thing")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON, MediaType()
                                                    .schema(Schema<Any>().apply {
                                                        `$ref`("thing")
                                                    })
                                            )
                                        )
                                )
                                .addApiResponse(apiResponsesProvider.provide400ApiResponse("/"))
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("/"))
                                .addApiResponse(apiResponsesProvider.provide404ApiResponse("/"))
                        )
                )
                .patch(
                    Operation()
                        .summary("Merges the complete ${thingModelTitle?.let { "'$it' " } ?: ""}thing")
                        .tags(listOf("Thing"))
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                        .addParametersItem(
                            Parameter()
                                .name("Content-Type")
                                .`in`("header")
                                .schema(
                                    StringSchema()
                                        .example("application/merge-patch+json")
                                        //._const("application/merge-patch+json")
                                )
                        )
                        .requestBody(
                            RequestBody()
                                .description("Request payload of merging the complete thing")
                                .content(
                                    Content().addMediaType(
                                        APPLICATION_JSON,
                                        MediaType().schema(Schema<Any>().apply{
                                            `$ref`("thing")
                                        })
                                    )
                                )
                        )
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "201", ApiResponse()
                                        .description("The not yet existing thing was successfully created")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON, MediaType()
                                                    .schema(Schema<Any>().apply {
                                                        `$ref`("thing")
                                                    })
                                            )
                                        )
                                )
                                .addApiResponse(
                                    "204", ApiResponse()
                                        .description("The existing thing was successfully merged")
                                )
                                .addApiResponse(apiResponsesProvider.provide400ApiResponse("/"))
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("/"))
                                .addApiResponse(apiResponsesProvider.provide403ApiResponse("/"))
                                .addApiResponse(apiResponsesProvider.provide404ApiResponse("/"))
                        )
                )
        )
    }

}