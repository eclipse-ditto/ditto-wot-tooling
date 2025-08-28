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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator

/**
 * Enumeration representing different types of WoT link relations.
 * This enum maps WoT link relation names to their corresponding types.
 */
enum class LinkRelationType(var wotName: String?) {
    /** Submodel link relation */
    SUBMODEL("tm:submodel"),
    /** Extends link relation */
    EXTENDS("tm:extends"),
    /** Unknown link relation type */
    UNKNOWN(null);

    companion object {
        /**
         * Creates a LinkRelationType instance from a WoT link relation name.
         * 
         * @param wotName The WoT link relation name
         * @return The corresponding LinkRelationType enum value, or null if not found
         */
        fun fromWotName(wotName: String?): LinkRelationType? {
            return values().find { it.wotName == wotName }
        }
    }
}