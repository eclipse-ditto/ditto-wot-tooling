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
package org.eclipse.ditto.wot.kotlin.generator.plugin.clazz

import com.squareup.kotlinpoet.TypeSpec
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.EnumConflictUtils
import java.util.*

/**
 * Registry for managing enum type specifications during code generation.
 *
 * This registry tracks enums by their name, package, and values to detect conflicts
 * where the same enum name is used with different values in the same package.
 * Such conflicts indicate a generator bug where multiple properties with the same
 * name but different enum values are incorrectly sharing the same enum type.
 */
object EnumRegistry {
    private val enums = mutableMapOf<String, TypeSpec>()
    private val nameToKey = mutableMapOf<String, String>()
    /**
     * Tracks enum values by package and name to detect conflicts.
     * Key: Pair(packageName, enumName)
     * Value: Set of enum constant names
     */
    private val enumValuesByPackageAndName = mutableMapOf<Pair<String, String>, Set<String>>()

    /**
     * Registers an enum type specification for inline enums (without UUID suffix).
     *
     * @param typeSpec The type specification for the enum
     * @return The key for the registered enum (same as the enum name)
     */
    fun registerEnumInline(typeSpec: TypeSpec): String {
        val name = typeSpec.name!!
        enums[name] = typeSpec
        nameToKey[name] = name
        return name
    }

    /**
     * Registers an enum type specification with value validation.
     *
     * This method validates that if an enum with the same name already exists in the package,
     * it must have the same enum values. If different values are detected, an exception is thrown
     * to prevent generator bugs where multiple properties with the same name but different enum
     * values incorrectly share the same enum type.
     *
     * @param typeSpec The type specification for the enum
     * @param packageName The package name where the enum is being registered
     * @param enumValues The set of enum constant names for this enum
     * @return The key for the registered enum
     * @throws IllegalArgumentException if an enum with the same name but different values already exists
     */
    fun registerEnum(typeSpec: TypeSpec, packageName: String, enumValues: Set<String>): String {
        val enumName = typeSpec.name!!
        val packageAndName = Pair(packageName, enumName)
        val existingValues = enumValuesByPackageAndName[packageAndName]
        
        if (existingValues != null) {
            if (existingValues != enumValues) {
                throw EnumConflictUtils.createEnumConflictException(enumName, packageName, existingValues, enumValues)
            }
            val existingKey = nameToKey[enumName]
            return existingKey ?: generateUniqueKey(enumName).also {
                enums[it] = typeSpec
                nameToKey[enumName] = it
            }
        }
        
        val newKey = generateUniqueKey(enumName)
        enums[newKey] = typeSpec
        nameToKey[enumName] = newKey
        enumValuesByPackageAndName[packageAndName] = enumValues
        return newKey
    }
    
    /**
     * Registers an enum type specification (backward compatibility - without value checking).
     * 
     * @param typeSpec The type specification for the enum
     * @return The key for the registered enum
     */
    fun registerEnum(typeSpec: TypeSpec): String {
        val key = generateUniqueKey(typeSpec.name!!)
        enums[key] = typeSpec
        nameToKey[typeSpec.name!!] = key
        return key
    }

    /**
     * Gets an enum by its name.
     *
     * @param name The name of the enum
     * @return The enum type specification or null if not found
     */
    fun getEnumByName(name: String): TypeSpec? {
        val key = nameToKey[name]
        return key?.let { enums[it] }
    }

    /**
     * Gets an enum by its key.
     *
     * @param key The key for the enum
     * @return The enum type specification or null if not found
     */
    fun getEnum(key: String): TypeSpec? {
        return enums[key]
    }

    /**
     * Gets all registered enums.
     *
     * @return A map of enum keys to their type specifications
     */
    fun getEnums(): MutableMap<String, TypeSpec> {
        return enums
    }

    private fun generateUniqueKey(baseName: String): String {
        return "$baseName.${UUID.randomUUID().toString().substring(0, 8)}"
    }
}
