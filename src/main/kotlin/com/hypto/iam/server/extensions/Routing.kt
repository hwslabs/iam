package com.hypto.iam.server.extensions

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

// Extension functions to support multiple paths for a single body

fun Route.get(
    vararg paths: String,
    body: suspend io.ktor.util.pipeline.PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
) {
    for (path in paths) {
        get(path, body)
    }
}

fun Route.patch(
    vararg paths: String,
    body: suspend io.ktor.util.pipeline.PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
) {
    for (path in paths) {
        patch(path, body)
    }
}

fun Route.post(
    vararg paths: String,
    body: suspend io.ktor.util.pipeline.PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
) {
    for (path in paths) {
        post(path, body)
    }
}

fun Route.delete(
    vararg paths: String,
    body: suspend io.ktor.util.pipeline.PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
) {
    for (path in paths) {
        delete(path, body)
    }
}
