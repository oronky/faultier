package ch.oronk.web.endpoint


import ch.oronk.data.model.Test
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

import io.ktor.server.routing.get
import io.ktor.server.routing.Route
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.http.HttpStatusCode

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select

@OptIn(ExperimentalUuidApi::class)
fun Route.generalRoutes() {

    route("/") {
        get("api/tests") {


            var idParamReq = call.request.queryParameters["id"]

            if (idParamReq == null) {
                call.respond(HttpStatusCode.BadRequest, "idParam is required and needs to be type {uuid}")
                return@get
            }

            val idParam = Uuid.parse(idParamReq)


            val query = ch.oronk.data.model.Test
                .select((ch.oronk.data.model.Test.id eq idParam))

                .toList()
            val returnObj = query.map { e ->
                ch.oronk.web.model.Test(
                    id = e.get(Test.id).toString(),
                    email = e.get("email"),
                    gender = e.get("gender"),
                )
            }

            call.respond(returnObj)
        }

    }
}
