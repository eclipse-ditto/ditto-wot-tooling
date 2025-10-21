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

class Features2LeafPath(
    val featureId: String,
    val category: PropertyCategory,
    val propertyId: String,
    val isDesired: Boolean = false,
    val subPropertyPath: Path = EmptyPath()
) : Path(
    listOfNotNull(
        HardcodedSegment("features"),
        FeatureSegment(featureId),
        if (isDesired) HardcodedSegment("desiredProperties") else HardcodedSegment("properties"),
        PropertyCategorySegment(category),
        PropertySegment(propertyId),
    ) + subPropertyPath.segments, true
) {
    companion object {
        // /features/<featureId>/properties/<category>/<propertyId>/<subPropertyPath>
        // /features/<featureId>/desiredProperties/<category>/<propertyId>/<subPropertyPath>
        fun parse(path: String): Features2LeafPath? {
            try {
                val (features, rest) = path.splitByNonFirstSlash()
                return if (features == "/features" && rest.isNotEmpty()) {
                    FeaturesPath + Feature2LeafPath.parse(rest)!!
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }
}
