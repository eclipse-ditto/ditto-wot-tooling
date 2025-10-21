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
package org.eclipse.ditto.wot.kotlin.generator.model.enum

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

interface JsonEnum {

    @JsonValue
    fun getProperties(): Map<String, Any?>

    companion object {
        @JsonCreator
        inline fun <reified T: JsonEnum> fromJson(properties: Map<String, Any?>): T? {
            val enumConstants = T::class.java.enumConstants ?: return null
            return enumConstants.firstOrNull { enumConstant ->
                properties.all { (key, value) ->
                    val enumValue = enumConstant::class.java.getDeclaredField(key).apply { isAccessible = true }
                        .get(enumConstant)
                    enumValue == value
                }
            }
        }
    }
}
