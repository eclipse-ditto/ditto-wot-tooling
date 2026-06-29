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
package org.eclipse.ditto.wot.openapi.generator.providers

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ErrorProviderTest {

    @Test
    fun `ditto error schemas are collapsed to a single shared component`() {
        val schemas = ErrorProvider.provideDittoErrorSchemas()

        assertEquals(listOf("dittoError"), schemas.keys.toList())

        val schema = schemas["dittoError"]
        assertNotNull(schema)
        assertEquals("Ditto error", (schema as ObjectSchema).title)
    }

    @Test
    fun `ditto error schema can be appended after existing schemas`() {
        val openApi = OpenAPI().components(
            Components().schemas(mutableMapOf<String, Schema<*>>("attributes" to ObjectSchema()))
        )

        openApi.components.schemas.putAll(ErrorProvider.provideDittoErrorSchemas())

        assertEquals(listOf("attributes", "dittoError"), openApi.components.schemas.keys.toList())
    }
}