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

import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

object ErrorProvider {

    fun provideDittoErrorSchemas(): MutableMap<String, Schema<*>> =
        mutableMapOf("dittoError" to dittoErrorObjectSchema())

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