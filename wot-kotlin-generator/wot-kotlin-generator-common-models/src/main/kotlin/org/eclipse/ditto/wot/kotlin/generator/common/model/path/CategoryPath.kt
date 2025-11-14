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

class CategoryPath(
    val category: PropertyCategory
) : Path(
    listOfNotNull(
        PropertyCategorySegment(category)
    )
) {
    companion object {
        // /configuration
        // /status
        fun parse(path: String): CategoryPath? {
            try {
                val parts = path.split("/")
                val category = parts.getOrNull(1)
                return if (parts.size == 2 && parts[0] == "" && !category.isNullOrBlank()) {
                    CategoryPath(category)
                } else null
            } catch (ex: Exception) {
                return null
            }
        }
    }

    operator fun plus(property2LeafPath: Property2LeafPath) = Category2LeafPath(
        category,
        property2LeafPath.propertyId,
        property2LeafPath.subPropertyPath
    )
}
