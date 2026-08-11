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
package org.eclipse.ditto.wot.kotlin.generator.plugin.common

import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.kotlin.generator.plugin.ThingModelGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.model.ThingModel
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests generation of `patternProperties` maps whose value schema is a scalar carrying an `enum`.
 *
 * The value type of such a map resolves to a bare `ClassName("", "<Prop>Item")` — a type the *enclosing* class is
 * expected to nest. Map wrapper classes resolve their value type before their own `TypeSpec` exists, so nothing
 * nested it and a type alias got written over the name instead, yielding `typealias XItem = XItem`, which does not
 * compile. The enum is now written as its own file.
 *
 * Maps over plain primitives must keep using a type alias, so both are asserted here.
 */
class PatternPropertiesEnumValueGenerationTest {

    @TempDir
    lateinit var outputDir: Path

    private fun generate(propertiesJson: String, pkg: String, strategy: EnumGenerationStrategy) = runBlocking {
        val thingModel = ThingModel.fromJson(
            JsonObject.of(
                """
                {
                  "@context": "https://www.w3.org/2022/wot/td/v1.1",
                  "@type": "tm:ThingModel",
                  "title": "Test",
                  "version": { "model": "1.0.0" },
                  "properties": { $propertiesJson }
                }
                """.trimIndent()
            )
        )

        ThingModelGenerator.generate(
            thingModel,
            GeneratorConfiguration(
                thingModelUrl = "in-memory",
                outputPackage = pkg,
                outputDirectory = outputDir.toFile(),
                enumGenerationStrategy = strategy
            )
        )
    }

    private fun readGenerated(pkg: String, simpleName: String): String {
        val file = outputDir.resolve("${pkg.replace('.', '/')}/attributes/$simpleName.kt")
        assertTrue(Files.exists(file), "Expected generated file at: $file")
        return Files.readString(file)
    }

    private val energySourceByYear = """
        "primaryEnergySource": {
          "title": "Primary energy source",
          "type": "object",
          "patternProperties": {
            "^\\d{4}$": {
              "type": "string",
              "enum": ["OIL", "GAS", "HEAT_PUMP"]
            }
          },
          "additionalProperties": false
        }
    """.trimIndent()

    @Test
    fun `enum valued pattern properties map generates the item enum instead of a self referential alias`() {
        val pkg = "org.eclipse.ditto.wot.kotlin.generator.plugin.patterntest.inlineenum"
        generate(energySourceByYear, pkg, EnumGenerationStrategy.INLINE)

        val item = readGenerated(pkg, "PrimaryEnergySourceItem")
        assertTrue(
            item.contains("enum class PrimaryEnergySourceItem"),
            "Expected the map value to be generated as an enum class, but got:\n$item"
        )
        listOf("OIL", "GAS", "HEAT_PUMP").forEach { constant ->
            assertTrue(item.contains(constant), "Expected enum constant <$constant> in:\n$item")
        }
        assertFalse(
            item.contains("typealias PrimaryEnergySourceItem = PrimaryEnergySourceItem"),
            "The item must not be a type alias to itself"
        )
    }

    @Test
    fun `enum valued pattern properties map is not affected by the enum generation strategy`() {
        val pkg = "org.eclipse.ditto.wot.kotlin.generator.plugin.patterntest.separateclass"
        generate(energySourceByYear, pkg, EnumGenerationStrategy.SEPARATE_CLASS)

        val item = readGenerated(pkg, "PrimaryEnergySourceItem")
        assertTrue(
            item.contains("enum class PrimaryEnergySourceItem"),
            "SEPARATE_CLASS must produce the same item enum, but got:\n$item"
        )
    }

    @Test
    fun `primitive valued pattern properties map still generates a type alias`() {
        val pkg = "org.eclipse.ditto.wot.kotlin.generator.plugin.patterntest.primitive"
        generate(
            """
            "yearlyCost": {
              "title": "Yearly cost",
              "type": "object",
              "patternProperties": {
                "^\\d{4}$": { "type": "number" }
              },
              "additionalProperties": false
            }
            """.trimIndent(),
            pkg,
            EnumGenerationStrategy.INLINE
        )

        val item = readGenerated(pkg, "YearlyCostItem")
        assertEquals(
            true,
            item.contains("typealias YearlyCostItem = Double"),
            "A primitive map value must stay a type alias, but got:\n$item"
        )
    }
}
