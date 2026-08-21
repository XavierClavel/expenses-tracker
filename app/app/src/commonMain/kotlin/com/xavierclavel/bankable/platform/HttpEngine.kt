package com.xavierclavel.bankable.platform

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android, NSURLSession (Darwin) on iOS. */
expect fun platformHttpClientEngine(): HttpClientEngine
