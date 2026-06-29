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
package org.eclipse.ditto.wot.kotlin.generator.plugin.util

import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KdocGenerator], verifying that WoT data-schema metadata is rendered into KDoc text.
 */
class KdocGeneratorTest {

    @BeforeEach
    fun enable() {
        KdocGenerator.configure(true)
    }

    @AfterEach
    fun reset() {
        KdocGenerator.configure(true)
    }

    private fun schema(json: String): SingleDataSchema = SingleDataSchema.fromJson(JsonObject.of(json))

    @Test
    fun `renders title description and unit`() {
        val kdoc = KdocGenerator.forSchema(
            schema(
                """
                {
                    "type": "integer",
                    "title": "Timeout",
                    "description": "Time to wait before giving up.",
                    "unit": "s"
                }
                """.trimIndent()
            )
        )

        assertNotNull(kdoc)
        assertTrue(kdoc!!.contains("Timeout"), "expected title, was: $kdoc")
        assertTrue(kdoc.contains("Time to wait before giving up."), "expected description, was: $kdoc")
        assertTrue(kdoc.contains("**Unit:** `s`"), "expected unit, was: $kdoc")
    }

    @Test
    fun `renders numeric constraints from schema json`() {
        val kdoc = KdocGenerator.forSchema(
            schema(
                """
                {
                    "type": "integer",
                    "title": "Brightness",
                    "minimum": 0,
                    "maximum": 100,
                    "unit": "%"
                }
                """.trimIndent()
            )
        )

        assertNotNull(kdoc)
        assertTrue(kdoc!!.contains("**Constraints:**"), "expected constraints block, was: $kdoc")
        assertTrue(kdoc.contains("minimum: `0`"), "expected minimum, was: $kdoc")
        assertTrue(kdoc.contains("maximum: `100`"), "expected maximum, was: $kdoc")
    }

    @Test
    fun `renders default const and allowed values`() {
        val kdoc = KdocGenerator.forSchema(
            schema(
                """
                {
                    "type": "string",
                    "default": "medium",
                    "enum": ["low", "medium", "high"]
                }
                """.trimIndent()
            )
        )

        assertNotNull(kdoc)
        assertTrue(kdoc!!.contains("**Default:** `medium`"), "expected default, was: $kdoc")
        assertTrue(kdoc.contains("**Allowed values:**"), "expected allowed values, was: $kdoc")
        assertTrue(kdoc.contains("`low`"), "expected enum value, was: $kdoc")
    }

    @Test
    fun `omits allowed values when requested`() {
        val kdoc = KdocGenerator.forSchema(
            schema(
                """
                {
                    "type": "string",
                    "title": "Severity",
                    "enum": ["low", "high"]
                }
                """.trimIndent()
            ),
            includeAllowedValues = false
        )

        assertNotNull(kdoc)
        assertTrue(kdoc!!.contains("Severity"))
        assertTrue(!kdoc.contains("Allowed values"), "should omit allowed values, was: $kdoc")
    }

    @Test
    fun `returns null when no documentable metadata is present`() {
        assertNull(KdocGenerator.forSchema(schema("""{"type": "boolean"}""")))
    }

    @Test
    fun `returns null when disabled`() {
        KdocGenerator.configure(false)
        assertNull(
            KdocGenerator.forSchema(
                schema("""{"type": "integer", "title": "Timeout", "unit": "s"}""")
            )
        )
    }

    @Test
    fun `returns null for null schema`() {
        assertNull(KdocGenerator.forSchema(null))
    }
}
