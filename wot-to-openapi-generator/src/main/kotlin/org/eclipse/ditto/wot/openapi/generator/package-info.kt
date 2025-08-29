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

/**
 * Main package for the WoT to OpenAPI generator.
 * 
 * This package contains the core components for converting Web of Things (WoT) Thing models
 * into OpenAPI 3.1.0 specifications. The generator supports:
 * 
 * - Converting WoT Thing models to OpenAPI schemas and paths
 * - Handling WoT features and their properties
 * - Resolving nested schemas and references
 * - Generating proper OpenAPI documentation
 * - Configurable Ditto API endpoints
 * 
 * Key components:
 * - [OpenApiGenerator]: Interface defining the generation contract
 * - [OpenApiGeneratorImpl]: Main implementation orchestrating the generation process
 * - [WotLoader]: Loads WoT models and coordinates the generation
 * - [GeneratorStarter]: Command-line entry point
 * - [Utils]: Utility functions for schema conversion and naming
 * 
 * @see org.eclipse.ditto.wot.openapi.generator.thing for Thing-specific generators
 * @see org.eclipse.ditto.wot.openapi.generator.features for Feature-specific generators
 * @see org.eclipse.ditto.wot.openapi.generator.providers for OpenAPI component providers
 * @see org.eclipse.ditto.wot.openapi.generator.loader for WoT model loading utilities
 */
package org.eclipse.ditto.wot.openapi.generator
