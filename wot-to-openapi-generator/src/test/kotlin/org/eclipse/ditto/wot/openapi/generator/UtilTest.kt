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

import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.openapi.generator.Utils.allUppercaseToLowercase
import org.eclipse.ditto.wot.openapi.generator.Utils.extractDesiredEnabled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilTest {

    @Test
    fun `test allUppercaseToLowercase`() {

        val testString = "TEST"
        val results = allUppercaseToLowercase(testString)

        assertEquals(results, "test")
    }

    @Test
    fun `extractDesiredEnabled returns false when ditto desired is absent`() {
        val property = Property.fromJson("prop", JsonObject.of("""{"type": "number"}"""))
        assertFalse(extractDesiredEnabled(property))
    }

    @Test
    fun `extractDesiredEnabled returns true when ditto desired is present without enabled field`() {
        val property = Property.fromJson(
            "prop",
            JsonObject.of("""{"type": "number", "ditto:desired": {}}""")
        )
        assertTrue(extractDesiredEnabled(property))
    }

    @Test
    fun `extractDesiredEnabled returns true when enabled is explicitly true`() {
        val property = Property.fromJson(
            "prop",
            JsonObject.of("""{"type": "number", "ditto:desired": {"enabled": true}}""")
        )
        assertTrue(extractDesiredEnabled(property))
    }

    @Test
    fun `extractDesiredEnabled returns false when enabled is explicitly false`() {
        val property = Property.fromJson(
            "prop",
            JsonObject.of("""{"type": "number", "ditto:desired": {"enabled": false}}""")
        )
        assertFalse(extractDesiredEnabled(property))
    }

    @Test
    fun `extractDesiredEnabled returns false when ditto desired is not an object`() {
        val property = Property.fromJson(
            "prop",
            JsonObject.of("""{"type": "number", "ditto:desired": "yes"}""")
        )
        assertFalse(extractDesiredEnabled(property))
    }
}
