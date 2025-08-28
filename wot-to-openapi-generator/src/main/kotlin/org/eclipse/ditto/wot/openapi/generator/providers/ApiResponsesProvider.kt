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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.providers

import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses

object ApiResponsesProvider {

    private const val APPLICATION_JSON = "application/json"

    fun provide400ApiResponse(path: String): Pair<Int, ApiResponse> {
        return 400 to ApiResponse()
            .description("The request at '$path' failed due to bad input, check the error for details")
            .content(
                Content().addMediaType(
                    APPLICATION_JSON, MediaType()
                        .schema(Schema<Any>().apply {
                            `$ref`("dittoError_400")
                        })
                )
            )
    }

    fun provide401ApiResponse(path: String): Pair<Int, ApiResponse> {
        return 401 to ApiResponse()
            .description("Unauthorized to access '$path'")
            .content(
                Content().addMediaType(
                    APPLICATION_JSON, MediaType()
                        .schema(Schema<Any>().apply {
                            `$ref`("dittoError_401")
                        })
                )
            )
    }

    fun provide403ApiResponse(path: String): Pair<Int, ApiResponse> {
        return 403 to ApiResponse()
            .description("Forbidden to access '$path', ensure caller has required `WRITE` permissions")
            .content(
                Content().addMediaType(
                    APPLICATION_JSON, MediaType()
                        .schema(Schema<Any>().apply {
                            `$ref`("dittoError_403")
                        })
                )
            )
    }

    fun provide404ApiResponse(path: String): Pair<Int, ApiResponse> {
        return 404 to ApiResponse()
            .description("Requested path '$path' does either not exist or caller has insufficient `READ` permissions")
            .content(
                Content().addMediaType(
                    APPLICATION_JSON, MediaType()
                        .schema(Schema<Any>().apply {
                            `$ref`("dittoError_404")
                        })
                )
            )
    }

    fun provide408ApiResponse(path: String): Pair<Int, ApiResponse> {
        return 408 to ApiResponse()
            .description("The request could not be completed due to timeout")
            .content(
                Content().addMediaType(
                    APPLICATION_JSON, MediaType()
                        .schema(Schema<Any>().apply {
                            `$ref`("dittoError_408")
                        })
                )
            )
    }
}

fun ApiResponses.addApiResponse(pair: Pair<Int, ApiResponse>): ApiResponses {
    return this.addApiResponse(pair.first.toString(), pair.second)
}
