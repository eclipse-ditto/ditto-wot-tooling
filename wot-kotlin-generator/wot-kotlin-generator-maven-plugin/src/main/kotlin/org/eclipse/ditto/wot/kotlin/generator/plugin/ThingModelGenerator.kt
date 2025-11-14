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
package org.eclipse.ditto.wot.kotlin.generator.plugin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.ClassGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.kotlin.generator.plugin.loader.DittoBasedWotLoader
import org.eclipse.ditto.wot.kotlin.generator.plugin.strategy.EnumGenerationStrategyFactory
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.*
import org.eclipse.ditto.wot.model.BaseLink
import org.eclipse.ditto.wot.model.ThingModel
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrNull

/**
 * Main generator for WoT Thing Models.
 *
 * This object orchestrates the generation of Kotlin code from WoT Thing Models.
 * It coordinates between the class generator, DSL generator, and various strategies
 * to produce complete, compilable Kotlin code.
 *
 * The generator is responsible for:
 * - Loading WoT Thing Models from URLs
 * - Coordinating class generation for attributes, features, and actions
 * - Managing enum generation strategies
 * - Producing the final Thing class with proper inheritance
 *
 * The generator follows the WoT specification and creates type-safe Kotlin classes
 * that can be used for IoT device modeling and communication.
 */
object ThingModelGenerator {
    private val classGenerator = ClassGenerator
    private val logger = LoggerFactory.getLogger(ThingModelGenerator::class.java)

    /**
     * Loads a WoT Thing Model from a given URL.
     *
     * This method fetches the Thing Model JSON from the specified URL and parses it
     * into a [ThingModel] object that can be processed by the generator.
     *
     * @param url The URL pointing to the WoT Thing Model JSON file
     * @return A [ThingModel] object representing the loaded model
     * @throws Exception if the model cannot be loaded or parsed
     */
    suspend fun loadModel(url: String): ThingModel {
        logger.info("Loading model to generate from: $url")
        return DittoBasedWotLoader.load(URI(url).toURL())
    }

    /**
     * Generates Kotlin code from a WoT Thing Model using default configuration.
     *
     * This is a convenience method that creates a default configuration for backward compatibility.
     * For more control over the generation process, use the overloaded version with [GeneratorConfiguration].
     *
     * @param thingModel The WoT Thing Model to generate code from
     * @param rootPackageName The base package name for generated classes
     * @param outputDirectory The directory where generated files will be written
     */
    suspend fun generate(thingModel: ThingModel, rootPackageName: String, outputDirectory: String) {
        // Create default configuration for backward compatibility
        val defaultConfig = GeneratorConfiguration(
            thingModelUrl = "",
            outputPackage = rootPackageName,
            outputDirectory = java.io.File(outputDirectory)
        )
        generate(thingModel, defaultConfig)
    }

    /**
     * Generates Kotlin code from a WoT Thing Model using the specified configuration.
     *
     * This is the main generation method that:
     * 1. Extracts the model name from the Thing Model title
     * 2. Configures the enum generation strategy
     * 3. Generates attributes, actions, and features classes
     * 4. Creates the main Thing class with proper inheritance
     * 5. Generates DSL functions for fluent API usage
     *
     * The generated code includes:
     * - A main Thing class extending the base [Thing] class
     * - Attributes class containing device properties
     * - Features class containing device capabilities
     * - Actions class containing device operations
     * - DSL functions for building instances fluently
     *
     * @param thingModel The WoT Thing Model to generate code from
     * @param config The configuration controlling the generation process
     */
    suspend fun generate(thingModel: ThingModel, config: GeneratorConfiguration) {
        val modelName = asClassNameWithStrategy(thingModel.title.get().toString(), null, config.classNamingStrategy, emptySet())
        val links = thingModel.links.getOrNull() ?: emptyList<BaseLink<*>>()

        logger.info("Using enum generation strategy: ${config.enumGenerationStrategy}")
        logger.info("Generate DSL: ${config.generateDsl}")
        logger.info("Generate Enums: ${config.generateEnums}")
        logger.info("Generate Interfaces: ${config.generateInterfaces}")

        classGenerator.setOutputDir(config.outputDirectory.path)
        classGenerator.setEnumGenerationStrategy(config)

        // Set enum generation strategy in WrapperTypeChecker
        val enumStrategy = EnumGenerationStrategyFactory.createStrategy(config)
        org.eclipse.ditto.wot.kotlin.generator.plugin.property.WrapperTypeChecker.setEnumGenerationStrategy(enumStrategy)

        classGenerator.generateAttributesClass(config.outputPackage, thingModel)
        classGenerator.generateThingActions(config.outputPackage, thingModel)
        classGenerator.generateFeaturesClass(config.outputPackage, links)

        val thingModelClass = TypeSpec.classBuilder(modelName)
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())
            .addAnnotation(buildJsonIgnoreAnnotationSpec())
            .superclass(
                ClassName(Const.COMMON_PACKAGE, "Thing")
                    .parameterizedBy(
                        ClassName("${config.outputPackage}.attributes", Const.ATTRIBUTES_CLASS_NAME),
                        ClassName("${config.outputPackage}.features", Const.FEATURES_CLASS_NAME)
                    )
            )
            .addFunction(
                classGenerator.generateAttributesDslFunSpec(
                    Const.ATTRIBUTES_CLASS_NAME,
                    "${config.outputPackage}.attributes"
                )
            )
            .addFunction(
                classGenerator.generateFeaturesDslFunSpec(
                    Const.FEATURES_CLASS_NAME,
                    "${config.outputPackage}.features"
                )
            )
            .build()

        val file = FileSpec.builder(config.outputPackage, modelName).addType(thingModelClass)
            .addFunction(generateModelEntryDslFunSpec(modelName, config.outputPackage))
            .build()
        file.writeTo(Path(config.outputDirectory.path))
    }

    /**
     * Generates a DSL function for creating instances of the main Thing class.
     *
     * This function creates a top-level DSL function that allows users to create
     * instances of the generated Thing class using a fluent API. For example:
     * ```kotlin
     * val lamp = floorLamp {
     *     thingId = ThingId.of("device:123")
     *     attributes {
     *         // configure attributes
     *     }
     *     features {
     *         // configure features
     *     }
     * }
     * ```
     *
     * @param modelName The name of the generated Thing class
     * @param package The package name where the DSL function will be generated
     * @return A [FunSpec] representing the DSL function
     */
    private fun generateModelEntryDslFunSpec(modelName: String, `package`: String): FunSpec {
        val modelClassName = ClassName(asPackageName(`package`), modelName)
        val propertyName = asPropertyName(modelName)
        val funSpecBuilder = FunSpec.builder(propertyName)
            .returns(modelClassName)

        // Add suspend modifier if configured
        if (classGenerator.getConfig()?.generateSuspendDsl == true) {
            funSpecBuilder.addModifiers(KModifier.SUSPEND)
        }

        // Create appropriate lambda type based on suspend configuration
        val lambdaType = if (classGenerator.getConfig()?.generateSuspendDsl == true) {
            LambdaTypeName.get(receiver = modelClassName, returnType = UNIT).copy(suspending = true)
        } else {
            LambdaTypeName.get(receiver = modelClassName, returnType = UNIT)
        }

        funSpecBuilder.addParameter("block", lambdaType)

        listOf(
            "val $propertyName = ${asClassName(modelName)}()",
            "$propertyName.block()",
            "return $propertyName"
        ).forEach {
            funSpecBuilder.addStatement(it)
        }

        return funSpecBuilder.build()
    }
}
