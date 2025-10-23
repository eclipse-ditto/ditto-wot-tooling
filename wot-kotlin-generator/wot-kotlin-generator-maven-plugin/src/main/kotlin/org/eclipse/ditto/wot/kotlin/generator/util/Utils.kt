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
package org.eclipse.ditto.wot.kotlin.generator.util

import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.wot.model.BaseLink
import kotlin.jvm.optionals.getOrNull

fun getLinkInstanceName(baseLink: BaseLink<*>) =
    baseLink.getValue(JsonPointer.of("instanceName")).getOrNull().toString().replace("\"", "")
