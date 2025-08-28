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
import org.eclipse.ditto.wot.openapi.generator.providers.ApiResponsesProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.providers.addApiResponse
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.eclipse.ditto.wot.model.Action
import org.eclipse.ditto.wot.model.ThingModel
import kotlin.jvm.optionals.getOrNull

object ActionsPathsGenerator {

    private const val APPLICATION_JSON = "application/json"

    var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateThingActionsPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        val thingActionModels = thingModel.actions.getOrNull()
        thingActionModels?.map {
            paths.addPathItem("/{thingId}/inbox/messages/${it.key}", providePathForAction(it.value, openAPI))
        }
    }

    private fun providePathForAction(action: Action, openAPI: OpenAPI): PathItem {

        val operation = Operation()
            .summary("Invokes the '${action.title.getOrNull()?.toString()}' action")
            .description(action.description.getOrNull()?.toString())
            .tags(listOf("Actions"))
            .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
            .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })

        provideInputSchema(action, openAPI)?.let { inputSchema ->
            operation.requestBody(
                RequestBody()
                    .description("Request payload of the '${action.title.getOrNull()?.toString()}' action")
                    .content(
                        Content().addMediaType(
                            APPLICATION_JSON,
                            MediaType().schema(inputSchema)
                        )
                    )
            )
        }

        val pathItem = PathItem()
            .post(
                operation
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The custom response of the '${action.actionName}' action - the status code is also custom")
                                    .content(
                                        provideOutputSchema(action, openAPI)?.let { outputSchema ->
                                            Content().addMediaType(
                                                APPLICATION_JSON,
                                                MediaType().schema(outputSchema)
                                            )
                                        }
                                    )
                            )
                            .addApiResponse(apiResponsesProvider.provide400ApiResponse("inbox/messages/${action.actionName}"))
                            .addApiResponse(apiResponsesProvider.provide401ApiResponse("inbox/messages/${action.actionName}"))
                            .addApiResponse(apiResponsesProvider.provide403ApiResponse("inbox/messages/${action.actionName}"))
                            .addApiResponse(apiResponsesProvider.provide404ApiResponse("inbox/messages/${action.actionName}"))
                            .addApiResponse(apiResponsesProvider.provide408ApiResponse("inbox/messages/${action.actionName}"))
                    )
            )
        return pathItem
    }

    private fun provideInputSchema(action: Action, openAPI: OpenAPI) =
        if (action.input.isPresent) {
            asOpenApiSchema(action.input.get(), null, "actioninput", openAPI)
        } else {
            null
        }

    private fun provideOutputSchema(action: Action, openAPI: OpenAPI) =
        if (action.output.isPresent) {
            asOpenApiSchema(action.output.get(), null, "actioninput", openAPI)
        } else {
            null
        }

}