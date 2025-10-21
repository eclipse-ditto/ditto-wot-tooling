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

interface Segment {
    fun getValue(): String
}

data class HardcodedSegment(val name: String) : Segment {
    init {
        if (name.contains("/")) {
            throw IllegalArgumentException("Hardcoded segment cannot contain '/'")
        }
        if (name.isEmpty()) {
            throw IllegalArgumentException("Hardcoded segment cannot be empty string")
        }
    }

    override fun getValue() = name
}

data class FeatureSegment(val featureId: String) : Segment {
    override fun getValue() = featureId
}

data class PropertySegment(val propertyId: String) : Segment {
    override fun getValue() = propertyId
}

data class PropertyCategorySegment(val propertyCategory: PropertyCategory) : Segment {
    override fun getValue() = propertyCategory
}