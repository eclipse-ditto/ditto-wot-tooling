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
package org.eclipse.ditto.wot.openapi.generator

import io.swagger.v3.core.util.ObjectMapperFactory
import io.swagger.v3.core.util.Yaml31
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import kotlinx.coroutines.runBlocking
import org.eclipse.ditto.wot.model.BaseLink
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.features.FeatureSchemaResolver
import org.eclipse.ditto.wot.openapi.generator.loader.DittoBasedWotLoader
import org.eclipse.ditto.wot.openapi.generator.providers.ErrorProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.thing.AttributeSchemaResolver
import java.io.File
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.isDirectory
import kotlin.jvm.optionals.getOrNull

/**
 * Main orchestrator for loading WoT models and generating OpenAPI specifications.
 * This object coordinates the entire process of loading WoT Thing models,
 * resolving their dependencies, and generating complete OpenAPI specifications.
 */
object WotLoader {

    /** OpenAPI generator instance */
    var generator: OpenApiGenerator = OpenApiGeneratorImpl

    /** WoT model loader based on Ditto libraries */
    var dittoWotLoader: DittoBasedWotLoader = DittoBasedWotLoader

    /** Resolver for attribute schemas */
    var attributeSchemaResolver: AttributeSchemaResolver = AttributeSchemaResolver

    /** Resolver for feature schemas */
    var featureSchemaResolver: FeatureSchemaResolver = FeatureSchemaResolver

    /** Provider for error schemas */
    var errorProvider: ErrorProvider = ErrorProvider

    /** Provider for common parameters */
    var parametersProvider: ParametersProvider = ParametersProvider

    /**
     * Generates a complete OpenAPI specification from a WoT Thing model.
     * Creates the OpenAPI structure, resolves all schemas, and generates paths
     * for the main Thing, its attributes, actions, and features.
     *
     * @param thingModel The root WoT Thing model
     * @param modelBaseUrl Base URL for model loading
     * @param modelName Name of the model
     * @param modelVersion Version of the model
     * @param dittoBaseUrl Base URL for the Ditto API (e.g., "https://ditto.example.com")
     */
    fun generate(
        thingModel: ThingModel,
        modelBaseUrl: String,
        modelName: String,
        modelVersion: String,
        dittoBaseUrl: String
    ) {
        val links = thingModel.links.getOrNull() ?: emptyList<BaseLink<*>>()

        validateTmExtendsLinks(links)
        val openAPI = OpenAPI(SpecVersion.V31)
            .info(provideInfo(thingModel))
            .openapi("3.1.0")
            .servers(provideServers(dittoBaseUrl))
            .tags(provideTags(thingModel))
            .security(
                listOf(SecurityRequirement().addList("Bearer"))
            )

        openAPI.components(
            Components()
                .schemas(errorProvider.provideDittoErrorSchemas())
                .parameters(
                    mutableMapOf(
                        parametersProvider.resolveParameter(ParametersProvider.PATH_PARAM_THING_ID),
                        parametersProvider.resolveParameter(ParametersProvider.QUERY_PARAM_CONDITION),
                        parametersProvider.resolveParameter(ParametersProvider.QUERY_PARAM_FIELDS)
                    )
                )
                .securitySchemes(
                    mapOf(
                        "Bearer" to SecurityScheme()
                            .name("Bearer")
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("A JSON Web Token issued by a configured OpenID Connect Identity Provider")
                    )
                )
        )
            .schema("attributes", attributeSchemaResolver.provideAttributesSchema(thingModel, openAPI))

        val paths = Paths()
        generator.generateThingPaths(thingModel, paths, openAPI)
        generator.generateThingAttributesPaths(thingModel, paths, openAPI)
        generator.generateThingActionsPaths(thingModel, paths, openAPI)

        runBlocking {
            val featureModels = getSubModels(links)
            openAPI.schema("features", featureSchemaResolver.resolveCompleteFeaturesSchema(featureModels, openAPI))
            featureModels.forEach {
                val featureName = it.first
                val featureModel = it.second
                val featureTitle = featureModel.title.getOrNull()?.toString() ?: featureName
                val tag = Tag()
                    .name("Feature: $featureTitle")
                    .description(featureModel.description.getOrNull()?.toString())
                if (!openAPI.tags.contains(tag)) {
                    openAPI.addTagsItem(tag)
                }
                if (featureModel.actions.isEmpty.not()) {
                    val actionTag = Tag()
                        .name("Feature: $featureTitle - Actions")
                        .description("Actions to be invoked on the $featureTitle")
                    if (!openAPI.tags.contains(actionTag)) {
                        openAPI.addTagsItem(actionTag)
                    }
                }
                generator.generateFeatureOpenApi(featureName, featureModel, openAPI)
                generator.generateFeaturePaths(featureName, featureModel, paths, openAPI)
                generator.generateFeatureActionsPaths(featureName, featureModel, paths, openAPI)
            }
        }

        openAPI.paths(paths)

        if (!Path("generated").isDirectory()) {
            Path("generated").createDirectory()
        }
        val fullModelPath = "$modelBaseUrl/${modelName}-${modelVersion}.tm.jsonld"
        val modelYamlName = fullModelPath.substring(fullModelPath.lastIndexOf('/'))
            .replace(Regex("tm\\.jsonld"), "yaml")
        val outputStream = File("generated/$modelYamlName").outputStream()
        val jsonGenerator = ObjectMapperFactory.createYaml31()
            .createGenerator(outputStream)

        Yaml31.mapper().writeValue(jsonGenerator, openAPI)
    }


    /**
     * Creates OpenAPI Info object from WoT Thing model metadata.
     *
     * @param thingModel The WoT Thing model
     * @return OpenAPI Info object with title, description, and version
     */
    private fun provideInfo(thingModel: ThingModel): Info? = Info()
        .title(thingModel.title.getOrNull()?.toString())
        .description(thingModel.description.getOrNull()?.toString())
        .version(thingModel.version.getOrNull()?.model?.getOrNull())

    /**
     * Creates OpenAPI Server objects for the specified Ditto base URL.
     *
     * @param dittoBaseUrl The base URL for the Ditto API
     * @return List of OpenAPI Server objects
     */
    private fun provideServers(dittoBaseUrl: String) = listOf(
        Server()
            .description("Ditto API")
            .url("$dittoBaseUrl/api/2/things")
    )

    /**
     * Note: The Ditto API URL is now provided as a parameter to the generate() method.
     * This allows users to configure their own Ditto deployment endpoints without modifying the code.
     * 
     * Example usage:
     * WotLoader.generate(thingModel, "1.0.0", "myThing", "https://ditto.mycompany.com")
     */

    /**
     * Creates OpenAPI Tag objects for the Thing model.
     *
     * @param thingModel The WoT Thing model
     * @return List of OpenAPI Tag objects
     */
    private fun provideTags(thingModel: ThingModel): MutableList<Tag> {
        val readableName = thingModel.title.getOrNull()?.toString()
        val tags : MutableList<Tag> = mutableListOf()
        tags.add(
            Tag()
                .name("Thing")
                .description("Complete ${readableName ?: "Thing"}")
        )
        if (thingModel.properties.isEmpty.not()) {
            tags.add(
                Tag()
                    .name("Attributes")
                    .description("Attributes of the ${readableName ?: "Thing"}")
            )
        }
        if (thingModel.actions.isEmpty.not()) {
            tags.add(
                Tag()
                    .name("Actions")
                    .description("Actions to be invoked on the ${readableName ?: "Thing"}")
            )
        }
        return tags
    }


    /**
     * Loads submodel Thing models from WoT links.
     *
     * @param links Collection of WoT links
     * @return List of pairs containing submodel names and their corresponding Thing models
     */
    suspend fun getSubModels(
        links: Iterable<BaseLink<*>>
    ): List<Pair<String, ThingModel>> {
        return links.filter { isSubmodel(it) }.map {
            val instanceName = it.toJson().getValue("instanceName").getOrNull()?.asString()!!
            val submodel = loadModelFromUrl(it.href.toString())

            val submodelLinks = submodel.links.getOrNull() ?: emptyList<BaseLink<*>>()
            validateTmExtendsLinks(submodelLinks)

            instanceName to submodel
        }
    }

    /**
     * Checks if a WoT link represents a submodel relation.
     *
     * @param it The WoT link to check
     * @return true if the link is a submodel relation, false otherwise
     */
    fun isSubmodel(it: BaseLink<*>) = LinkRelationType.fromWotName(it.rel.get()) == LinkRelationType.SUBMODEL

    /**
     * Loads a WoT Thing model from a URL.
     *
     * @param modelUrl The URL of the WoT model to load
     * @return The loaded Thing model
     */
    suspend fun loadModelFromUrl(modelUrl: String): ThingModel {
        val thingModel = dittoWotLoader.load(URL(modelUrl))

        // Validate the model immediately after loading
        val links = thingModel.links.getOrNull() ?: emptyList<BaseLink<*>>()
        validateTmExtendsLinks(links)

        return thingModel
    }

    /**
     * Loads a WoT Thing model with the specified parameters.
     *
     * @param modelBaseUrl Base URL for model loading
     * @param modelName Name of the model
     * @param version Version of the model
     * @return The loaded Thing model
     */
    suspend fun loadModel(modelBaseUrl: String, modelName: String, version: String): ThingModel {
        val modelUrl = "$modelBaseUrl/${modelName}-${version}.tm.jsonld"
        println("Generating from model: $modelUrl")
        val thingModel = loadModelFromUrl(modelUrl)

        val links = thingModel.links.getOrNull() ?: emptyList<BaseLink<*>>()
        validateTmExtendsLinks(links)

        return thingModel
    }

    /**
     * Validates that the WoT Thing Model has at most one tm:extends link.
     * According to the WoT standard, multiple tm:extends links are forbidden.
     *
     * @param links Collection of WoT links to validate
     * @throws IllegalArgumentException if multiple tm:extends links are found
     */
    private fun validateTmExtendsLinks(links: Iterable<BaseLink<*>>) {
        val extendsLinks = links.filter {
            LinkRelationType.fromWotName(it.rel.get()) == LinkRelationType.EXTENDS
        }

        if (extendsLinks.size > 1) {
            val linkUrls = extendsLinks.joinToString(", ") { it.href.toString() }
            throw IllegalArgumentException(
                "WoT Thing Model validation failed: Multiple 'tm:extends' links are forbidden by the standard. " +
                "Found ${extendsLinks.size} extends links: $linkUrls. " +
                "Only one 'tm:extends' link is allowed per Thing Model."
            )
        }
    }

}




