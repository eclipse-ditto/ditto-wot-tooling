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
package org.eclipse.ditto.wot.kotlin.generator.clazz

import com.squareup.kotlinpoet.TypeSpec
import java.util.*

object EnumRegistry {
    private val enums = mutableMapOf<String, TypeSpec>()
    private val nameToKey = mutableMapOf<String, String>()

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
     * Registers an enum type specification.
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
