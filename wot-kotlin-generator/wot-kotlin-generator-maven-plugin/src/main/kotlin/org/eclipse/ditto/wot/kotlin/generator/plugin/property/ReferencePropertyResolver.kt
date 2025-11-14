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
package org.eclipse.ditto.wot.kotlin.generator.plugin.property

import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.loadJson
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.readJsonValue
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.replaceJsonValue
import org.eclipse.ditto.wot.model.ObjectSchema
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull

object ReferencePropertyResolver {

    private val logger = LoggerFactory.getLogger(ReferencePropertyResolver::class.java)

    fun resolveReferenceProperties(objectSchema: ObjectSchema): ObjectSchema {
        var jsonWithRefs = objectSchema.toJson()
        val referenceFields: MutableMap<JsonPointer, String> = mutableMapOf()
        findReferenceFieldsRecursively(objectSchema.toJson(), JsonPointer.empty(), referenceFields)
        referenceFields.keys.forEach {
            val referenceLink = referenceFields[it]
            val hashCount = referenceLink?.count { it == '#' }
            require(hashCount == 1) { "Unsupported reference link: $referenceLink" }
            val linkParts = referenceLink!!.replace("\"", "").split("#", limit = 2)
            val linkUrl = linkParts[0]
            val linkPath = linkParts[1]
            val linkJson = loadJson(linkUrl)
            val replacementJson = readJsonValue(linkJson, JsonPointer.of(linkPath))
            if (replacementJson != null) {
                jsonWithRefs = replaceJsonValue(jsonWithRefs, it, replacementJson)
                logger.debug("Setting jsonWithRefs= $jsonWithRefs")
            }
        }
        return ObjectSchema.fromJson(jsonWithRefs)
    }

    fun findReferenceFieldsRecursively(
        obj: JsonObject,
        objPath: JsonPointer,
        foundReferences: MutableMap<JsonPointer, String>
    ) {
        obj.keys.forEach {
            val singleFieldJson = obj.getField(it).getOrNull()
            if (it.toString() == "tm:ref") {
                // Important: we are putting tm:ref parent path here, not tm:ref itself
                foundReferences[objPath] = singleFieldJson?.value.toString()
            }
            val newObjPath = objPath.addLeaf(it)
            if (singleFieldJson?.value is JsonObject) {
                findReferenceFieldsRecursively((singleFieldJson.value as JsonObject), newObjPath, foundReferences)
            }
        }
    }
}
