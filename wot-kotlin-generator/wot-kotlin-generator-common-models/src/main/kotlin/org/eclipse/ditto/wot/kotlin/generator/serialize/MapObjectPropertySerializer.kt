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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.eclipse.ditto.wot.kotlin.generator.model.features.MapObjectProperty

/**
 * This class exists so that MapObjectProperties are serialized correctly.
 */
class MapObjectPropertySerializer : JsonSerializer<MapObjectProperty<*>>() {
    override fun serialize(
        mapObjectProperty: MapObjectProperty<*>,
        jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider
    ) {
        serializerProvider.defaultSerializeValue(mapObjectProperty.map, jsonGenerator)
    }
}
