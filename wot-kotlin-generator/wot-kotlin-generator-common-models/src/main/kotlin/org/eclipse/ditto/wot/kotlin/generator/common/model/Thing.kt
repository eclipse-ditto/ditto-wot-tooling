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
package org.eclipse.ditto.wot.kotlin.generator.common.model

import com.fasterxml.jackson.annotation.JsonInclude
import org.eclipse.ditto.policies.model.PolicyId
import org.eclipse.ditto.things.model.ThingId
import org.eclipse.ditto.wot.kotlin.generator.common.model.attributes.Attributes
import org.eclipse.ditto.wot.kotlin.generator.common.model.features.Features

@DittoJsonDsl
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class Thing<A : Attributes, F : Features> {
    var thingId: ThingId? = null
    var policyId: PolicyId? = null
    var definition: String? = null
        set(value) {
            if (value == null) {
                _definition_explicitly_set_null = true
            }
            field = value
        }

    var attributes: A? = null
        set(value) {
            if (value == null) {
                _attributes_explicitly_set_null = true
            }
            field = value
        }

    var features: F? = null
        set(value) {
            if (value == null) {
                _features_explicitly_set_null = true
            }
            field = value
        }

    protected var _definition_explicitly_set_null: Boolean = false
    protected var _attributes_explicitly_set_null: Boolean = false
    protected var _features_explicitly_set_null: Boolean = false

    override fun hashCode(): Int {
        var result = thingId?.hashCode() ?: 0
        result = 31 * result + (policyId?.hashCode() ?: 0)
        result = 31 * result + (definition?.hashCode() ?: 0)
        result = 31 * result + (attributes?.hashCode() ?: 0)
        result = 31 * result + (features?.hashCode() ?: 0)
        result = 31 * result + (_definition_explicitly_set_null.hashCode())
        result = 31 * result + (_attributes_explicitly_set_null.hashCode())
        result = 31 * result + (_features_explicitly_set_null.hashCode())
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as Thing<*, *>

        if (thingId != other.thingId) return false
        if (policyId != other.policyId) return false
        if (definition != other.definition) return false
        if (attributes != other.attributes) return false
        if (features != other.features) return false
        if (_definition_explicitly_set_null != other._definition_explicitly_set_null) return false
        if (_attributes_explicitly_set_null != other._attributes_explicitly_set_null) return false
        if (_features_explicitly_set_null != other._features_explicitly_set_null) return false

        return true
    }

    override fun toString() = "${javaClass.simpleName}(thingId=$thingId, policyId=$policyId, definition=$definition, " +
        "attributes=$attributes, " +
        "features=$features, " +
        "_definition_explicitly_set_null=$_definition_explicitly_set_null, " +
        "_attributes_explicitly_set_null=$_attributes_explicitly_set_null, " +
        "_features_explicitly_set_null=$_features_explicitly_set_null)"
}
