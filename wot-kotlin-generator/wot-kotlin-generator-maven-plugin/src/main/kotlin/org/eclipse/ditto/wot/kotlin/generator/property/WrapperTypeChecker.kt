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
package org.eclipse.ditto.wot.kotlin.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import org.eclipse.ditto.wot.kotlin.generator.clazz.EnumGenerator
import org.eclipse.ditto.wot.kotlin.generator.strategy.IEnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.util.asPrimitiveClassName
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.SingleDataSchema

object WrapperTypeChecker {

    private val enumGenerator = EnumGenerator
    private var enumGenerationStrategy: IEnumGenerationStrategy? = null

    fun setEnumGenerationStrategy(strategy: IEnumGenerationStrategy) {
        enumGenerationStrategy = strategy
    }

    fun checkForWrapperType(
        propertySchema: SingleDataSchema,
        propertyName: String?,
        propertyPackage: String,
        schemaType: DataSchemaType,
        role: PropertyRole,
        feature: String? = null,
        dittoCategory: String? = null,
        parentClassName: String? = null
    ): TypeName {
        val enumArray = propertySchema.enum

        return when {
            (schemaType == DataSchemaType.OBJECT || enumArray?.isNotEmpty() == true) -> {
                val enumKey = if (enumGenerationStrategy != null) {
                    enumGenerationStrategy!!.generateEnum(propertySchema, propertyName, enumArray ?: mutableSetOf(), schemaType, propertyPackage, parentClassName)
                } else {
                    enumGenerator.generateEnum(propertySchema, propertyName, enumArray ?: mutableSetOf(), schemaType, parentClassName)
                }

                val isInlineStrategy = enumGenerationStrategy?.javaClass?.simpleName == "InlineEnumGenerationStrategy"

                val enumName = if (isInlineStrategy) {
                    enumKey
                } else {
                    enumKey.substringBeforeLast(".")
                }

                val enumClassName = if (isInlineStrategy) {
                    ClassName("", enumName)
                } else {
                    ClassName(propertyPackage, enumName)
                }

                enumClassName.copy(nullable = true)
            }

            else                                                                     -> {
                asPrimitiveClassName(propertySchema, true)
            }
        }
    }
}
