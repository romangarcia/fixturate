package fixturate

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import dridco.fixturate.Fixture
import fixturate.model.TestUser

class FixtureSpec extends WordSpec with ShouldMatchers {

    "Fixture" should {
        "fail" in {
            val pepe = Fixture(classOf[TestUser]).get("prueba")
            println(pepe)
            pepe.getFirstName should be ("Pepe")
            pepe.getLastName should be ("Mugica")
            pepe.age should be (65)
            pepe.getAge should be (65)
        }
        
    }
}