package com.template.contracts

import com.template.states.PropertyState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class SellCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val firstOwner = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val secondOwner = TestIdentity(CordaX500Name("Jane Doe", "City", "BG"))
    private val owners = listOf(firstOwner, secondOwner)
    private val buyer = TestIdentity(CordaX500Name("Richard Roe", "City", "BG"))
    private val firstTenant = TestIdentity(CordaX500Name("Johny Roe", "City", "BG"))
    private val secondTenant = TestIdentity(CordaX500Name("Johny Doe", "City", "BG"))

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
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                input(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState.copy(owners = listOf(buyer.party)))
                verifies()
            }
        }
    }

    @Test
    fun `Only one PropertyState input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                output(PropertyContract.ID, propertyState.copy(owners = listOf(buyer.party)))
                failsWith("Only one PropertyState input should be consumed.")
            }
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                input(PropertyContract.ID, propertyState)
                input(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState.copy(owners = listOf(buyer.party)))
                failsWith("Only one PropertyState input should be consumed.")
            }
        }
    }

    @Test
    fun `Only one PropertyState output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                input(PropertyContract.ID, propertyState)
                failsWith("Only one PropertyState output should be produced.")
            }
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                input(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState.copy(owners = listOf(buyer.party)))
                output(PropertyContract.ID, propertyState.copy(owners = listOf(buyer.party)))
                failsWith("Only one PropertyState output should be produced.")
            }
        }
    }

    @Test
    fun `Only the owner should change`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                input(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState.copy(owners = listOf(buyer.party), area = 200))
                failsWith("Only the owner should change.")
            }
        }
    }

    @Test
    fun `There should be at least one buyer`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.Sell())
                input(PropertyContract.ID, propertyState)
                output(PropertyContract.ID, propertyState.copy(owners = emptyList()))
                failsWith("There should be at least one buyer.")
            }
        }
    }

}
