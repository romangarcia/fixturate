package fixturate

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import dridco.fixturate.Fixture
import fixturate.model.TestUser
import fixturate.model.TestOrder

class FixtureSpec extends WordSpec with ShouldMatchers {

    "Fixture" should {
        "retrieve complete TestUser" in {
            val pepe = Fixture[TestUser].get("pepe")
            println(pepe)
            pepe.getFirstName should be ("Pepe")
            pepe.getLastName should be ("Mugica")
            pepe.age should be (65)
            pepe.getAge should be (65)
        }
        
        "retrieve complete TestOrder" in {
            val order = Fixture[TestOrder].get()
            println(order)
            
            order.item should be (4321L)
            order.description should be ("Un producto berreta")
            order.user should be (TestUser("Pepe", "Mugica"))
            
        }
    }
    
    
}