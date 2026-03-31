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
package org.eclipse.ditto.wot.kotlin.generator.plugin.clazz

import com.squareup.kotlinpoet.ClassName
import org.eclipse.ditto.json.JsonFactory
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.model.DataSchemaType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedTypeRegistryTest {

    @BeforeTest
    fun resetState() {
        SharedTypeRegistry.clear()
        SharedTypeRegistry.clearRefMappings()
    }

    @AfterTest
    fun cleanupState() {
        SharedTypeRegistry.clear()
        SharedTypeRegistry.clearRefMappings()
    }

    @Test
    fun `exact package lookup prefers the matching package for same enum name`() {
        SharedTypeRegistry.registerEnum(
            setOf("AUTO"),
            DataSchemaType.STRING,
            ClassName("org.eclipse.ditto.pkg.one", "Mode")
        )
        SharedTypeRegistry.registerEnum(
            setOf("MANUAL"),
            DataSchemaType.STRING,
            ClassName("org.eclipse.ditto.pkg.two", "Mode")
        )

        assertEquals(
            "org.eclipse.ditto.pkg.one",
            SharedTypeRegistry.findExistingEnumByNameInPackage("Mode", "org.eclipse.ditto.pkg.one")?.packageName
        )
        assertEquals(
            "org.eclipse.ditto.pkg.two",
            SharedTypeRegistry.findExistingEnumByNameInPackage("Mode", "org.eclipse.ditto.pkg.two")?.packageName
        )
    }

    // --- Conflicting title detection tests ---

    @Test
    fun `markConflictingTitles detects two refs with same title`() {
        val refA = "https://models.example.org/severity-2.1.1.tm.jsonld#/properties/severity"
        val refB = "https://models.example.org/severity-info-1.0.0.tm.jsonld#/properties/severity"

        SharedTypeRegistry.registerRefTitle(refA, "Severity")
        SharedTypeRegistry.registerRefTitle(refB, "Severity")
        SharedTypeRegistry.markConflictingTitles()

        assertTrue(SharedTypeRegistry.hasConflictingTitle(refA), "refA should be marked as conflicting")
        assertTrue(SharedTypeRegistry.hasConflictingTitle(refB), "refB should be marked as conflicting")
    }

    @Test
    fun `markConflictingTitles does not flag unique titles`() {
        val refA = "https://models.example.org/day.tm.jsonld#/properties/day"
        val refB = "https://models.example.org/transition.tm.jsonld#/properties/transition"

        SharedTypeRegistry.registerRefTitle(refA, "Day")
        SharedTypeRegistry.registerRefTitle(refB, "Transition")
        SharedTypeRegistry.markConflictingTitles()

        assertFalse(SharedTypeRegistry.hasConflictingTitle(refA))
        assertFalse(SharedTypeRegistry.hasConflictingTitle(refB))
    }

    @Test
    fun `markConflictingTitles uses asClassName normalization for conflict detection`() {
        // "severity-info" and "Severity Info" both produce class name "SeverityInfo"
        val refA = "https://models.example.org/a.tm.jsonld#/properties/a"
        val refB = "https://models.example.org/b.tm.jsonld#/properties/b"

        SharedTypeRegistry.registerRefTitle(refA, "severity-info")
        SharedTypeRegistry.registerRefTitle(refB, "Severity Info")
        SharedTypeRegistry.markConflictingTitles()

        assertTrue(SharedTypeRegistry.hasConflictingTitle(refA), "hyphenated title should conflict")
        assertTrue(SharedTypeRegistry.hasConflictingTitle(refB), "spaced title should conflict")
    }

    @Test
    fun `clearRefMappings resets conflicting title state`() {
        val refA = "https://models.example.org/a.tm.jsonld#/properties/a"
        val refB = "https://models.example.org/b.tm.jsonld#/properties/b"

        SharedTypeRegistry.registerRefTitle(refA, "Severity")
        SharedTypeRegistry.registerRefTitle(refB, "Severity")
        SharedTypeRegistry.markConflictingTitles()
        assertTrue(SharedTypeRegistry.hasConflictingTitle(refA))

        SharedTypeRegistry.clearRefMappings()
        assertFalse(SharedTypeRegistry.hasConflictingTitle(refA), "should be cleared after clearRefMappings")
    }

    // --- Ref count and title-based naming tests ---

    @Test
    fun `ref count increments for each reference`() {
        val refUrl = "https://models.example.org/day.tm.jsonld#/properties/day"

        assertEquals(0, SharedTypeRegistry.getRefCount(refUrl))
        SharedTypeRegistry.incrementRefCount(refUrl)
        assertEquals(1, SharedTypeRegistry.getRefCount(refUrl))
        SharedTypeRegistry.incrementRefCount(refUrl)
        SharedTypeRegistry.incrementRefCount(refUrl)
        assertEquals(3, SharedTypeRegistry.getRefCount(refUrl))
    }

    @Test
    fun `ref title is stored and retrievable`() {
        val refUrl = "https://models.example.org/day.tm.jsonld#/properties/day"

        assertNull(SharedTypeRegistry.findRefTitle(refUrl))
        SharedTypeRegistry.registerRefTitle(refUrl, "Day")
        assertEquals("Day", SharedTypeRegistry.findRefTitle(refUrl))
    }

    @Test
    fun `single-referenced tm-ref should not use title for naming`() {
        // Simulates the naming decision: a ref with count=1 should keep property name
        val refUrl = "https://models.example.org/severity-info.tm.jsonld#/properties/severity"
        SharedTypeRegistry.registerRefTitle(refUrl, "Severity")
        SharedTypeRegistry.incrementRefCount(refUrl)

        // count == 1, so the generator should use the property name, not the title
        assertTrue(SharedTypeRegistry.getRefCount(refUrl) <= 1)
    }

    @Test
    fun `multi-referenced tm-ref with unique title should use title for naming`() {
        // Simulates "Day" schema referenced 7 times by monday..sunday
        val refUrl = "https://models.example.org/day.tm.jsonld#/properties/day"
        SharedTypeRegistry.registerRefTitle(refUrl, "Day")
        repeat(7) { SharedTypeRegistry.incrementRefCount(refUrl) }
        SharedTypeRegistry.markConflictingTitles()

        assertTrue(SharedTypeRegistry.getRefCount(refUrl) > 1, "should be multi-referenced")
        assertFalse(SharedTypeRegistry.hasConflictingTitle(refUrl), "title should not be conflicting")
        assertEquals("Day", SharedTypeRegistry.findRefTitle(refUrl))
        // Generator should use "Day" as class name instead of "Monday"
    }

    @Test
    fun `multi-referenced tm-ref with conflicting title should fall back to property name`() {
        // Simulates "severity" and "severityInfo" both having title "Severity"
        val refSeverity = "https://models.example.org/severity.tm.jsonld#/properties/severity"
        val refSeverityInfo = "https://models.example.org/severity-info.tm.jsonld#/properties/severity"

        SharedTypeRegistry.registerRefTitle(refSeverity, "Severity")
        SharedTypeRegistry.registerRefTitle(refSeverityInfo, "Severity")
        repeat(16) { SharedTypeRegistry.incrementRefCount(refSeverity) }
        repeat(16) { SharedTypeRegistry.incrementRefCount(refSeverityInfo) }
        SharedTypeRegistry.markConflictingTitles()

        assertTrue(SharedTypeRegistry.getRefCount(refSeverity) > 1)
        assertTrue(SharedTypeRegistry.getRefCount(refSeverityInfo) > 1)
        assertTrue(SharedTypeRegistry.hasConflictingTitle(refSeverity), "should detect conflict")
        assertTrue(SharedTypeRegistry.hasConflictingTitle(refSeverityInfo), "should detect conflict")
        // Generator should fall back to property names "severity" and "severityInfo"
    }

    // --- normalizeSchema key-order independence ---

    @Test
    fun `findExistingClass matches schemas with different key order`() {
        val schemaA = JsonObject.of("""{"type":"object","properties":{"name":{"type":"string"},"code":{"type":"integer"}},"required":["name","code"]}""")
        val schemaB = JsonObject.of("""{"required":["name","code"],"type":"object","properties":{"code":{"type":"integer"},"name":{"type":"string"}}}""")
        val className = ClassName("org.example", "MyClass")

        SharedTypeRegistry.registerClass(schemaA, className)
        val found = SharedTypeRegistry.findExistingClass(schemaB)

        assertNotNull(found, "Should find class registered with differently-ordered keys")
        assertEquals("MyClass", found.simpleName)
    }

    @Test
    fun `findExistingClass matches schemas with different nested key order`() {
        val schemaA = JsonObject.of("""{"type":"object","properties":{"inner":{"type":"object","properties":{"b":{"type":"string"},"a":{"type":"integer"}}}}}""")
        val schemaB = JsonObject.of("""{"properties":{"inner":{"properties":{"a":{"type":"integer"},"b":{"type":"string"}},"type":"object"}},"type":"object"}""")
        val className = ClassName("org.example", "Nested")

        SharedTypeRegistry.registerClass(schemaA, className)
        assertNotNull(SharedTypeRegistry.findExistingClass(schemaB), "Should match nested schemas with different key order")
    }

    @Test
    fun `findExistingClass does NOT match structurally different schemas`() {
        val schemaA = JsonObject.of("""{"type":"object","properties":{"name":{"type":"string"}}}""")
        val schemaB = JsonObject.of("""{"type":"object","properties":{"name":{"type":"integer"}}}""")

        SharedTypeRegistry.registerClass(schemaA, ClassName("org.example", "A"))
        assertNull(SharedTypeRegistry.findExistingClass(schemaB), "Different schemas should not match")
    }

    @Test
    fun `clearRefMappings also resets refCountRegistry`() {
        val refUrl = "https://example.org/test.tm.jsonld#/properties/test"
        SharedTypeRegistry.incrementRefCount(refUrl)
        SharedTypeRegistry.incrementRefCount(refUrl)
        assertEquals(2, SharedTypeRegistry.getRefCount(refUrl))

        SharedTypeRegistry.clearRefMappings()
        assertEquals(0, SharedTypeRegistry.getRefCount(refUrl), "refCount should be 0 after clearRefMappings")
    }
}
