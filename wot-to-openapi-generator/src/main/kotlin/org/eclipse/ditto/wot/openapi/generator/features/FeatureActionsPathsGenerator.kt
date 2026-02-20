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
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.eclipse.ditto.wot.model.Action
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.Utils.asOpenApiSchema
import org.eclipse.ditto.wot.openapi.generator.Utils.extractDeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.Utils.mergeWithDeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.providers.ApiResponsesProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.providers.addApiResponse
import kotlin.jvm.optionals.getOrNull

object FeatureActionsPathsGenerator {

    private const val APPLICATION_JSON = "application/json"

    var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateFeatureActionsPaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        val featureTitle = featureModel.title.getOrNull()?.toString() ?: featureName
        val featureActions = featureModel.actions.getOrNull()
        featureActions?.entries?.sortedBy { it.key }?.map {
            paths.addPathItem(
                "/{thingId}/features/$featureName/inbox/messages/${it.key}",
                providePathForAction(featureTitle, featureName, it.value, openAPI)
            )
        }
    }

    private fun providePathForAction(featureTitle: String, featureName: String, action: Action, openAPI: OpenAPI): PathItem {
        val deprecationNotice = extractDeprecationNotice(action)
        val deprecated = deprecationNotice?.deprecated == true

        val operation = Operation()
            .also { if (deprecated) it.deprecated(true) }
            .summary("Invokes the '${action.title.getOrNull()?.toString()}' action")
            .description(mergeWithDeprecationNotice(action.description.getOrNull()?.toString(), deprecationNotice))
            .tags(listOf("Feature: $featureTitle - Actions"))
            .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
            .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })

        provideInputSchema(action, featureName, openAPI)?.let { inputSchema ->
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
                operation.responses(
                    ApiResponses()
                        .addApiResponse(
                            "200", ApiResponse()
                                .description("The custom response of the '${action.actionName}' action - the status code is also custom")
                                .content(
                                    provideOutputSchema(action, featureName, openAPI)?.let { outputSchema ->
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(outputSchema)
                                        )
                                    }
                                )
                        )
                        .addApiResponse(apiResponsesProvider.provide400ApiResponse("features/$featureName/inbox/messages/${action.actionName}"))
                        .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName/inbox/messages/${action.actionName}"))
                        .addApiResponse(apiResponsesProvider.provide403ApiResponse("features/$featureName/inbox/messages/${action.actionName}"))
                        .addApiResponse(apiResponsesProvider.provide404ApiResponse("features/$featureName/inbox/messages/${action.actionName}"))
                        .addApiResponse(apiResponsesProvider.provide408ApiResponse("features/$featureName/inbox/messages/${action.actionName}"))
                )
            )
        return pathItem
    }

    private fun provideInputSchema(action: Action, featureName: String, openAPI: OpenAPI) =
        if (action.input.isPresent) {
            asOpenApiSchema(action.input.get(), featureName, "actioninput", openAPI)
        } else {
            null
        }

    private fun provideOutputSchema(action: Action, featureName: String, openAPI: OpenAPI) =
        if (action.output.isPresent) {
            asOpenApiSchema(action.output.get(), featureName, "actionoutput", openAPI)
        } else {
            null
        }

}
