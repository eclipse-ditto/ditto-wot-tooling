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
package org.eclipse.ditto.wot.kotlin.generator.plugin.property

import org.eclipse.ditto.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for [TmRefScanner.computeFingerprint] — the structural fingerprint that enables
 * tm:ref-based deduplication even when the WoT library rewrites JSON metadata.
 *
 * @since 1.1.0
 */
class TmRefScannerTest {

    @Test
    fun `identical schemas produce identical fingerprints`() {
        val schema1 = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "value": { "type": "integer" }
              },
              "required": ["name"]
            }
        """.trimIndent())

        val schema2 = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "value": { "type": "integer" }
              },
              "required": ["name"]
            }
        """.trimIndent())

        assertEquals(
            TmRefScanner.computeFingerprint(schema1),
            TmRefScanner.computeFingerprint(schema2)
        )
    }

    @Test
    fun `fingerprint ignores title and description`() {
        val withMetadata = JsonObject.of("""
            {
              "title": "Severity Info",
              "description": "Contains severity level and timestamp",
              "type": "object",
              "properties": {
                "level": { "title": "Level", "type": "string" },
                "activeSince": { "title": "Active Since", "description": "When it started", "type": "string" }
              },
              "required": ["level", "activeSince"]
            }
        """.trimIndent())

        val withoutMetadata = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "level": { "type": "string" },
                "activeSince": { "type": "string" }
              },
              "required": ["level", "activeSince"]
            }
        """.trimIndent())

        assertEquals(
            TmRefScanner.computeFingerprint(withMetadata),
            TmRefScanner.computeFingerprint(withoutMetadata),
            "Fingerprints should match regardless of title/description"
        )
    }

    @Test
    fun `fingerprint ignores readOnly, format, unit, and ditto-category`() {
        val withAnnotations = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "timestamp": {
                  "@type": "om2:Date",
                  "type": "string",
                  "readOnly": true,
                  "format": "date-time",
                  "unit": "xsd:dateTimeStamp"
                }
              }
            }
        """.trimIndent())

        val bare = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "timestamp": { "type": "string" }
              }
            }
        """.trimIndent())

        assertEquals(
            TmRefScanner.computeFingerprint(withAnnotations),
            TmRefScanner.computeFingerprint(bare),
            "Fingerprints should match regardless of readOnly/format/unit/@type"
        )
    }

    @Test
    fun `different property names produce different fingerprints`() {
        val schema1 = JsonObject.of("""
            {
              "type": "object",
              "properties": { "name": { "type": "string" } }
            }
        """.trimIndent())

        val schema2 = JsonObject.of("""
            {
              "type": "object",
              "properties": { "label": { "type": "string" } }
            }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(schema1),
            TmRefScanner.computeFingerprint(schema2)
        )
    }

    @Test
    fun `different types produce different fingerprints`() {
        val stringType = JsonObject.of("""{ "type": "string" }""")
        val integerType = JsonObject.of("""{ "type": "integer" }""")

        assertNotEquals(
            TmRefScanner.computeFingerprint(stringType),
            TmRefScanner.computeFingerprint(integerType)
        )
    }

    @Test
    fun `different required fields produce different fingerprints`() {
        val schema1 = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "a": { "type": "string" },
                "b": { "type": "string" }
              },
              "required": ["a"]
            }
        """.trimIndent())

        val schema2 = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "a": { "type": "string" },
                "b": { "type": "string" }
              },
              "required": ["a", "b"]
            }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(schema1),
            TmRefScanner.computeFingerprint(schema2)
        )
    }

    @Test
    fun `different enum values produce different fingerprints`() {
        val schema1 = JsonObject.of("""
            { "type": "string", "enum": ["OK", "WARNING"] }
        """.trimIndent())

        val schema2 = JsonObject.of("""
            { "type": "string", "enum": ["OK", "WARNING", "ERROR"] }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(schema1),
            TmRefScanner.computeFingerprint(schema2)
        )
    }

    @Test
    fun `different const values produce different fingerprints`() {
        val schema1 = JsonObject.of("""
            { "type": "string", "const": "temperature" }
        """.trimIndent())

        val schema2 = JsonObject.of("""
            { "type": "string", "const": "humidity" }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(schema1),
            TmRefScanner.computeFingerprint(schema2)
        )
    }

    @Test
    fun `property order does not affect fingerprint`() {
        val schema1 = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "alpha": { "type": "string" },
                "beta": { "type": "integer" }
              }
            }
        """.trimIndent())

        // Same properties, different JSON order
        val schema2 = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "beta": { "type": "integer" },
                "alpha": { "type": "string" }
              }
            }
        """.trimIndent())

        assertEquals(
            TmRefScanner.computeFingerprint(schema1),
            TmRefScanner.computeFingerprint(schema2),
            "Fingerprints should be stable regardless of JSON property order"
        )
    }

    @Test
    fun `numeric constraints are included in fingerprint`() {
        val withConstraints = JsonObject.of("""
            { "type": "integer", "minimum": 0, "maximum": 60, "multipleOf": 10 }
        """.trimIndent())

        val withoutConstraints = JsonObject.of("""
            { "type": "integer" }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(withConstraints),
            TmRefScanner.computeFingerprint(withoutConstraints)
        )
    }

    @Test
    fun `additionalProperties value is included in fingerprint`() {
        val withFalse = JsonObject.of("""
            { "type": "object", "properties": { "x": { "type": "string" } }, "additionalProperties": false }
        """.trimIndent())

        val withoutIt = JsonObject.of("""
            { "type": "object", "properties": { "x": { "type": "string" } } }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(withFalse),
            TmRefScanner.computeFingerprint(withoutIt)
        )
    }

    @Test
    fun `nested objects are fingerprinted recursively`() {
        val shallow = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "inner": { "type": "object", "properties": { "x": { "type": "string" } } }
              }
            }
        """.trimIndent())

        val deeperInner = JsonObject.of("""
            {
              "type": "object",
              "properties": {
                "inner": { "type": "object", "properties": { "x": { "type": "integer" } } }
              }
            }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(shallow),
            TmRefScanner.computeFingerprint(deeperInner),
            "Different nested property types should produce different fingerprints"
        )
    }

    @Test
    fun `oneOf composition is included in fingerprint`() {
        val withOneOf = JsonObject.of("""
            {
              "oneOf": [
                { "type": "string" },
                { "type": "integer" }
              ]
            }
        """.trimIndent())

        val withoutOneOf = JsonObject.of("""
            {
              "oneOf": [
                { "type": "string" }
              ]
            }
        """.trimIndent())

        val noComposition = JsonObject.of("{}")

        assertNotEquals(
            TmRefScanner.computeFingerprint(withOneOf),
            TmRefScanner.computeFingerprint(withoutOneOf),
            "Different oneOf variants should produce different fingerprints"
        )
        assertNotEquals(
            TmRefScanner.computeFingerprint(withOneOf),
            TmRefScanner.computeFingerprint(noComposition),
            "Schema with oneOf should differ from schema without"
        )
    }

    @Test
    fun `allOf and anyOf composition are included in fingerprint`() {
        val withAllOf = JsonObject.of("""
            {
              "allOf": [
                { "type": "object", "properties": { "a": { "type": "string" } } },
                { "type": "object", "properties": { "b": { "type": "integer" } } }
              ]
            }
        """.trimIndent())

        val withAnyOf = JsonObject.of("""
            {
              "anyOf": [
                { "type": "object", "properties": { "a": { "type": "string" } } },
                { "type": "object", "properties": { "b": { "type": "integer" } } }
              ]
            }
        """.trimIndent())

        assertNotEquals(
            TmRefScanner.computeFingerprint(withAllOf),
            TmRefScanner.computeFingerprint(withAnyOf),
            "allOf and anyOf should produce different fingerprints"
        )
    }
}
