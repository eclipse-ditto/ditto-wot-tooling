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
package org.eclipse.ditto.wot.kotlin.generator.model.path

import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.thingsearch.model.SearchModelFactory
import org.eclipse.ditto.thingsearch.model.SearchProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.javaField

/**
 * PathGenerator helps dynamically construct paths for nested attributes in a fluent and type-safe manner.
 *
 * @param R the type of the current path segment.
 * @property path the current path string.
 */
class PathGenerator<R>(val path: String = "") {
    companion object {
        /**
         * Initializes the path generation from a given root class.
         *
         * @param T the type of the root class.
         * @param R the type of the attribute or feature.
         * @param start the starting property.
         * @return a new PathGenerator instance with the initial path.
         */
        fun <T: Any, R> from(start: KProperty1<T, R?>): PathGenerator<R?> {
            val rootPath = (start.javaField?.declaringClass?.kotlin?.companionObjectInstance as? HasPath)?.startPath()
                ?: ""
            val fullPath = if (rootPath.isEmpty()) start.name else "$rootPath/${start.name}"
            return PathGenerator(fullPath)
        }

        /**
         * Initializes the path generation from a given path representing the root path.
         *
         * @param start the starting path.
         * @return a new PathGenerator instance with the initial path.
         */
        fun from(start: KFunction0<String>): PathGenerator<String> {
            return PathGenerator(start())
        }
    }

    /**
     * Appends a nested property to the current path.
     *
     * @param N the type of the nested property.
     * @param nextRef the nested property to add to the path.
     * @return a new PathGenerator instance with the updated path.
     */
    fun <N> add(nextRef: KProperty1<out R?, N?>): PathGenerator<N?> {
        val name = nextRef.name
        val returnClass = nextRef.returnType.classifier as? KClass<*>

        val pathSegment = if (name in listOf("properties", "desiredProperties")) {
            name
        } else {
            (returnClass?.companionObjectInstance as? HasPath)?.startPath() ?: name
        }
        return PathGenerator("$path/$pathSegment")
    }


    /**
     * Returns the current path as a string.
     *
     * @return the current path.
     */
    fun build(): String = path

    /**
     * Returns the current path as a JsonPointer.
     *
     * @return the current path as a JsonPointer.
     */
    fun buildJsonPointer(): JsonPointer = JsonPointer.of(path)

    /**
     * Builds the current path as a Ditto Search Property.
     *
     * @return the Ditto Search Property.
     */
    fun buildSearchProperty(): SearchProperty = SearchModelFactory.property(JsonPointer.of(path))

    override fun toString() = path
}

/**
 * Usage examples:
 *
 * ```
 * // Starting from a root class
 * val path1 = PathGenerator.from(FloorLamp::attributes)
 *     .add(Attributes::location)
 *     .add(Location::room)
 *     .add(Room::type)
 *
 * // Starting from a nested property
 * val path2 = PathGenerator.from(Attributes::location)
 *     .add(Location::room)
 *     .add(Room::type)
 * ```
 *
 * This provides a convenient and type-safe way to generate paths for nested properties.
 */
