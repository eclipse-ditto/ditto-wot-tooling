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
package org.eclipse.ditto.wot.kotlin.generator.common.model.path

import org.eclipse.ditto.thingsearch.model.SearchFilter
import org.eclipse.ditto.thingsearch.model.SearchModelFactory

/**
 * DittoRql encapsulates RQL query generation using PathGenerator and SearchModelFactory.
 */
class DittoRql {
    companion object {
        /**
         * Provides a builder to create paths and generate search properties.
         *
         * @return a new PathBuilder instance.
         */
        fun pathBuilder(): PathGenerator.Companion {
            return PathGenerator
        }

        /**
         * Combines multiple conditions with logical AND.
         *
         * @param filter1 the first filter condition.
         * @param filter2 the second filter condition.
         * @param furtherFilters additional filter conditions.
         * @return the combined SearchFilter.
         */
        fun and(filter1: SearchFilter, filter2: SearchFilter, vararg furtherFilters: SearchFilter): SearchFilter {
            return SearchModelFactory.and(filter1, filter2, *furtherFilters)
        }

        /**
         * Combines multiple conditions with logical OR.
         *
         * @param filter1 the first filter condition.
         * @param filter2 the second filter condition.
         * @param furtherFilters additional filter conditions.
         * @return the combined SearchFilter.
         */
        fun or(filter1: SearchFilter, filter2: SearchFilter, vararg furtherFilters: SearchFilter): SearchFilter {
            return SearchModelFactory.or(filter1, filter2, *furtherFilters)
        }

        /**
         * Negates a condition with logical NOT.
         *
         * @param filter the filter condition to negate.
         * @return the negated SearchFilter.
         */
        fun not(filter: SearchFilter): SearchFilter {
            return SearchModelFactory.not(filter)
        }
    }
}
