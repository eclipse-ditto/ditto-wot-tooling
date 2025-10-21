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

import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.json.JsonValue
import java.net.URI
import java.net.URL
import kotlin.jvm.optionals.getOrNull

fun loadJson(jsonUrl: String): JsonObject {
    return JsonObject.of(URI(jsonUrl).toURL().readText(Charsets.UTF_8))
}

fun readJsonValue(jsonObject: JsonObject, pointer: JsonPointer): JsonValue? {
    return jsonObject.getValue(pointer).getOrNull()
}

fun replaceJsonValue(jsonObject: JsonObject, pointer: JsonPointer, newValue: JsonValue): JsonObject {
    return jsonObject.remove(pointer).setValue(pointer, newValue)
}
