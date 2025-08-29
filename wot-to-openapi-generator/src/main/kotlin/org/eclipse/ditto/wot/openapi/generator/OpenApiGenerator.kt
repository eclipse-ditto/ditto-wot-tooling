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

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import org.eclipse.ditto.wot.model.ThingModel

/**
 * Interface for generating OpenAPI specifications from Web of Things (WoT) Thing models.
 * This interface defines the contract for converting WoT Thing models into OpenAPI paths,
 * schemas, and documentation.
 */
interface OpenApiGenerator {
    
    /**
     * Generates OpenAPI paths for the main Thing operations.
     * 
     * @param thingModel The WoT Thing model to generate paths from
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    fun generateThingPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI)
    
    /**
     * Generates OpenAPI paths for Thing attributes operations.
     * 
     * @param thingModel The WoT Thing model containing attributes
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    fun generateThingAttributesPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI)
    
    /**
     * Generates OpenAPI paths for Thing actions operations.
     * 
     * @param thingModel The WoT Thing model containing actions
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    fun generateThingActionsPaths(thingModel: ThingModel, paths: Paths, openAPI: OpenAPI)
    
    /**
     * Generates OpenAPI schemas for a specific feature.
     * 
     * @param featureName The name of the feature
     * @param featureModel The WoT Thing model representing the feature
     * @param openAPI The OpenAPI instance for schema registration
     */
    fun generateFeatureOpenApi(featureName: String, featureModel: ThingModel, openAPI: OpenAPI)
    
    /**
     * Generates OpenAPI paths for a specific feature.
     * 
     * @param featureName The name of the feature
     * @param featureModel The WoT Thing model representing the feature
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    fun generateFeaturePaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI)
    
    /**
     * Generates OpenAPI paths for feature actions operations.
     * 
     * @param featureName The name of the feature
     * @param featureModel The WoT Thing model representing the feature
     * @param paths The OpenAPI paths object to add generated paths to
     * @param openAPI The OpenAPI instance for schema registration
     */
    fun generateFeatureActionsPaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI)
}