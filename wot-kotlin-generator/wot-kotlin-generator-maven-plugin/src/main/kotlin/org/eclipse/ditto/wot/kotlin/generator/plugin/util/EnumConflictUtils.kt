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

/**
 * Utility functions for handling enum conflicts during code generation.
 */
object EnumConflictUtils {
    
    /**
     * Creates an IllegalArgumentException for enum conflicts.
     *
     * This method generates a descriptive error message when an enum with the same name
     * but different values is detected in the same package. This indicates a generator bug
     * where multiple properties with the same name but different enum values are incorrectly
     * sharing the same enum type.
     *
     * @param enumName The name of the conflicting enum
     * @param packageName The package where the conflict occurs
     * @param existingValues The existing enum values
     * @param newValues The new enum values that conflict
     * @return An IllegalArgumentException with a descriptive error message
     */
    fun createEnumConflictException(
        enumName: String,
        packageName: String,
        existingValues: Set<String>,
        newValues: Set<String>
    ): IllegalArgumentException {
        val existingValuesStr = existingValues.sorted().joinToString(", ")
        val newValuesStr = newValues.sorted().joinToString(", ")
        
        return IllegalArgumentException(
            "Enum conflict detected: Enum '$enumName' already exists in package '$packageName' " +
            "with different values.\n" +
            "  Existing values: [$existingValuesStr]\n" +
            "  New values: [$newValuesStr]\n" +
            "This indicates a generator bug where multiple properties with the same name but different " +
            "enum values are incorrectly sharing the same enum type. Consider using a more specific " +
            "enum name (e.g., '${enumName}ItemType' or '${enumName}Enum') to avoid conflicts."
        )
    }
}

