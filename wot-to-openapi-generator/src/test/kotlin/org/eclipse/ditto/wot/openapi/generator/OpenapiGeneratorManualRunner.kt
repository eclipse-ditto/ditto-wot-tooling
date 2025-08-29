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
package org.eclipse.ditto.wot.openapi.generator

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenapiGeneratorManualRunner {

    @Test
    fun `test OpenAPI generator execution for heatsource model succeeds`() {

        val generator = GeneratorStarter

        runBlocking {
            assertEquals(0,
                         generator.run(arrayOf(
                             "0.1.1",
                             "lamp",
                             "https://ditto.example.com",
                             "https://models.dev.example.com"
                         ))
            )
        }
    }

}
