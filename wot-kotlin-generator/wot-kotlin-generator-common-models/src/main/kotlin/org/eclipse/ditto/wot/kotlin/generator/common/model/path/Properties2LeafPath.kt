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
package org.eclipse.ditto.wot.kotlin.generator.common.model.path

import org.eclipse.ditto.wot.kotlin.generator.common.splitByNonFirstSlash

class Properties2LeafPath(
    val category: PropertyCategory,
    val propertyId: String,
    val isDesired: Boolean = false,
    val subPropertyPath: Path = EmptyPath()
) : Path(
    listOfNotNull(
        if (isDesired) HardcodedSegment("desiredProperties") else HardcodedSegment("properties"),
        PropertyCategorySegment(category),
        PropertySegment(propertyId)
    ) + subPropertyPath.segments, true
) {
    companion object {
        // /properties/<category>/<propertyId>/<subPropertyPath>
        // /desiredProperties/<category>/<propertyId>/<subPropertyPath>
        fun parse(path: String): Properties2LeafPath? {
            try {
                val (propertiesPathString, rest) = path.splitByNonFirstSlash()
                val propertiesPath = PropertiesPath.parse(propertiesPathString)
                return if (propertiesPath != null && rest.isNotEmpty()) {
                    propertiesPath + Category2LeafPath.parse(rest)!!
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }
}