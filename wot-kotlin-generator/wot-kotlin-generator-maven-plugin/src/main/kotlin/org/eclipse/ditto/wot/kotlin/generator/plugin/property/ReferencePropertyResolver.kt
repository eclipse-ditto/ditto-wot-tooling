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

    /**
     * Collects all `tm:ref` entries from [jsonObject], returning a map of parent JSON path → ref URL.
     *
     * The returned key is the path of the object containing the `tm:ref`, not the `tm:ref` field itself.
     */
    fun collectReferenceFields(jsonObject: JsonObject): Map<JsonPointer, String> {
        val foundReferences = mutableMapOf<JsonPointer, String>()
        findReferenceFieldsRecursively(jsonObject, JsonPointer.empty(), foundReferences)
        return foundReferences
    }

    /**
     * Resolves all tm:ref references in the schema, inlining the referenced content.
     * Also returns a map of JSON paths → tm:ref URLs for deduplication tracking.
     */
    fun resolveWithRefTracking(objectSchema: ObjectSchema): Pair<ObjectSchema, Map<JsonPointer, String>> {
        var jsonWithRefs = objectSchema.toJson()
        val referenceFields = collectReferenceFields(objectSchema.toJson())

        val refMap = mutableMapOf<JsonPointer, String>()
        referenceFields.forEach { (referencePath, referenceLink) ->
            val hashCount = referenceLink?.count { ch -> ch == '#' }
            require(hashCount == 1) { "Unsupported reference link: $referenceLink" }
            val cleanUrl = referenceLink!!.replace("\"", "")
            refMap[referencePath] = cleanUrl

            val linkParts = cleanUrl.split("#", limit = 2)
            val linkUrl = linkParts[0]
            val linkPath = linkParts[1]
            val linkJson = loadJson(linkUrl)
            val replacementJson = readJsonValue(linkJson, JsonPointer.of(linkPath))
            if (replacementJson != null) {
                jsonWithRefs = replaceJsonValue(jsonWithRefs, referencePath, replacementJson)
            } else {
                logger.warn("Could not resolve tm:ref at path '{}' in '{}' — referenced content not found", linkPath, linkUrl)
            }
        }
        return ObjectSchema.fromJson(jsonWithRefs) to refMap
    }

    /**
     * Resolves all tm:ref references, discarding the ref tracking info.
     * Backward-compatible with existing callers.
     */
    fun resolveReferenceProperties(objectSchema: ObjectSchema): ObjectSchema {
        return resolveWithRefTracking(objectSchema).first
    }

    /**
     * Recursively walks a [JsonObject] collecting all `tm:ref` entries.
     * Each found `tm:ref` is stored in [foundReferences] with the **parent** path as key
     * (not the `tm:ref` field itself) and the reference URL as value.
     *
     * @param obj the JSON object to scan
     * @param objPath the current path in the JSON tree (pass [JsonPointer.empty] for root)
     * @param foundReferences output map that will be mutated with discovered references
     */
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
