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

/**
 * Path starts with "/" and ends without it.
 * It has at least one segment.
 * If it has more segments, they are separated by "/"
 * If path ens with leaf, @code{toLeaf} is true
 */
open class Path(val segments: List<Segment>, val endsWithLeaf: Boolean = false) {

    constructor(singleSegment: Segment, endsWithLeaf: Boolean = false) : this(listOf(singleSegment), endsWithLeaf)
    constructor(path: String, endsWithLeaf: Boolean = false) : this(path.split("/").filter { it.isNotEmpty() }
        .map { HardcodedSegment(it.replace("/", "")) }, endsWithLeaf)

    override fun toString(): String {
        return segments.joinToString(separator = "/", prefix = "/") { it.getValue() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is Path) return false
        return segments == other.segments && endsWithLeaf == other.endsWithLeaf
    }

    override fun hashCode(): Int {
        return 31 * segments.hashCode() + endsWithLeaf.hashCode()
    }
}

class EmptyPath : Path(emptyList(), false)

