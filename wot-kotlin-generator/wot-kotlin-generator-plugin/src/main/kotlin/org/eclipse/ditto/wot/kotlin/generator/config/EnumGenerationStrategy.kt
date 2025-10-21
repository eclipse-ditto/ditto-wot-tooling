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
package org.eclipse.ditto.wot.kotlin.generator.config

/**
 * Strategy for generating enums from WoT Thing Model enums.
 *
 */
enum class EnumGenerationStrategy {
    /**
     * Generate enums as inline classes (current behavior).
     * Creates compact, efficient representations where enums are nested
     * within the classes that use them.
     */
    INLINE,

    /**
     * Generate enums as separate enum classes.
     * Provides more flexibility and standard enum behavior with
     * better IDE support and standalone usage.
     */
    SEPARATE_CLASS
}
