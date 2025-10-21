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
package org.eclipse.ditto.wot.kotlin.generator.property

/**
 * Defines the role of a property in the WoT Thing Model hierarchy.
 *
 * Property roles determine how properties are wrapped and processed during code generation.
 * Different roles have different wrapper types and generation strategies:
 *
 * - [FEATURE]: Top-level feature properties (e.g., device capabilities)
 * - [FEATURE_PROPERTY]: Properties within a feature (e.g., temperature sensor properties)
 * - [ATTRIBUTE]: Thing-level attributes (e.g., device metadata)
 * - [OTHER]: Generic properties that don't fit other categories
 *
 * The role hierarchy is used to determine appropriate wrapper types:
 * - Primitive feature properties → SimpleProperty<T>
 * - Object feature properties → ObjectProperty<T>
 * - Timestream values → TimestreamProperty<T>
 * - Enum values → EnumProperty<T>
  *
 */
enum class PropertyRole {
    /** Top-level feature properties representing device capabilities */
    FEATURE,
    /** Properties within a feature (nested properties) */
    FEATURE_PROPERTY,
    /** Thing-level attributes representing device metadata */
    ATTRIBUTE,
    /** Generic properties that don't fit other categories */
    OTHER;

    companion object {
        /**
         * Returns the next level role in the property hierarchy.
         *
         * This method is used to determine the appropriate role for nested properties.
         * The hierarchy follows: FEATURE → FEATURE_PROPERTY → OTHER
         *
         * @param role The current property role
         * @return The next level role in the hierarchy
         */
        fun nextLevel(role: PropertyRole): PropertyRole {
            return when (role) {
                FEATURE -> FEATURE_PROPERTY
                FEATURE_PROPERTY -> OTHER
                ATTRIBUTE -> OTHER
                OTHER -> OTHER
            }
        }
    }
}