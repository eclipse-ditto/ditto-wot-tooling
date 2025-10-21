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

class PropertyPath(
    val propertyId: String
) : Path(
    listOfNotNull(
        PropertySegment(propertyId)
    )
) {
    companion object {
        // /configuration
        // /status
        fun parse(path: String): PropertyPath? {
            try {
                val parts = path.split("/")
                val propertyId = parts[1]
                return if (parts.size == 2 && parts[0] == "" && propertyId != null) {
                    PropertyPath(propertyId)
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }

    operator fun plus(subPropertyPath: Path) = Property2LeafPath(
        propertyId,
        subPropertyPath
    )
}