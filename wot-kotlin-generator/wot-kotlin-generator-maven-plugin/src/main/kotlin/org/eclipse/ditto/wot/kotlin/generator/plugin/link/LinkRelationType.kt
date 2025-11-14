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
package org.eclipse.ditto.wot.kotlin.generator.plugin.link

import org.eclipse.ditto.wot.model.BaseLink

enum class LinkRelationType(var wotName: String?) {
    SUBMODEL("tm:submodel"),
    EXTENDS("tm:extends"),
    UNKNOWN(null);

    companion object {
        private fun fromWotName(wotName: String?): LinkRelationType? {
            return entries.find { it.wotName == wotName }
        }

        fun isExtends(baseLink: BaseLink<*>): Boolean {
            return fromWotName(baseLink.rel.get()) == EXTENDS
        }

        fun isSubmodel(baseLink: BaseLink<*>): Boolean {
            return fromWotName(baseLink.rel.get()) == SUBMODEL
        }
    }
}