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
 * Package for WoT Thing-specific OpenAPI generation components.
 * 
 * This package contains generators and resolvers specifically for handling
 * WoT Thing models and their components:
 * 
 * - Thing properties and attributes
 * - Thing actions and their inputs/outputs
 * - Thing-level schemas and paths
 * 
 * Key components:
 * - [ThingPathsGenerator]: Generates OpenAPI paths for main Thing operations
 * - [AttributesPathsGenerator]: Generates paths for Thing attributes
 * - [ActionsPathsGenerator]: Generates paths for Thing actions
 * - [AttributeSchemaResolver]: Resolves and creates schemas for Thing attributes
 * 
 * These components work together to convert WoT Thing models into
 * comprehensive OpenAPI specifications with proper schema definitions
 * and REST API endpoints.
 */
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.thing
