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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator

import org.eclipse.ditto.wot.openapi.generator.features.FeatureActionsPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.features.FeatureSchemaResolver
import org.eclipse.ditto.wot.openapi.generator.features.FeaturesPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.thing.ActionsPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.thing.AttributesPathsGenerator
import org.eclipse.ditto.wot.openapi.generator.thing.ThingPathsGenerator
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import org.eclipse.ditto.wot.model.ThingModel

/**
 * Implementation of the OpenApiGenerator interface.
 * This object provides the concrete implementation for generating OpenAPI specifications
 * from WoT Thing models by delegating to specialized generators for different components.
 */
object OpenApiGeneratorImpl : OpenApiGenerator {

    /** Generator for main Thing paths and schemas */
    var thingPathsGenerator: ThingPathsGenerator = ThingPathsGenerator

    /** Generator for Thing attributes paths and schemas */
    var attributesPathsGenerator: AttributesPathsGenerator = AttributesPathsGenerator

    /** Generator for Thing actions paths and schemas */
    var actionsPathsGenerator: ActionsPathsGenerator = ActionsPathsGenerator

    /** Generator for feature paths and schemas */
    var featuresPathsGenerator: FeaturesPathsGenerator = FeaturesPathsGenerator

    /** Generator for feature actions paths and schemas */
    var featuresActionsPathsGenerator: FeatureActionsPathsGenerator = FeatureActionsPathsGenerator

    /** Resolver for feature schemas */
    var featureSchemaResolver: FeatureSchemaResolver = FeatureSchemaResolver

    /**
     * Generates OpenAPI paths for the main Thing operations.
     * Delegates to the ThingPathsGenerator for path generation.
     * 
     * @param thingModel The WoT Thing model to generate paths from
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    override fun generateThingPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        thingPathsGenerator.generateThingPaths(thingModel, paths, openAPI)
    }

    /**
     * Generates OpenAPI paths for Thing attributes operations.
     * Delegates to the AttributesPathsGenerator for path generation.
     * 
     * @param thingModel The WoT Thing model containing attributes
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    override fun generateThingAttributesPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        attributesPathsGenerator.generateThingAttributesPaths(thingModel, paths, openAPI)
    }

    /**
     * Generates OpenAPI paths for Thing actions operations.
     * Delegates to the ActionsPathsGenerator for path generation.
     * 
     * @param thingModel The WoT Thing model containing actions
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    override fun generateThingActionsPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        actionsPathsGenerator.generateThingActionsPaths(thingModel, paths, openAPI)
    }

    /**
     * Generates OpenAPI schemas for a specific feature.
     * Resolves the feature schema and registers it with the OpenAPI instance.
     * 
     * @param featureName The name of the feature
     * @param featureModel The WoT Thing model representing the feature
     * @param openAPI The OpenAPI instance for schema registration
     */
    override fun generateFeatureOpenApi(featureName: String, featureModel: ThingModel, openAPI: OpenAPI) {
        val featureSchema = featureSchemaResolver.resolveFeatureSchema(featureName, featureModel, openAPI)
        openAPI.schema("feature_$featureName", featureSchema)
    }

    /**
     * Generates OpenAPI paths for a specific feature.
     * Delegates to the FeaturesPathsGenerator for path generation.
     * 
     * @param featureName The name of the feature
     * @param featureModel The WoT Thing model representing the feature
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    override fun generateFeaturePaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        featuresPathsGenerator.generateFeaturesPaths(featureName, featureModel, paths, openAPI)
    }

    /**
     * Generates OpenAPI paths for feature actions operations.
     * Delegates to the FeatureActionsPathsGenerator for path generation.
     * 
     * @param featureName The name of the feature
     * @param featureModel The WoT Thing model representing the feature
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    override fun generateFeatureActionsPaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI) {
        featuresActionsPathsGenerator.generateFeatureActionsPaths(featureName, featureModel, paths, openAPI)
    }
}









