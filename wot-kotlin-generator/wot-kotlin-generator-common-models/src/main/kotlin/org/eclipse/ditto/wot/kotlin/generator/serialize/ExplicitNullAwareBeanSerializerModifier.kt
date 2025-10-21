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
package org.eclipse.ditto.wot.kotlin.generator.serialize

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import kotlin.reflect.full.memberProperties

/**
 * Chooses a JsonSerializer which is aware of explicitly set nulls inside the generated WoT based Kotlin DSL.
 * This will serialize explicitly set Kotlin `null` values to JSON `null`s in the output JSON.
 */
class ExplicitNullAwareBeanSerializerModifier : BeanSerializerModifier() {

    override fun modifySerializer(config: SerializationConfig?,
                                  beanDesc: BeanDescription?,
                                  serializer: JsonSerializer<*>?
    ): JsonSerializer<*> {
        if (beanDesc?.beanClass?.kotlin?.memberProperties?.any { it.isExplicitlySetNullProperty() } == true) {
            return ExplicitNullAwareSerializer(serializer as JsonSerializer<Any>?)
        }
        return super.modifySerializer(config, beanDesc, serializer)
    }
}
