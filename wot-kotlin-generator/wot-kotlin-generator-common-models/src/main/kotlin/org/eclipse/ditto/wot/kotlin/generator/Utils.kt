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
package org.eclipse.ditto.wot.kotlin.generator

fun String.splitByNonFirstSlash(): Pair<String, String> {
    // Find the index of the first slash after the first character
    val secondSlashIndex = this.indexOf('/', 1)
    if (secondSlashIndex == -1) return Pair(this, "")
    val firstPart = this.substring(0, secondSlashIndex)
    val secondPart = this.substring(secondSlashIndex)
    return Pair(firstPart, secondPart)
}
