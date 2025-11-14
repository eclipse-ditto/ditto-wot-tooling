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
package org.eclipse.ditto.wot.kotlin.generator.common.model.features

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.eclipse.ditto.wot.kotlin.generator.common.deserialize.MapObjectPropertyDeserializer
import org.eclipse.ditto.wot.kotlin.generator.common.serialize.MapObjectPropertySerializer

/**
 * Interface for WoT properties.
 *
 * This interface is implemented by generated property classes to mark them
 * as WoT properties and enable type-safe property handling.
 */
interface Property


/**
 * Interface for WoT object properties.
 *
 * This interface is implemented by generated object property classes.
 */
interface ObjectProperty: Property

@JsonSerialize(using = MapObjectPropertySerializer::class)
@JsonDeserialize(using = MapObjectPropertyDeserializer::class)
abstract class MapObjectProperty<T>: ObjectProperty {
    var map: MutableMap<String, T> = mutableMapOf()

    override fun hashCode(): Int {
        return 31 * super.hashCode() + map.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false

        other as MapObjectProperty<*>

        return map == other.map
    }

    override fun toString() = "${javaClass.simpleName}(map=$map)"
}
