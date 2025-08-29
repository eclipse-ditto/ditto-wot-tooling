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
 * Package for WoT Feature-specific OpenAPI generation components.
 * 
 * This package contains generators and resolvers specifically for handling
 * WoT Feature models and their components:
 * 
 * - Feature properties and schemas
 * - Feature actions and their inputs/outputs
 * - Feature-level paths and endpoints
 * 
 * Key components:
 * - [FeaturesPathsGenerator]: Generates OpenAPI paths for feature operations
 * - [FeatureActionsPathsGenerator]: Generates paths for feature actions
 * - [FeatureSchemaResolver]: Resolves and creates schemas for features
 * 
 * These components handle the conversion of WoT Feature models into
 * OpenAPI specifications, supporting modular and extensible Thing models
 * through feature-based architecture.
 */
package org.eclipse.ditto.wot.openapi.generator.features
