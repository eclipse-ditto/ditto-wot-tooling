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
package org.eclipse.ditto.wot.kotlin.generator.plugin.util

object Const {
    const val ATTRIBUTES_CLASS_NAME = "Attributes"
    const val FEATURES_CLASS_NAME = "Features"
    const val COMMON_PACKAGE = "org.eclipse.ditto.wot.kotlin.generator.model"
    const val COMMON_PACKAGE_PATH = "$COMMON_PACKAGE.path"
    const val COMMON_PACKAGE_FEATURES = "$COMMON_PACKAGE.features"
    const val COMMON_PACKAGE_ATTRIBUTES = "$COMMON_PACKAGE.attributes"
    const val EXISTING_ATTRIBUTES_PACKAGE = COMMON_PACKAGE_ATTRIBUTES
    const val EXISTING_FEATURES_PACKAGE = COMMON_PACKAGE_FEATURES
}