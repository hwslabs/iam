package com.hypto.iam.server.security

import com.hypto.iam.server.configs.AppConfig
import com.hypto.iam.server.utils.ActionHrn
import com.hypto.iam.server.utils.ResourceHrn
import com.hypto.iam.server.utils.policy.PolicyRequest
import com.hypto.iam.server.utils.policy.PolicyValidator
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.request.path
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/* Authorization logic is defined based on - https://www.ximedes.com/2020-09-17/role-based-authorization-in-ktor/ */

private typealias Action = String

private val logger = KotlinLogging.logger { }

class AuthorizationException(override val message: String) : Exception(message)

/**
 * This class is used in api request flow. This performs user authorization checks before allowing any action
 */
@Suppress("UnusedPrivateMember")
class Authorization(config: Configuration) : KoinComponent {
    private val policyValidator: PolicyValidator by inject()
    private val appConfig: AppConfig.Config by inject()

    class Configuration : KoinComponent

    fun interceptPipeline(
        pipeline: ApplicationCallPipeline,
        any: Set<Action>? = null,
        all: Set<Action>? = null,
        none: Set<Action>? = null
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, Authentication.ChallengePhase)
        pipeline.insertPhaseAfter(Authentication.ChallengePhase, authorizationPhase)
        pipeline.intercept(authorizationPhase) {

            val principal =
                call.authentication.principal<UserPrincipal>() ?: throw AuthenticationException("Missing principal")

            val principalHrn = principal.hrnStr
            val resourceHrn = ResourceHrn(context.request)
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

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Authorization> {
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
    build: Route.() -> Unit
): Route {
    val description = listOfNotNull(
        any?.let { "anyOf (${any.joinToString(" ")})" },
        all?.let { "allOf (${all.joinToString(" ")})" },
        none?.let { "noneOf (${none.joinToString(" ")})" }
    ).joinToString(",")
    val authorizedRoute = createChild(AuthorizedRouteSelector(description))
    application.feature(Authorization).interceptPipeline(authorizedRoute, any, all, none)
    authorizedRoute.build()
    return authorizedRoute
}

fun Route.withPermission(action: Action, build: Route.() -> Unit) = authorizedRoute(all = setOf(action), build = build)

fun Route.withAllPermission(vararg action: Action, build: Route.() -> Unit) =
    authorizedRoute(all = action.toSet(), build = build)

fun Route.withAnyPermission(vararg action: Action, build: Route.() -> Unit) =
    authorizedRoute(any = action.toSet(), build = build)

fun Route.withoutPermission(vararg action: Action, build: Route.() -> Unit) =
    authorizedRoute(none = action.toSet(), build = build)
