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

class Feature2LeafPath(
    val featureId: String,
    val category: PropertyCategory,
    val propertyId: String,
    val isDesired: Boolean = false,
    val subPropertyPath: Path = EmptyPath()
) : Path(
    listOfNotNull(
        FeatureSegment(featureId),
        if (isDesired) HardcodedSegment("desiredProperties") else HardcodedSegment("properties"),
        PropertyCategorySegment(category),
        PropertySegment(propertyId)
    ) + subPropertyPath.segments, true
) {
    companion object {
        // /<featureId>/properties/<category>/<propertyId>/<subPropertyPath>
        // /<featureId>/desiredProperties/<category>/<propertyId>/<subPropertyPath>
        fun parse(path: String): Feature2LeafPath? {
            try {
                val (featurePathsString, rest) = path.splitByNonFirstSlash()
                val featurePath = FeaturePath.parse(featurePathsString)
                return if (featurePath != null && rest.isNotEmpty()) {
                    featurePath + Properties2LeafPath.parse(rest)!!
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }
}