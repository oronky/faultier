package ch.oronk.data.model
import org.jetbrains.exposed.v1.core.Table

import kotlin.uuid.ExperimentalUuidApi

data class TestEntity (
  val id: String,
    val email : Int,
    val gender : Int,
)
object Test : Table() {

    @OptIn(ExperimentalUuidApi::class)

    val id = uuid("id").autoGenerate()
    val email = integer("email")

    val gender = integer("gender")

    @OptIn(ExperimentalUuidApi::class)

    override val primaryKey = PrimaryKey(id)

}
