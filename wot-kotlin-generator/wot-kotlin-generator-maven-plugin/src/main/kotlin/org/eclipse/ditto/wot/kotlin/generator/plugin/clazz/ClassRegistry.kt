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

import org.eclipse.ditto.json.JsonObject

object ClassRegistry {
    private val registry = mutableMapOf<String, String>()

    private fun getFullyQualifiedClassName(packageName: String, simpleClassName: String) = "$packageName.$simpleClassName"

    /**
     * Checks if there is a naming conflict for a class: a class with the given name
     * already exists in the package but has a different structure.
     *
     * @param packageName The package name
     * @param simpleClassName The simple class name
     * @param schema The schema for the class
     * @return true if there is a conflict, false otherwise
     */
    fun hasConflict(packageName: String, simpleClassName: String, schema: JsonObject): Boolean {
        val fqcn = getFullyQualifiedClassName(packageName, simpleClassName)
        val structureKey = schema.toString()
        val existingStructureKey = registry[fqcn]

        val hasConflict = existingStructureKey != null && existingStructureKey != structureKey

        return hasConflict
    }

    /**
     * Checks if a class with the given name has been registered in the package.
     */
    fun hasClass(packageName: String, simpleClassName: String): Boolean {
        val fqcn = getFullyQualifiedClassName(packageName, simpleClassName)
        return registry.containsKey(fqcn)
    }

    /**
     * Clears all registered classes. Called at the start of each generation run
     * to prevent stale entries from a previous model leaking into the current one.
     */
    fun clear() {
        registry.clear()
    }

    /**
     * Registers a generated class using its fully qualified name.
     *
     * @param packageName The package name
     * @param simpleClassName The simple class name
     * @param schema The schema for the class
     */
    fun registerGeneratedClass(packageName: String, simpleClassName: String, schema: JsonObject) {
        val fqcn = getFullyQualifiedClassName(packageName, simpleClassName)
        val structureKey = schema.toString()
        if (!registry.containsKey(fqcn)) {
            registry[fqcn] = structureKey
        }
    }
}