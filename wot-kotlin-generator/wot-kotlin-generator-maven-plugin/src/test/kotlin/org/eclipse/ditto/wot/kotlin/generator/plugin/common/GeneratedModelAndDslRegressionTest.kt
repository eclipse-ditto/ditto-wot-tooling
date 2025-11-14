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

package org.eclipse.ditto.wot.kotlin.generator.plugin.common

import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.wot.kotlin.generator.plugin.ThingModelGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Generated models regression test that can be run independently
 * This test verifies that our compound naming changes work correctly
 * without introducing regressions
 */
class GeneratedModelAndDslRegressionTest {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(GeneratedModelAndDslRegressionTest::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val test = GeneratedModelAndDslRegressionTest()
            test.runRegressionTest()
            System.exit(0)
        }
    }

    private val modelUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld"
    private val outputDir = "standalone-regression-test"
    private val packageName = "org.eclipse.ditto.wot.kotlin.standalone.test"

    fun runRegressionTest() {
        logger.info("Starting Standalone Regression Test")
        logger.info("Model URL: $modelUrl")
        val classLoader = javaClass.getClassLoader()
        val outputDirPath = classLoader.getResource(".").path + outputDir
        logger.info("Output Directory: $outputDirPath")
        logger.info("Package Name: $packageName")
        logger.info("==================================================================================")

        try {
            // Clean up previous test output
            cleanupTestOutput(outputDirPath)

            val thingModelGenerator = ThingModelGenerator

            runBlocking {
                logger.info("Loading model from $modelUrl...")
                val model = thingModelGenerator.loadModel(modelUrl)
                logger.info("Model loaded successfully")

                logger.info("Generating Kotlin classes...")
                thingModelGenerator.generate(model, packageName, outputDirPath)
                logger.info("Code generation completed")

                // Run all verification checks
                verifyBasicGeneration(outputDirPath)
                verifyMainClasses(outputDirPath)
                verifyCompoundNaming(outputDirPath)
                verifyEnumsAndActions(outputDirPath)
                verifyStartPathFunctionality(outputDirPath)
                verifyNoUnresolvedReferences(outputDirPath)

                // New: Check for missing DSL builder methods
                verifyDslBuilderMethods(outputDirPath)

                logger.info("==================================================================================")
                logger.info("All regression tests passed successfully!")
                logger.info("Compound naming is working correctly")
                logger.info("No regressions detected")
            }

        } catch (e: Throwable) {
            logger.error("Regression test failed: ${e.message}", e)
            System.exit(1)
        }
    }

    private fun verifyBasicGeneration(outputDirPath: String) {
        logger.info("Verifying basic generation...")

        val outputPath = Paths.get(outputDirPath)
        if (!Files.exists(outputPath)) {
            throw AssertionError("Output directory does not exist: $outputPath")
        }

        val kotlinFiles = Files.walk(outputPath)
            .filter { it.toString().endsWith(".kt") }
            .count()

        if (kotlinFiles == 0L) {
            throw AssertionError("No Kotlin files were generated")
        }

        logger.info("Generated $kotlinFiles Kotlin files")
    }

    private fun verifyMainClasses(outputDirPath: String) {
        logger.info("Verifying main classes...")

        val expectedClasses = listOf("FloorLamp.kt", "Attributes.kt", "Features.kt")
        val foundClasses = mutableListOf<String>()

        expectedClasses.forEach { className ->
            val filePath = Paths.get(outputDirPath, packageName.replace(".", "/"), className)
            if (Files.exists(filePath)) {
                foundClasses.add(className)
            }
        }

        if (foundClasses.isEmpty()) {
            throw AssertionError("No main classes were generated. Expected at least one of: $expectedClasses")
        }

        logger.info("Found main classes: $foundClasses")
    }

    private fun verifyCompoundNaming(outputDirPath: String) {
        logger.info("Verifying compound naming...")

        val allContent = getAllGeneratedContent(outputDirPath)

        // Regex to match class names with compound patterns (e.g., ending with SubStates, Internal, Item, Properties)
        val compoundPattern = Regex("""class\s+([A-Z][a-zA-Z]+(SubStates|Internal|Item|Properties))""")
        val foundCompoundNames = compoundPattern.findAll(allContent)
            .map { it.groupValues[1] }
            .toSet()

        if (foundCompoundNames.isEmpty()) {
            logger.error("No compound names found. This indicates an issue with compound naming.")
            throw AssertionError("No compound names found. This indicates an issue with compound naming.")
        } else {
            logger.info("Found compound names: $foundCompoundNames")
        }
    }

    private fun verifyEnumsAndActions(outputDirPath: String) {
        logger.info("Verifying enums and actions...")

        val allContent = getAllGeneratedContent(outputDirPath)

        // Count enums
        val enumCount = allContent.split("enum class").size - 1
        val MIN_EXPECTED_ENUMS = 3
        if (enumCount < MIN_EXPECTED_ENUMS) {
            logger.error("Expected at least $MIN_EXPECTED_ENUMS enum classes, but found $enumCount")
            throw AssertionError("Expected at least $MIN_EXPECTED_ENUMS enum classes, but found $enumCount")
        } else {
            logger.info("Found $enumCount enum classes")
        }

        // Count actions
        val actionCount = allContent.split("interface").count { it.contains("Action") }
        val MIN_EXPECTED_ACTIONS = 10
        if (actionCount < MIN_EXPECTED_ACTIONS) {
            logger.error("Expected at least $MIN_EXPECTED_ACTIONS action interfaces, but found $actionCount")
            throw AssertionError("Expected at least $MIN_EXPECTED_ACTIONS action interfaces, but found $actionCount")
        } else {
            logger.info("Found $actionCount action interfaces")
        }

        // Check for specific enums that should exist
        val expectedEnums = listOf("Severity", "State", "Mode")
        val foundEnums = expectedEnums.filter { enumName ->
            allContent.contains("enum class.*$enumName".toRegex(RegexOption.IGNORE_CASE))
        }

        if (foundEnums.isNotEmpty()) {
            logger.info("Found specific enums: $foundEnums")
        }
    }

    private fun verifyStartPathFunctionality(outputDirPath: String) {
        logger.info("Verifying startPath functionality...")

        val allContent = getAllGeneratedContent(outputDirPath)

        // Check for HasPath implementations
        val hasPathCount = allContent.split("HasPath").size - 1
        val MIN_EXPECTED_HASPATH = 49
        if (hasPathCount < MIN_EXPECTED_HASPATH) {
            logger.error("Expected at least $MIN_EXPECTED_HASPATH HasPath implementations, but found $hasPathCount")
            throw AssertionError("Expected at least $MIN_EXPECTED_HASPATH HasPath implementations, but found $hasPathCount")
        } else {
            logger.info("Found $hasPathCount HasPath implementations")
        }

        // Check for startPath functions
        val startPathCount = allContent.split("startPath").size - 1
        val MIN_EXPECTED_STARTPATH = 24
        if (startPathCount < MIN_EXPECTED_STARTPATH) {
            logger.error("Expected at least $MIN_EXPECTED_STARTPATH startPath functions, but found $startPathCount")
            throw AssertionError("Expected at least $MIN_EXPECTED_STARTPATH startPath functions, but found $startPathCount")
        } else {
            logger.info("Found $startPathCount startPath functions")
        }

        // Check for startPath return values
        val startPathPattern = Regex("return \"([^\"]+)\"")
        val startPathValues = startPathPattern.findAll(allContent)
            .map { it.groupValues[1] }
            .toSet()

        if (startPathValues.isNotEmpty()) {
            logger.info("Found startPath values: $startPathValues")
        }
    }

    private fun verifyNoUnresolvedReferences(outputDirPath: String) {
        logger.info("Verifying no unresolved references...")

        val allContent = getAllGeneratedContent(outputDirPath)

        // Check for potential unresolved references
        val unresolvedPatterns = listOf(
            "Internal",  // Should be compound names like ThermostatInternal
            "SubStates", // Should be compound names like ThermostatSubStates
            "Item"       // Should be compound names like ThermostatItem
        )

        var foundIssues = false

        unresolvedPatterns.forEach { pattern ->
            // Look for standalone references that might be unresolved
            val standalonePattern = Regex("\\b$pattern\\b")
            val matches = standalonePattern.findAll(allContent).toList()

            // Filter out legitimate uses
            val problematicMatches = matches.filter { match ->
                val context = allContent.substring(
                    maxOf(0, match.range.first - 50),
                    minOf(allContent.length, match.range.last + 50)
                )
                // Should not be in comments, strings, or part of compound names
                !context.contains("//") &&
                !context.contains("\"") &&
                !context.contains("") &&
                !context.contains("$pattern.")
            }

            if (problematicMatches.isNotEmpty()) {
                logger.warn("Found potential unresolved references for '$pattern': ${problematicMatches.size} instances")
                foundIssues = true
            }
        }

        if (!foundIssues) {
            logger.info("No obvious unresolved references found")
        }
    }

    private fun verifyDslBuilderMethods(outputDirPath: String) {
        logger.info("Verifying presence of DSL builder methods for object properties...")

        val outputPath = Paths.get(outputDirPath)
        val kotlinFiles = Files.walk(outputPath)
            .filter { it.toString().endsWith(".kt") }
            .toList()

        val classToProperties = mutableMapOf<String, List<Pair<String, String>>>()
        val classToMethods = mutableMapOf<String, Set<String>>()

        // First, collect all class names
        val allClassNames = kotlinFiles.mapNotNull { file ->
            Regex("""(data\s+)?class\s+([A-Z][A-Za-z0-9_]*)""")
                .find(Files.readString(file))
                ?.groupValues?.get(2)
        }.toSet()

        // Collect properties and methods for each class
        for (file in kotlinFiles) {
            val content = Files.readString(file)
            val classMatch = Regex("""(data\s+)?class\s+([A-Z][A-Za-z0-9_]*)""").find(content)
            val className = classMatch?.groupValues?.get(2) ?: continue

            // Find properties in primary constructor and as fields
            val propertyPattern = Regex("""var\s+([a-zA-Z0-9_]+):\s*([A-Z][A-Za-z0-9_?]*)""")
            val properties = propertyPattern.findAll(content)
                .map { it.groupValues[1].removePrefix("_") to it.groupValues[2].removeSuffix("?") }
                .toList()
            classToProperties[className] = properties

            // Find all builder methods
            val methodPattern = Regex("""fun\s+`?([a-zA-Z0-9_]+)`?\(block:\s*([A-Z][A-Za-z0-9_]*)\.\(\) -> Unit\)""")
            val methods = methodPattern.findAll(content)
                .map { it.groupValues[1] }
                .toSet()
            classToMethods[className] = methods
        }

        // Now, for each class, check that for every object property, a builder method exists
        val missingMethods = mutableListOf<String>()
        for ((className, properties) in classToProperties) {
            for ((propName, propType) in properties) {
                if (propType in allClassNames) {
                    // Should have a builder method
                    val expectedMethod = propName
                    if (expectedMethod !in classToMethods[className].orEmpty()) {
                        missingMethods.add("$className is missing DSL builder for property '$propName' of type '$propType'")
                    }
                }
            }
        }

        if (missingMethods.isNotEmpty()) {
            logger.error("Missing DSL builder methods detected:\n" + missingMethods.joinToString("\n"))
            throw AssertionError("Missing DSL builder methods detected:\n" + missingMethods.joinToString("\n"))
        } else {
            logger.info("All DSL builder methods for object properties are present.")
        }
    }

    private fun getAllGeneratedContent(outputDirPath: String): String {
        return Files.walk(Paths.get(outputDirPath))
            .filter { it.toString().endsWith(".kt") }
            .map { it.toFile().readText() }
            .toList()
            .joinToString("\n")
    }

    private fun cleanupTestOutput(outputDirPath: String) {
        val outputPath = Paths.get(outputDirPath)
        if (Files.exists(outputPath)) {
            Files.walk(outputPath)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
            logger.info("Cleaned up previous test output")
        }
    }
}