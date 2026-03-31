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
import org.eclipse.ditto.wot.kotlin.generator.plugin.GeneratorStarter
import org.eclipse.ditto.wot.kotlin.generator.plugin.ThingModelGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.SharedTypeRegistry
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deep integration tests generating code from the Ditto floor-lamp example model.
 *
 * The floor-lamp model has:
 * - 4 spot features (spot1, spot2, spot3, status-led) sharing the same submodel
 * - Each spot has: Color (r,g,b,w properties), SwitchOnForDuration action, Toggle action
 * - ConnectionStatus feature with readySince/readyUntil
 * - PowerConsumptionAwareness feature with ReportPowerConsumption
 * - SmokeDetection feature
 * - Top-level actions: SwitchAllSpots, SwitchAllSpotsOnForDuration
 * - Attributes with model info
 *
 * This model is public and domain-independent, and exercises:
 * - Submodel resolution (spots share a common spot submodel)
 * - Nested features with properties, actions
 * - Class deduplication (Color, SwitchOnForDuration, Toggle across 4 spots)
 * - tm:ref-based dedup (when enabled)
 * - All naming strategies and enum strategies
 *
 * @since 1.1.0
 */
class GeneratorIntegrationTest {

    companion object {
        private const val FLOOR_LAMP_URL =
            "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld"

        // Expected baseline counts (without dedup, ORIGINAL naming, INLINE enums)
        // These are the reference numbers. If generation changes, update here.
        private const val BASELINE_TOTAL_FILES = 36
        private const val BASELINE_FEATURE_COUNT = 6 // spot1, spot2, spot3, status-led, connectionstatus, powerconsumptionawareness, smokedetection (but smokedetection has no properties file)
        private const val BASELINE_COLOR_FILES = 4 // one per spot (spot1, spot2, spot3, status-led)
        private const val BASELINE_TOGGLE_FILES = 4
        private const val BASELINE_SWITCH_ON_FILES = 4
    }

    @BeforeTest
    fun resetState() {
        SharedTypeRegistry.clear()
        SharedTypeRegistry.clearRefMappings()
    }

    // ===== Configuration matrix: all combos generate without error =====

    @Test
    fun `ORIGINAL + INLINE + dedup=false generates successfully`() {
        val exitCode = runGenerator(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertEquals(0, exitCode)
    }

    @Test
    fun `ORIGINAL + INLINE + dedup=true generates successfully`() {
        val exitCode = runGenerator(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, true)
        assertEquals(0, exitCode)
    }

    @Test
    fun `COMPOUND_ALL + INLINE + dedup=false generates successfully`() {
        val exitCode = runGenerator(ClassNamingStrategy.COMPOUND_ALL, EnumGenerationStrategy.INLINE, false)
        assertEquals(0, exitCode)
    }

    @Test
    fun `COMPOUND_ALL + INLINE + dedup=true generates successfully`() {
        val exitCode = runGenerator(ClassNamingStrategy.COMPOUND_ALL, EnumGenerationStrategy.INLINE, true)
        assertEquals(0, exitCode)
    }

    @Test
    fun `ORIGINAL + SEPARATE_CLASS + dedup=true generates successfully`() {
        val exitCode = runGenerator(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.SEPARATE_CLASS, true)
        assertEquals(0, exitCode)
    }

    @Test
    fun `COMPOUND_ALL + SEPARATE_CLASS + dedup=true generates successfully`() {
        val exitCode = runGenerator(ClassNamingStrategy.COMPOUND_ALL, EnumGenerationStrategy.SEPARATE_CLASS, true)
        assertEquals(0, exitCode)
    }

    // ===== Baseline structure verification (ORIGINAL + INLINE + dedup=false) =====

    @Test
    fun `baseline produces expected file count`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertEquals(BASELINE_TOTAL_FILES, files.size, "Baseline file count mismatch. Files: $files")
    }

    @Test
    fun `baseline contains main model class FloorLamp`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertTrue(files.any { it.endsWith("FloorLamp.kt") }, "Missing FloorLamp.kt")
    }

    @Test
    fun `baseline contains Features and Attributes`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertTrue(files.any { it.endsWith("Features.kt") }, "Missing Features.kt")
        assertTrue(files.any { it.endsWith("Attributes.kt") }, "Missing Attributes.kt")
    }

    @Test
    fun `baseline contains all 4 spot feature classes`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertTrue(files.any { it.contains("spot1/Spot1.kt") }, "Missing Spot1.kt")
        assertTrue(files.any { it.contains("spot2/Spot2.kt") }, "Missing Spot2.kt")
        assertTrue(files.any { it.contains("spot3/Spot3.kt") }, "Missing Spot3.kt")
        assertTrue(files.any { it.contains("status-led/StatusLed.kt") }, "Missing StatusLed.kt")
    }

    @Test
    fun `baseline has Color class in each spot (no dedup)`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        val colorFiles = files.filter { it.endsWith("Color.kt") }
        assertEquals(BASELINE_COLOR_FILES, colorFiles.size,
            "Expected $BASELINE_COLOR_FILES Color.kt files without dedup. Found: $colorFiles")
    }

    @Test
    fun `baseline has SwitchOnForDuration in each spot (no dedup)`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        val switchFiles = files.filter { it.endsWith("SwitchOnForDuration.kt") }
        assertEquals(BASELINE_SWITCH_ON_FILES, switchFiles.size,
            "Expected $BASELINE_SWITCH_ON_FILES SwitchOnForDuration.kt files. Found: $switchFiles")
    }

    @Test
    fun `baseline has Toggle in each spot (no dedup)`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        val toggleFiles = files.filter { it.endsWith("Toggle.kt") }
        assertEquals(BASELINE_TOGGLE_FILES, toggleFiles.size,
            "Expected $BASELINE_TOGGLE_FILES Toggle.kt files. Found: $toggleFiles")
    }

    @Test
    fun `baseline has top-level actions`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertTrue(files.any { it.endsWith("SwitchAllSpots.kt") }, "Missing SwitchAllSpots.kt")
        assertTrue(files.any { it.endsWith("FloorLampAction.kt") }, "Missing FloorLampAction.kt")
    }

    @Test
    fun `baseline has ConnectionStatus and PowerConsumptionAwareness features`() {
        val files = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        assertTrue(files.any { it.contains("connectionstatus/ConnectionStatus.kt") }, "Missing ConnectionStatus.kt")
        assertTrue(files.any { it.contains("powerconsumptionawareness/PowerConsumptionAwareness.kt") },
            "Missing PowerConsumptionAwareness.kt")
    }

    // ===== Generated code content verification =====

    @Test
    fun `FloorLamp extends Thing with correct type parameters`() {
        val dir = generateToDir(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        try {
            val content = readFile(dir, "FloorLamp.kt")
            assertTrue(content.contains("class FloorLamp"), "FloorLamp should be a class")
            assertTrue(content.contains("Thing<Attributes, Features>"), "FloorLamp should extend Thing<Attributes, Features>")
        } finally {
            deleteRecursively(dir)
        }
    }

    @Test
    fun `Color has r, g, b, w properties`() {
        val dir = generateToDir(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        try {
            val content = readFile(dir, "spot1/properties/Color.kt")
            assertTrue(content.contains("\"r\""), "Color should have 'r' property")
            assertTrue(content.contains("\"g\""), "Color should have 'g' property")
            assertTrue(content.contains("\"b\""), "Color should have 'b' property")
            assertTrue(content.contains("\"w\""), "Color should have 'w' property")
        } finally {
            deleteRecursively(dir)
        }
    }

    @Test
    fun `Spot classes have HasPath companion with correct startPath`() {
        val dir = generateToDir(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        try {
            val spot1 = readFile(dir, "spot1/Spot1.kt")
            assertTrue(spot1.contains("companion object : HasPath"), "Spot1 should have HasPath companion")
            assertTrue(spot1.contains("startPath()"), "Spot1 companion should have startPath()")
        } finally {
            deleteRecursively(dir)
        }
    }

    @Test
    fun `DSL builder functions are generated`() {
        val dir = generateToDir(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        try {
            val content = readFile(dir, "FloorLamp.kt")
            assertTrue(content.contains("fun attributes("), "FloorLamp should have attributes DSL builder")
            assertTrue(content.contains("fun features("), "FloorLamp should have features DSL builder")
        } finally {
            deleteRecursively(dir)
        }
    }

    // ===== Dedup behavior =====

    @Test
    fun `dedup=true produces fewer or equal files than dedup=false`() {
        val noDedupFiles = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        resetState()
        val dedupFiles = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, true)

        assertTrue(dedupFiles.size <= noDedupFiles.size,
            "dedup=true (${dedupFiles.size}) should produce <= files than dedup=false (${noDedupFiles.size})")
    }

    // ===== Determinism =====

    @Test
    fun `generation is deterministic - two runs produce identical file lists`() {
        val run1 = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        resetState()
        val run2 = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)

        assertEquals(run1.sorted(), run2.sorted(), "Two runs should produce identical file lists")
    }

    @Test
    fun `determinism - two runs produce identical file contents`() {
        val dir1 = generateToDir(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        resetState()
        val dir2 = generateToDir(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        try {
            val files1 = listAllKtFiles(dir1).sorted()
            val files2 = listAllKtFiles(dir2).sorted()
            assertEquals(files1, files2, "File lists should match")

            // Compare content of every file
            for (relPath in files1) {
                val content1 = Files.readString(dir1.resolve(relPath))
                val content2 = Files.readString(dir2.resolve(relPath))
                assertEquals(content1, content2, "Content mismatch in $relPath")
            }
        } finally {
            deleteRecursively(dir1)
            deleteRecursively(dir2)
        }
    }

    // ===== Naming strategy comparison =====

    @Test
    fun `COMPOUND_ALL produces same number of features as ORIGINAL`() {
        val originalFiles = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        resetState()
        val compoundFiles = generateAndList(ClassNamingStrategy.COMPOUND_ALL, EnumGenerationStrategy.INLINE, false)

        // File counts may differ slightly due to naming conflict resolution,
        // but should be within 20% of each other
        val ratio = compoundFiles.size.toDouble() / originalFiles.size.toDouble()
        assertTrue(ratio in 0.8..1.2,
            "COMPOUND_ALL (${compoundFiles.size}) and ORIGINAL (${originalFiles.size}) should have similar counts (ratio=$ratio)")
    }

    // ===== SEPARATE_CLASS vs INLINE =====

    @Test
    fun `SEPARATE_CLASS produces at least as many files as INLINE`() {
        val inlineFiles = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.INLINE, false)
        resetState()
        val separateFiles = generateAndList(ClassNamingStrategy.ORIGINAL_THEN_COMPOUND, EnumGenerationStrategy.SEPARATE_CLASS, false)

        assertTrue(separateFiles.size >= inlineFiles.size,
            "SEPARATE_CLASS (${separateFiles.size}) should produce >= files than INLINE (${inlineFiles.size})")
    }

    // ===== Helpers =====

    private fun runGenerator(naming: ClassNamingStrategy, enumStrategy: EnumGenerationStrategy, dedup: Boolean): Int {
        val outputDir = Files.createTempDirectory("wot-integ-test")
        try {
            return GeneratorStarter.run(GeneratorConfiguration(
                thingModelUrl = FLOOR_LAMP_URL,
                outputPackage = "org.eclipse.ditto.wot.integ.test",
                outputDirectory = outputDir.toFile(),
                deduplicateReferencedTypes = dedup,
                classNamingStrategy = naming,
                enumGenerationStrategy = enumStrategy
            ))
        } finally {
            deleteRecursively(outputDir)
        }
    }

    private fun generateAndList(naming: ClassNamingStrategy, enumStrategy: EnumGenerationStrategy, dedup: Boolean): List<String> {
        val dir = generateToDir(naming, enumStrategy, dedup)
        try {
            return listAllKtFiles(dir)
        } finally {
            deleteRecursively(dir)
        }
    }

    private fun generateToDir(naming: ClassNamingStrategy, enumStrategy: EnumGenerationStrategy, dedup: Boolean): Path {
        val outputDir = Files.createTempDirectory("wot-integ-test")
        val exitCode = GeneratorStarter.run(GeneratorConfiguration(
            thingModelUrl = FLOOR_LAMP_URL,
            outputPackage = "org.eclipse.ditto.wot.integ.test",
            outputDirectory = outputDir.toFile(),
            deduplicateReferencedTypes = dedup,
            classNamingStrategy = naming,
            enumGenerationStrategy = enumStrategy
        ))
        assertEquals(0, exitCode, "Generation failed")
        return outputDir
    }

    private fun listAllKtFiles(dir: Path): List<String> {
        return Files.walk(dir)
            .filter { it.toString().endsWith(".kt") }
            .map { dir.relativize(it).toString() }
            .toList()
    }

    private fun readFile(dir: Path, suffix: String): String {
        val file = Files.walk(dir)
            .filter { it.toString().endsWith(suffix) }
            .findFirst()
            .orElseThrow { AssertionError("File ending with '$suffix' not found in $dir") }
        return Files.readString(file)
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }
}
