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

import com.squareup.kotlinpoet.TypeSpec
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnumRegistryTest {

    @BeforeTest
    fun resetState() {
        EnumRegistry.clear()
    }

    @AfterTest
    fun cleanupState() {
        EnumRegistry.clear()
    }

    @Test
    fun `package lookup keeps same enum name isolated by package`() {
        EnumRegistry.registerEnum(
            TypeSpec.enumBuilder("Mode").addEnumConstant("AUTO").build(),
            "org.eclipse.ditto.pkg.one",
            setOf("AUTO")
        )
        EnumRegistry.registerEnum(
            TypeSpec.enumBuilder("Mode").addEnumConstant("MANUAL").build(),
            "org.eclipse.ditto.pkg.two",
            setOf("MANUAL")
        )

        assertEquals(
            setOf("AUTO"),
            EnumRegistry.getEnumValuesInPackage("org.eclipse.ditto.pkg.one", "Mode")
        )
        assertEquals(
            setOf("MANUAL"),
            EnumRegistry.getEnumValuesInPackage("org.eclipse.ditto.pkg.two", "Mode")
        )
        assertEquals(
            setOf("AUTO"),
            EnumRegistry.getEnumByNameInPackage("Mode", "org.eclipse.ditto.pkg.one")?.enumConstants?.keys
        )
        assertEquals(
            setOf("MANUAL"),
            EnumRegistry.getEnumByNameInPackage("Mode", "org.eclipse.ditto.pkg.two")?.enumConstants?.keys
        )
    }

    @Test
    fun `inline lookup does not fall back to package scoped enums`() {
        EnumRegistry.registerEnum(
            TypeSpec.enumBuilder("Status").addEnumConstant("OK").build(),
            "org.eclipse.ditto.pkg.one",
            setOf("OK")
        )

        assertNull(EnumRegistry.getInlineEnumByName("Status"))

        EnumRegistry.registerEnumInline(TypeSpec.enumBuilder("Status").addEnumConstant("LOCAL").build())

        assertEquals(
            setOf("LOCAL"),
            EnumRegistry.getInlineEnumByName("Status")?.enumConstants?.keys
        )
    }
}
