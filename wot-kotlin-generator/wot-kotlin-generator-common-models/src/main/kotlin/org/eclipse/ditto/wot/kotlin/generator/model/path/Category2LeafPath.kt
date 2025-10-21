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

import org.eclipse.ditto.wot.kotlin.generator.splitByNonFirstSlash

class Category2LeafPath(
    val category: PropertyCategory,
    val propertyId: String,
    val subPropertyPath: Path = EmptyPath()
) : Path(
    listOfNotNull(
        PropertyCategorySegment(category),
        PropertySegment(propertyId)
    ) + subPropertyPath.segments, true
) {
    companion object {
        // /<category>/<propertyId>/<subPropertyPath>
        fun parse(path: String): Category2LeafPath? {
            try {
                val (categoryPathString, rest) = path.splitByNonFirstSlash()
                val categoryPath = CategoryPath.parse(categoryPathString)
                return if (categoryPath != null && rest.isNotEmpty()) {
                    categoryPath + Property2LeafPath.parse(rest)!!
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }
}
