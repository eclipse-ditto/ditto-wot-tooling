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
package org.eclipse.ditto.wot.openapi.generator.providers

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

object ErrorProvider {

    fun provideDittoErrorSchemas(): MutableMap<String, Schema<*>> =
        mutableMapOf(
            "dittoError_400" to dittoErrorObjectSchema()
                .example(
                    ObjectMapper().valueToTree(
                        DittoError(
                            400,
                            "things:id.invalid",
                            "Thing ID 'nope' is not valid!",
                            "It must conform to the namespaced entity ID notation (see Ditto documentation)"
                        )
                    )
                ),

            "dittoError_401" to dittoErrorObjectSchema()
                .example(
                    ObjectMapper().valueToTree(
                        DittoError(
                            401,
                            "gateway:authentication.failed",
                            "The JWT was missing.",
                            "Check if your credentials were correct."
                        )
                    )
                ),

            "dittoError_403" to dittoErrorObjectSchema()
                .example(
                    ObjectMapper().valueToTree(
                        DittoError(
                            403,
                            "things:thing.notmodifiable",
                            "The Thing with ID 'some:thing' could not be modified as the requester had insufficient permissions ('WRITE' is required).",
                            "Check if the ID of your requested Thing was correct and you have sufficient permissions."
                        )
                    )
                ),

            "dittoError_404" to dittoErrorObjectSchema()
                .example(
                    ObjectMapper().valueToTree(
                        DittoError(
                            404,
                            "things:thing.notfound",
                            "The Thing with ID 'some:thing' could not be found or requester had insufficient permissions to access it.",
                            "Check if the ID of your requested Thing was correct and you have sufficient permissions."
                        )
                    )
                ),

            "dittoError_408" to dittoErrorObjectSchema()
                .example(
                    ObjectMapper().valueToTree(
                        DittoError(
                            408,
                            "command.timeout",
                            "The Command reached the specified timeout of {10000}ms.",
                            "Try increasing the command timeout."
                        )
                    )
                )
        )

    private fun dittoErrorObjectSchema() = ObjectSchema()
        .title("Ditto error")
        .description("Provides additional information about an occurred error and how to resolve it")
        .properties(
            mapOf(
                "status" to IntegerSchema()
                    .title("Status code")
                    .description("The status code of the error with HTTP status code semantics (e.g.: 4xx for user errors, 5xx for server errors)")
                    .minimum(400.toBigDecimal())
                    .maximum(599.toBigDecimal()),
                "error" to StringSchema()
                    .title("Error code")
                    .description("The error code or identifier that uniquely identifies the error"),
                "message" to StringSchema()
                    .title("Error message")
                    .description("The human readable message that explains what went wrong during the execution of a command/message"),
                "description" to StringSchema()
                    .title("Error description")
                    .description("Contains further information about the error e.g. a hint what caused the problem and how to solve it"),
                "href" to StringSchema()
                    .title("Error link")
                    .description("A link to further information about the error and how to fix it")
                    .format("uri")
            )
        )
        .required(
            listOf(
                "status",
                "error",
                "message"
            )
        )
}

data class DittoError(
    val status: Int,
    val error: String,
    val message: String,
    val description: String? = null,
    val href: String? = null
)