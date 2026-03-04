package ch.oronk.web.endpoint)


import io.ktor.server.routing.get 
import io.ktor.server.routing.Route
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.route

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
    
fun Route.generalRoutes() {

route("/") {
      get("api/tests") {

            val idParam = call.request.queryParameters["id"]
            
    
      val query = ch.oronk.data.model.Test
      .select{ (ch.oronk.data.model.Test.id eq idParam) }

    .toList()
    val returnObj = query.map { e -> 
        ch.oronk.web.model.Test(
            id = e["id"],
            email = e["email"],
gender = e["gender"],
        )
    }
        
  call.respond(returnObj)
}

  }
}
