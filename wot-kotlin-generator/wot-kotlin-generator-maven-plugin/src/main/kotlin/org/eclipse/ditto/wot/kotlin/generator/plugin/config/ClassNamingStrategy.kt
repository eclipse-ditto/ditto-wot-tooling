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
package org.eclipse.ditto.wot.kotlin.generator.plugin.config

/**
 * Strategy for naming generated classes.
 *
 * This enum controls how class names are generated from WoT schema titles
 * and property names.
 *
 */
enum class ClassNamingStrategy {
    /**
     * Always use compound naming strategy.
     *
     * This strategy combines parent and child names to create unique class names.
     * For example:
     * - Parent: "smartheating", Child: "thermostat" → "SmartheatingThermostat"
     * - Parent: "room", Child: "attributes" → "RoomAttributes"
     *
     * This ensures unique class names but may result in longer names.
     */
    COMPOUND_ALL,

    /**
     * Use original names when possible, then fall back to compound naming.
     *
     * This strategy tries to use the original schema title as the class name,
     * but falls back to compound naming if there would be conflicts.
     * For example:
     * - Schema title: "Thermostat" → "Thermostat" (if no conflict)
     * - Schema title: "Thermostat" → "SmartheatingThermostat" (if conflict exists)
     *
     * This provides shorter, more readable names when possible.
     */
    ORIGINAL_THEN_COMPOUND
}

