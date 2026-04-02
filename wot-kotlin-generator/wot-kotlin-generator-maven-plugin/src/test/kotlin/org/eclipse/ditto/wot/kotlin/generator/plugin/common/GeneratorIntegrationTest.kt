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

import org.eclipse.ditto.wot.kotlin.generator.plugin.GeneratorStarter
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deep integration tests generating from Ditto floor-lamp model (36 files baseline).
 * Portable across generator versions (with and without dedup support).
 */
class GeneratorIntegrationTest {

    companion object {
        private const val FLOOR_LAMP_URL =
            "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld"
        private const val BASELINE_TOTAL_FILES = 36
        private const val BASELINE_COLOR_FILES = 4
        private const val BASELINE_TOGGLE_FILES = 4
        private const val BASELINE_SWITCH_ON_FILES = 4
    }

    @Test fun `ORIGINAL + INLINE generates`() = assertGenerates(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE)
    @Test fun `COMPOUND_ALL + INLINE generates`() = assertGenerates(ClassNamingStrategy.COMPOUND_ALL, EnumGenerationStrategy.INLINE)
    @Test fun `ORIGINAL + SEPARATE_CLASS generates`() = assertGenerates(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.SEPARATE_CLASS)
    @Test fun `COMPOUND_ALL + SEPARATE_CLASS generates`() = assertGenerates(ClassNamingStrategy.COMPOUND_ALL, EnumGenerationStrategy.SEPARATE_CLASS)

    @Test fun `baseline file count`() {
        assertEquals(BASELINE_TOTAL_FILES, gen().size, "Files:\n${gen().sorted().joinToString("\n")}")
    }
    @Test fun `has FloorLamp`() = assertTrue(gen().any { it.endsWith("FloorLamp.kt") })
    @Test fun `has Features and Attributes`() { val f = gen(); assertTrue(f.any { it.endsWith("Features.kt") }); assertTrue(f.any { it.endsWith("Attributes.kt") }) }
    @Test fun `has 4 spots`() { val f = gen(); for (s in listOf("Spot1","Spot2","Spot3","StatusLed")) assertTrue(f.any { it.endsWith("$s.kt") }, "Missing $s") }
    @Test fun `has 4 Color files`() = assertEquals(BASELINE_COLOR_FILES, gen().count { it.endsWith("Color.kt") })
    @Test fun `has 4 Toggle files`() = assertEquals(BASELINE_TOGGLE_FILES, gen().count { it.endsWith("Toggle.kt") })
    @Test fun `has 4 SwitchOnForDuration files`() = assertEquals(BASELINE_SWITCH_ON_FILES, gen().count { it.endsWith("SwitchOnForDuration.kt") })
    @Test fun `has top-level actions`() { val f = gen(); assertTrue(f.any { it.endsWith("SwitchAllSpots.kt") }); assertTrue(f.any { it.endsWith("FloorLampAction.kt") }) }
    @Test fun `has ConnectionStatus`() = assertTrue(gen().any { it.contains("connectionstatus") && it.endsWith("ConnectionStatus.kt") })

    @Test fun `FloorLamp extends Thing`() { val c = readGen("FloorLamp.kt"); assertTrue(c.contains("class FloorLamp")); assertTrue(c.contains("Thing<Attributes, Features>")) }
    @Test fun `Color has rgbw`() { val c = readGen("spot1/properties/Color.kt"); for (p in "rgbw".toList()) assertTrue(c.contains("\"$p\""), "Missing '$p'") }
    @Test fun `Spot has HasPath`() { val c = readGen("spot1/Spot1.kt"); assertTrue(c.contains("companion object : HasPath")); assertTrue(c.contains("startPath()")) }
    @Test fun `DSL builders exist`() { val c = readGen("FloorLamp.kt"); assertTrue(c.contains("fun attributes(")); assertTrue(c.contains("fun features(")) }

    @Test fun `deterministic file lists`() = assertEquals(gen().sorted(), gen().sorted())
    @Test fun `deterministic file contents`() {
        val d1 = genDir(); val d2 = genDir()
        try { for (f in list(d1).sorted()) assertEquals(Files.readString(d1.resolve(f)), Files.readString(d2.resolve(f)), "Diff: $f") }
        finally { del(d1); del(d2) }
    }

    @Test fun `COMPOUND and ORIGINAL similar counts`() {
        val o = gen(); val c = gen(ClassNamingStrategy.COMPOUND_ALL)
        assertTrue(c.size.toDouble() / o.size in 0.8..1.2, "COMPOUND(${c.size}) vs ORIGINAL(${o.size})")
    }
    @Test fun `SEPARATE_CLASS produces at least as many files as INLINE`() {
        val i = gen(); val s = gen(enumStrategy = EnumGenerationStrategy.SEPARATE_CLASS)
        assertTrue(s.size >= i.size, "SEPARATE(${s.size}) vs INLINE(${i.size})")
    }

    // --- helpers ---
    private fun assertGenerates(n: ClassNamingStrategy, e: EnumGenerationStrategy) = assertEquals(0, run(n, e))
    private fun gen(n: ClassNamingStrategy = ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, enumStrategy: EnumGenerationStrategy = EnumGenerationStrategy.INLINE): List<String> {
        val d = genDir(n, enumStrategy); try { return list(d) } finally { del(d) }
    }
    private fun readGen(suffix: String): String { val d = genDir(); try { return read(d, suffix) } finally { del(d) } }
    private fun genDir(n: ClassNamingStrategy = ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, e: EnumGenerationStrategy = EnumGenerationStrategy.INLINE): Path {
        val d = Files.createTempDirectory("wot-integ"); assertEquals(0, run(n, e, d)); return d
    }
    private fun run(n: ClassNamingStrategy, e: EnumGenerationStrategy, d: Path = Files.createTempDirectory("wot-integ")): Int {
        try { return GeneratorStarter.run(GeneratorConfiguration(thingModelUrl = FLOOR_LAMP_URL, outputPackage = "org.eclipse.ditto.wot.integ.test", outputDirectory = d.toFile(), classNamingStrategy = n, enumGenerationStrategy = e))
        } finally { if (d.toString().contains("wot-integ") && !Files.walk(d).anyMatch { it.toString().endsWith(".kt") }) del(d) }
    }
    private fun list(d: Path): List<String> = Files.walk(d).filter { it.toString().endsWith(".kt") }.map { d.relativize(it).toString() }.toList()
    private fun read(d: Path, s: String): String = Files.readString(Files.walk(d).filter { it.toString().endsWith(s) }.findFirst().orElseThrow { AssertionError("Not found: $s") })
    private fun del(p: Path) { if (Files.exists(p)) Files.walk(p).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
}
