package ch.oronk.web.model
data class Test(
  val id: String,
    val email: Int,
    val gender: Int
) {


    fun copyFrom(other: ch.oronk.data.model.TestEntity): Test {

        return copy(
            email = other.email,
            gender = other.gender
        )

    }

}

