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

class FeaturePath(
    val featureId: String
) : Path(
    listOfNotNull(
        FeatureSegment(featureId)
    )
) {
    companion object {
        // /<featureId>
        fun parse(path: String): FeaturePath? {
            try {
                val parts = path.split("/")
                val featureId = parts[1]
                return if (parts.size == 2 && parts[0] == "" && parts[1] == featureId) {
                    FeaturePath(featureId)
                } else null
            } catch (ex: NullPointerException) {
                return null
            }
        }
    }

    operator fun plus(properties2LeafPath: Properties2LeafPath) = Feature2LeafPath(
        featureId,
        properties2LeafPath.category,
        properties2LeafPath.propertyId, properties2LeafPath.isDesired,
        properties2LeafPath.subPropertyPath
    )
}