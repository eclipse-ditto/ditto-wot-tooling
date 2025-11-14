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

class Property2LeafPath(
    val propertyId: String,
    val subPropertyPath: Path = EmptyPath()
) : Path(
    listOfNotNull(
        PropertySegment(propertyId)
    ) + subPropertyPath.segments, true
) {
    companion object {
        // /<propertyId>/<subPropertyPath>
        fun parse(path: String): Property2LeafPath? {
            try {
                val (propertyPathString, rest) = path.splitByNonFirstSlash()
                val propertyPath = PropertyPath.parse(propertyPathString)
                return if (propertyPath != null && rest.isNotEmpty()) {
                    propertyPath + Path(rest)
                } else if (propertyPath != null && rest.isEmpty()) {
                    Property2LeafPath(propertyPath.propertyId)
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }
}
