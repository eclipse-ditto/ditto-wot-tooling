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
package org.eclipse.ditto.wot.kotlin.generator.plugin.util

import org.eclipse.ditto.wot.kotlin.generator.plugin.config.ClassNamingStrategy

/**
 * Converts a string to a valid Kotlin class name.
 *
 * Transforms the input string by converting it to PascalCase format,
 * handling special characters and ensuring valid Kotlin identifier syntax.
 *
 * @param name The input string to convert
 * @return A valid Kotlin class name in PascalCase
 */
fun asClassName(name: String): String {
    return asSpaceSeparated(name).split(" ").map { allUppercaseToLowercase(it) }.map { splitByUppercase(it) }.flatten()
        .map { it.lowercase().replaceFirstChar { it.titlecase() } }.joinToString("")
}

/**
 * Generates a class name based on the specified naming strategy.
 *
 * @param originalName The original name from the schema
 * @param parentName Optional parent name for compound naming
 * @param strategy The naming strategy to use
 * @param usedNames Set of already used class names in the current context (for conflict detection)
 * @return The generated class name
 */
fun asClassNameWithStrategy(
    originalName: String,
    parentName: String? = null,
    strategy: ClassNamingStrategy,
    usedNames: Set<String> = emptySet()
): String {
    return when (strategy) {
        ClassNamingStrategy.COMPOUND_ALL -> {
            val originalClassName = asClassName(originalName)
            if (parentName == null) {
                return originalClassName
            }

            // Avoid duplication: if originalName already contains parentName
            val originalLower = originalName.lowercase()
            val parentLower = parentName.lowercase()
            if (originalLower.contains(parentLower) && originalLower != parentLower) {
                originalClassName
            } else {
                "${asClassName(parentName)}$originalClassName"
            }
        }
        ClassNamingStrategy.ORIGINAL_THEN_COMPOUND -> {
            val originalClassName = asClassName(originalName)

            // Try to use the original name first
            if (!usedNames.contains(originalClassName)) {
                originalClassName
            } else {
                // If there's a conflict, use compound naming
                if (parentName != null) {
                    "${asClassName(parentName)}${originalClassName}"
                } else {
                    // If no parent name but still conflict, add a suffix
                    "${originalClassName}Item"
                }
            }
        }
    }
}

/**
 * Converts a string to a valid Kotlin property name.
 *
 * Transforms the input string by converting it to camelCase format,
 * handling special characters and ensuring valid Kotlin identifier syntax.
 *
 * @param name The input string to convert
 * @return A valid Kotlin property name in camelCase
 */
fun asClassProperty(name: String): String {
    val words = asSpaceSeparated(name).split(" ")
    if (words.size < 2) {
        return name
    }
    val camelCaseWords = words.mapIndexed { index, word ->
        if (index == 0) word.lowercase().replaceFirstChar { it.lowercase() }
        else word.lowercase().replaceFirstChar { it.titlecase() }
    }
    return camelCaseWords.joinToString("")
}

/**
 * Converts a string to space-separated format.
 *
 * Replaces hyphens with spaces to create a more readable format.
 *
 * @param name The input string to convert
 * @return The string with hyphens replaced by spaces
 */
fun asSpaceSeparated(name: String): String {
    return name.replace("-", " ")
}

/**
 * Converts a string to a valid Kotlin property name.
 *
 * Transforms the input string by converting it to camelCase format,
 * handling special characters and ensuring valid Kotlin identifier syntax.
 * If the result is not a valid Kotlin identifier, it adds an underscore prefix.
 *
 * @param name The input string to convert
 * @return A valid Kotlin property name in camelCase
 */
fun asPropertyName(name: String): String {
    val firstAttempt = asClassName(name).replaceFirstChar { it.lowercase() }
    return if (isValidKotlinIdentifier(firstAttempt)) {
        firstAttempt
    } else {
        "_${firstAttempt}"
    }
}

/**
 * Converts a string to screaming snake case format.
 *
 * Transforms the input string to UPPER_CASE_WITH_UNDERSCORES format,
 * commonly used for enum constants.
 *
 * @param name The input string to convert
 * @return The string in screaming snake case format
 */
fun asScreamingSnakeCase(name: String): String {
    val tokens = splitByUppercase(name.replaceFirstChar { it.lowercase() })
    return if (tokens.size > 1) tokens.joinToString("_").uppercase() else name.uppercase()
}

/**
 * Converts a string to a valid enum constant name.
 *
 * Transforms the input string to a valid Kotlin enum constant name,
 * handling special characters and ensuring valid identifier syntax.
 * If the result is not valid, it prefixes with the enum name.
 *
 * @param enumName The name of the enum
 * @param constantName The input string to convert
 * @return A valid Kotlin enum constant name
 */
fun asEnumConstantName(enumName: String, constantName: String): String {
    val firstAttempt = constantName.uppercase().replace("/", "_OF_").replace(" ", "_").replace("\"", "")
    return if (isValidKotlinIdentifier(firstAttempt)) {
        firstAttempt
    } else {
        "${enumName.uppercase()}_${firstAttempt}"
    }
}

fun isValidKotlinIdentifier(name: String): Boolean {
    val keywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return", "super",
        "this", "throw", "true", "try", "typealias", "val", "var", "when", "while"
    )

    if (name.isEmpty()) {
        return false
    }

    if (!name[0].isLetter() && name[0] != '_') {
        return false
    }

    for (char in name) {
        if (!(char.isLetterOrDigit() || char == '_')) {
            return false
        }
    }

    return !keywords.contains(name)
}

fun allUppercaseToLowercase(it: String): String {
    return if (it.length > 1 && it == it.uppercase()) {
        it.lowercase()
    } else {
        it
    }
}

fun asPackageName(name: String): String {
    return name.replace(" ", "").lowercase()
}

fun splitByUppercase(input: String): List<String> {
    val regex = Regex("(?=[A-Z])")
    return input.split(regex)
}
