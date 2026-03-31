/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.wot.kotlin.generator.plugin.strategy

import com.fasterxml.jackson.annotation.JsonCreator
import com.squareup.kotlinpoet.*
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.asEnumConstantName
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.asValidEnumConstant
import com.fasterxml.jackson.annotation.JsonValue as JacksonJsonValue

/**
 * Shared helper methods for building enum type specs, used by both
 * [InlineEnumGenerationStrategy] and [SeparateClassEnumGenerationStrategy].
 */
object EnumBuilderHelper {

    fun buildToValueFunc(valueType: TypeName): FunSpec {
        return FunSpec.builder("toValue")
            .addAnnotation(JacksonJsonValue::class)
            .returns(valueType)
            .addCode("return _value")
            .build()
    }

    fun buildFromValueFunc(enumClassName: ClassName, valueType: TypeName): FunSpec {
        return FunSpec.builder("fromValue")
            .addAnnotation(JsonCreator::class)
            .returns(enumClassName.copy(nullable = true))
            .addParameter("v", valueType.copy(nullable = true))
            .addCode("return entries.firstOrNull { it._value == v }")
            .build()
    }

    fun buildFromNameFunc(enumClassName: ClassName): FunSpec {
        return FunSpec.builder("fromName")
            .returns(enumClassName.copy(nullable = true))
            .addParameter("name", String::class.asTypeName().copy(nullable = true))
            .addCode("return entries.firstOrNull { it.name == name }")
            .build()
    }

    fun addCustomEnumConstants(
        enumSpecBuilder: TypeSpec.Builder,
        enumName: String,
        enumValues: List<Any>,
        valueType: TypeName,
        useSeparateClassNaming: Boolean
    ) {
        enumValues.forEach {
            val constantName = if (useSeparateClassNaming) {
                asEnumConstantName(enumName, it.toString())
            } else {
                asValidEnumConstant(it.toString())
            }
            enumSpecBuilder.addEnumConstant(
                constantName,
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter(
                        if (valueType == STRING) "%S" else "%L",
                        it
                    )
                    .build()
            )
        }
    }
}
