package com.hypto.iam.server.security

import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.IamResources
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.utils.policy.PolicyRequest
import com.hypto.iam.server.utils.policy.PolicyValidator
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.server.auth.authentication
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/* Authorization logic is defined based on - https://www.ximedes.com/2020-09-17/role-based-authorization-in-ktor/ */

private typealias Action = String

const val URL_SEPARATOR = '/'

private val logger = KotlinLogging.logger { }

class AuthorizationException(override val message: String) : Exception(message)

/**
 * This class is used in api request flow. This performs user authorization checks before allowing any action
 */

@Suppress("UnusedPrivateMember")
class Authorization(config: Configuration) : KoinComponent {
    private val policyValidator: PolicyValidator by inject()

    class Configuration : KoinComponent

    fun interceptPipeline(
        pipeline: ApplicationCallPipeline,
        any: Set<Action>? = null,
        all: Set<Action>? = null,
        none: Set<Action>? = null,
        getResourceHrn: (ApplicationRequest) -> ResourceHrn
    ) {
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, authorizationPhase)
        pipeline.intercept(authorizationPhase) {
            val principal =
                call.authentication.principal<UserPrincipal>() ?: throw AuthenticationException("Missing principal")

            val principalHrn = principal.hrnStr
            val resourceHrn = getResourceHrn(context.request)
            val denyReasons = mutableListOf<String>()
            all?.let {
                val policyRequests = all.map {
                    val actionHrn = ActionHrn(resourceHrn.organization, null, resourceHrn.resource, it)
                    PolicyRequest(principalHrn, resourceHrn.toString(), actionHrn.toString())
                }.toList()
                if (!policyValidator.validate(principal.policies.stream(), policyRequests)) {
                    denyReasons += "Principal $principalHrn lacks one or more permission(s) -" +
                        "  ${policyRequests.joinToString { it.action }}"
                }
            }

            any?.let {
                val policyRequests = any.map {
                    val actionHrn = ActionHrn(resourceHrn.organization, null, resourceHrn.resource, it)
                    PolicyRequest(principalHrn, resourceHrn.toString(), actionHrn.toString())
                }.toList()
                if (!policyValidator.validateAny(principal.policies.stream(), policyRequests)) {
                    denyReasons += "Principal $principalHrn has none of the permission(s) -" +
                        "  ${policyRequests.joinToString { it.action }}"
                }
            }

            none?.let {
                val policyRequests = none.map {
                    val actionHrn = ActionHrn(resourceHrn.organization, null, resourceHrn.resource, it)
                    PolicyRequest(principalHrn, resourceHrn.toString(), actionHrn.toString())
                }.toList()
                if (!policyValidator.validateNone(principal.policies.stream(), policyRequests)) {
                    denyReasons += "Principal $principalHrn shouldn't have these permission(s) -" +
                        "  ${policyRequests.joinToString { it.action }}"
                }
            }

            if (denyReasons.isNotEmpty()) {
                val message = denyReasons.joinToString(". ")
                logger.warn { "Authorization failed for ${call.request.path()}. $message" }
                throw AuthorizationException(message)
            }
        }
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, Authorization> {
        override val key = AttributeKey<Authorization>("Authorization")
        val authorizationPhase = PipelinePhase("Authorization")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): Authorization {
            val configuration = Configuration().apply(configure)
            return Authorization(configuration)
        }
    }
}

class AuthorizedRouteSelector(private val description: String) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant
    override fun toString(): String = "(authorize $description)"
}

private fun Route.authorizedRoute(
    any: Set<Action>? = null,
    all: Set<Action>? = null,
    none: Set<Action>? = null,
    getResourceHrn: (ApplicationRequest) -> ResourceHrn,
    build: Route.() -> Unit
): Route {
    val description = listOfNotNull(
        any?.let { "anyOf (${any.joinToString(" ")})" },
        all?.let { "allOf (${all.joinToString(" ")})" },
        none?.let { "noneOf (${none.joinToString(" ")})" }
    ).joinToString(",")
    val authorizedRoute = createChild(AuthorizedRouteSelector(description))
    application.plugin(Authorization).interceptPipeline(
        authorizedRoute,
        any,
        all,
        none,
        getResourceHrn
    )
    authorizedRoute.build()
    return authorizedRoute
}

fun getResourceHrnFunc(
    resourceNameIndex: Int,
    resourceInstanceIndex: Int,
    organizationIdIndex: Int
): (ApplicationRequest) -> ResourceHrn {
    return { request ->
        val pathSegments = request.path().trim(URL_SEPARATOR).split(URL_SEPARATOR)
        ResourceHrn(
            pathSegments[organizationIdIndex],
            null,
            IamResources.resourceMap[pathSegments[resourceNameIndex]]!!,
            pathSegments[resourceInstanceIndex]
        )
    }
}

fun Route.withPermission(
    action: Action,
    getResourceHrn: (ApplicationRequest) -> ResourceHrn,
    build: Route.() -> Unit
) = authorizedRoute(all = setOf(action), getResourceHrn = getResourceHrn, build = build)

fun Route.withAllPermission(
    vararg action: Action,
    getResourceHrn: (ApplicationRequest) -> ResourceHrn,
    build: Route.() -> Unit
) = authorizedRoute(all = action.toSet(), getResourceHrn = getResourceHrn, build = build)

fun Route.withAnyPermission(
    vararg action: Action,
    getResourceHrn: (ApplicationRequest) -> ResourceHrn,
    build: Route.() -> Unit
) = authorizedRoute(any = action.toSet(), getResourceHrn = getResourceHrn, build = build)

fun Route.withoutPermission(
    action: Action,
    getResourceHrn: (ApplicationRequest) -> ResourceHrn,
    build: Route.() -> Unit
) = authorizedRoute(none = setOf(action), getResourceHrn = getResourceHrn, build = build)
