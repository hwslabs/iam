package com.hypto.iam.server.extensions

import com.hypto.iam.server.security.getResourceHrnFunc
import com.hypto.iam.server.security.withPermission
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext

// Extension functions to support multiple paths for a single body
fun Route.get(
    vararg paths: String,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (path in paths) {
        get(path, body)
    }
}

fun Route.patch(
    vararg paths: String,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (path in paths) {
        patch(path, body)
    }
}

fun Route.post(
    vararg paths: String,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (path in paths) {
        post(path, body)
    }
}

fun Route.delete(
    vararg paths: String,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (path in paths) {
        delete(path, body)
    }
}

// Functions composing withPermission and routing builder functions like get, post, etc.
fun Route.getWithPermission(
    routeOptions: List<RouteOption>,
    action: String,
    validateOrgIdFromPath: Boolean = true,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (routeOption in routeOptions) {
        withPermission(
            action,
            getResourceHrnFunc(routeOption),
            validateOrgIdFromPath,
        ) {
            get(routeOption.pathTemplate, body)
        }
    }
}

fun Route.patchWithPermission(
    routeOptions: List<RouteOption>,
    action: String,
    validateOrgIdFromPath: Boolean = true,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (routeOption in routeOptions) {
        withPermission(
            action,
            getResourceHrnFunc(routeOption),
            validateOrgIdFromPath,
        ) {
            patch(routeOption.pathTemplate, body)
        }
    }
}

fun Route.postWithPermission(
    routeOptions: List<RouteOption>,
    action: String,
    validateOrgIdFromPath: Boolean = true,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (routeOption in routeOptions) {
        withPermission(
            action,
            getResourceHrnFunc(routeOption),
            validateOrgIdFromPath,
        ) {
            post(routeOption.pathTemplate, body)
        }
    }
}

fun Route.deleteWithPermission(
    routeOptions: List<RouteOption>,
    action: String,
    validateOrgIdFromPath: Boolean = true,
    body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
) {
    for (routeOption in routeOptions) {
        withPermission(
            action,
            getResourceHrnFunc(routeOption),
            validateOrgIdFromPath,
        ) {
            delete(routeOption.pathTemplate, body)
        }
    }
}

data class RouteOption(
    val pathTemplate: String,
    val resourceNameIndex: Int,
    val resourceInstanceIndex: Int,
    val organizationIdIndex: Int? = null,
    val subOrganizationNameIndex: Int? = null,
)
