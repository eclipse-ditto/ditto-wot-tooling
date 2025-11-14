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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.eclipse.ditto.wot.kotlin.generator.common.model.DittoJsonDsl

@DittoJsonDsl
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class Feature<F : Feature<F, P>, P : Properties<P, F>>(
    @JsonIgnore val featureId: String
) {

    var properties: P? = null
        set(value) {
            if (value == null) {
                _properties_explicitly_set_null = true
            }
            field = value
        }

    var desiredProperties: P? = null
        set(value) {
            if (value == null) {
                _desiredProperties_explicitly_set_null = true
            }
            field = value
        }

    protected var _properties_explicitly_set_null: Boolean = false
    protected var _desiredProperties_explicitly_set_null: Boolean = false

    override fun hashCode(): Int {
        var result = featureId.hashCode()
        result = 31 * result + (properties?.hashCode() ?: 0)
        result = 31 * result + (desiredProperties?.hashCode() ?: 0)
        result = 31 * result + (_properties_explicitly_set_null.hashCode())
        result = 31 * result + (_desiredProperties_explicitly_set_null.hashCode())
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Feature<*, *>

        if (featureId != other.featureId) return false
        if (properties != other.properties) return false
        if (desiredProperties != other.desiredProperties) return false
        if (_properties_explicitly_set_null != other._properties_explicitly_set_null) return false
        if (_desiredProperties_explicitly_set_null != other._desiredProperties_explicitly_set_null) return false

        return true
    }

    override fun toString() = "${javaClass.simpleName}(properties=$properties, desiredProperties=$desiredProperties, " +
        "_properties_explicitly_set_null=$_properties_explicitly_set_null, " +
        "_desiredProperties_explicitly_set_null=$_desiredProperties_explicitly_set_null)"

    fun properties(block: P.() -> Unit): P {
        val props = createProperties()
        props.block()
        properties = props
        return props
    }

    abstract fun createProperties(): P

    fun desiredProperties(block: P.() -> Unit): P {
        val desiredProps = createDesiredProperties()
        desiredProps.block()
        desiredProperties = desiredProps
        return desiredProps
    }

    abstract fun createDesiredProperties(): P

}
