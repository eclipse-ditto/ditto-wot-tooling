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

class PropertiesPath(val isDesired: Boolean = false) : Path(
    listOfNotNull(
        if (isDesired) HardcodedSegment("desiredProperties") else HardcodedSegment("properties"),
    )
) {
    companion object {
        // /properties
        // /desiredProperties
        fun parse(path: String): PropertiesPath? {
            try {
                val parts = path.split("/")
                return if (parts.size == 2 && parts[0] == "" && (parts[1] == "properties" || parts[1] == "desiredProperties")) {
                    PropertiesPath(parts[1] == "desiredProperties")
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }

    operator fun plus(category2LeafPath: Category2LeafPath) = Properties2LeafPath(
        category2LeafPath.category,
        category2LeafPath.propertyId, isDesired,
        category2LeafPath.subPropertyPath
    )
}