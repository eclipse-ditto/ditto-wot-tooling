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

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.util.*

/**
 * Extension function to generate a unique key for a [TypeSpec].
 *
 * This function creates a deterministic key based on the content of the TypeSpec.
 * The key is used for conflict detection and class registry management.
 *
 * For enums, the key is based on enum constant names.
 * For objects, the key is based on object names.
 * For classes with properties, the key is based on property names and types.
 * For empty classes, a random UUID is generated.
 *
 * @return A string key representing the TypeSpec content
 */
fun TypeSpec.getKey(): String {
    return if (this.isEnum) {
        this.enumConstants.map { enum -> enum.key }.joinToString(";")
    } else if (this.typeSpecs.none { it.kind == TypeSpec.Kind.OBJECT && it.isCompanion.not() }.not()) {
        this.typeSpecs.filter { it.kind == TypeSpec.Kind.OBJECT && it.isCompanion.not() }
            .joinToString(";") { it.name.toString() }
    } else if (this.propertySpecs.isEmpty().not()) {
        if (this.typeSpecs.isEmpty()) {
            collectPropertySpecsString(this.propertySpecs)
        } else {
            collectTypeSpecString(this.typeSpecs) + "-" + collectPropertySpecsString(this.propertySpecs)
        }
    } else {
        UUID.randomUUID().toString()
    }
}

/**
 * Collects property specifications into a string representation.
 *
 * @param propertySpecs List of property specifications
 * @return A string representation of the properties
 */
private fun collectPropertySpecsString(propertySpecs: List<PropertySpec>) =
    propertySpecs.sortedBy { it.name }.joinToString(";") { "${it.name}:${it.type}" }

/**
 * Collects type specifications into a string representation.
 *
 * @param typeSpecs List of type specifications
 * @return A string representation of the types
 */
private fun collectTypeSpecString(typeSpecs: List<TypeSpec>): String = typeSpecs.sortedBy { it.name }.joinToString(";") {
    if (it.typeSpecs.isNotEmpty()) {
        collectTypeSpecString(it.typeSpecs)
    } else if (it.propertySpecs.isNotEmpty()) {
        collectPropertySpecsString(it.propertySpecs)
    } else {
        "${it.name}"
    }
}

/**
 * Represents a generated class with its metadata and properties.
 *
 * This class holds information about a generated Kotlin class, including:
 * - The actual [TypeSpec] representing the class structure
 * - Class name and package information
 * - Whether the class is sealed
 * - Methods for generating unique keys and accessing class metadata
 *
 * This is used by the [ClassRegistry] to track generated classes and detect conflicts.
 *
 * @property typeSpec The KotlinPoet TypeSpec representing the class
 * @property name The name of the class
 * @property packageName The package where the class is located
 * @property sealed Whether the class is sealed (default: false)
  *
 */
class ClassDescription(
    val typeSpec: TypeSpec,
    val name: String,
    val packageName: String,
    val sealed: Boolean = false
) {
    /**
     * Returns the fully qualified name of the class.
     *
     * @return The full class name including package
     */
    fun getFullName() = "$packageName.$name"

    /**
     * Returns a unique key for this class based on its content.
     *
     * @return A string key representing the class content
     */
    fun getKey() = typeSpec.getKey()

    /**
     * Returns the number of properties in this class.
     *
     * @return The count of properties
     */
    fun getPropertyCount() = typeSpec.propertySpecs.size

    /**
     * Returns whether this class is sealed.
     *
     * @return True if the class is sealed, false otherwise
     */
    fun sealed() = this.sealed
}
