package com.template.contracts

import com.template.states.PropertyState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class CreateCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val firstOwner = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val secondOwner = TestIdentity(CordaX500Name("Jane Doe", "City", "BG"))
    private val owners = listOf(firstOwner, secondOwner)

    private val propertyState = PropertyState(
        owners = owners.map { it.party },
        tenants = emptyList(),
        constructedAt = Instant.now(),
        area = 100,
        address = "Uzundjovska 7-9"
    )

    @Test
    fun `Golden path`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Create())
                output(PropertyContract.ID, propertyState)
                verifies()
            }
        }
    }

    @Test
    fun `No PropertyState inputs should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Create())
                input(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState)
                failsWith("No PropertyState inputs should be consumed.")
            }
        }
    }

    @Test
    fun `One PropertyState output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Create())
                output(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState)
                failsWith("One PropertyState output should be produced.")
            }
        }
    }

    @Test
    fun `There should be at least one owner`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Create())
                output(PropertyContract.ID, propertyState.copy(owners = emptyList()))
                failsWith("There should be at least one owner.")
            }
        }
    }

    @Test
    fun `Area cannot be less than or equal to zero`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Create())
                output(PropertyContract.ID, propertyState.copy(area = 0))
                failsWith("Area cannot be less than or equal to zero.")
            }
        }
    }
}
