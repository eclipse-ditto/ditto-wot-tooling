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
package org.eclipse.ditto.wot.kotlin.generator.plugin.loader

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.future.asDeferred
import org.eclipse.ditto.base.model.headers.DittoHeaders
import org.eclipse.ditto.wot.api.config.DefaultWotConfig
import org.eclipse.ditto.wot.api.generator.WotThingModelExtensionResolver
import org.eclipse.ditto.wot.api.provider.WotThingModelFetcher
import org.eclipse.ditto.wot.model.ThingModel
import java.net.URL
import java.util.concurrent.Executors

object DittoBasedWotLoader {

    private val wotThingModelFetcher: WotThingModelFetcher
    private val wotThingModelExtensionResolver: WotThingModelExtensionResolver

    init {
        val cachedThreadPool = Executors.newCachedThreadPool()
        val wotConfig = DefaultWotConfig.of(ConfigFactory.load("wot-config"))
        wotThingModelFetcher = WotThingModelFetcher.of(wotConfig, ToolJsonDownloader, cachedThreadPool)
        wotThingModelExtensionResolver =
            WotThingModelExtensionResolver.of(
                wotThingModelFetcher,
                cachedThreadPool
            )
    }

    suspend fun load(url: URL): ThingModel {
        return wotThingModelFetcher.fetchThingModel(url, DittoHeaders.empty())
            .thenCompose { thingModel: ThingModel? ->
                wotThingModelExtensionResolver
                    .resolveThingModelExtensions(thingModel, DittoHeaders.empty())
                    .thenCompose { thingModelWithExtensions: ThingModel? ->
                        wotThingModelExtensionResolver.resolveThingModelRefs(
                            thingModelWithExtensions,
                            DittoHeaders.empty()
                        )
                    }
            }.asDeferred().await()
    }
}
