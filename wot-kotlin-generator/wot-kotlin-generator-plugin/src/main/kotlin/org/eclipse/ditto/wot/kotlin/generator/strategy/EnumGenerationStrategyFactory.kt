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
package org.eclipse.ditto.wot.kotlin.generator.strategy

import org.eclipse.ditto.wot.kotlin.generator.config.EnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.config.GeneratorConfiguration

/**
 * Factory for creating enum generation strategies.
 *
 * This object provides a factory method to create the appropriate enum generation
 * strategy based on the configuration.
 */
object EnumGenerationStrategyFactory {

    /**
     * Creates an enum generation strategy based on the configuration.
     *
     * @param config The generator configuration containing the enum strategy setting
     * @return The appropriate enum generation strategy
     */
    fun createStrategy(config: GeneratorConfiguration): IEnumGenerationStrategy {
        return when (config.enumGenerationStrategy) {
            EnumGenerationStrategy.INLINE -> InlineEnumGenerationStrategy()
            EnumGenerationStrategy.SEPARATE_CLASS -> SeparateClassEnumGenerationStrategy()
        }
    }
}
