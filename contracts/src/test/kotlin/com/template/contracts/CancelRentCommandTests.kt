package com.template.contracts

import com.template.states.PropertyState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class CancelCancelRentCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val firstOwner = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val firstTenant = TestIdentity(CordaX500Name("Jeremy", "City", "BG"))
    private val owners = listOf(firstOwner.party)
    private val tenants = listOf(firstTenant.party)

    private val inputState = PropertyState(
            owners = owners,
            tenants = tenants,
            constructedAt = Instant.now(),
            area = 100,
            address = "Uzundjovska 7-9"
    )

    private val outputState = inputState.copy(
            tenants = emptyList()
    )

    @Test
    fun `Golden path`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                input(PropertyContract.ID, inputState)
                output(PropertyContract.ID, outputState)
                verifies()
            }
        }
    }

    @Test
    fun `Only one input state should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                output(PropertyContract.ID, outputState)
                failsWith("Only one input state should be consumed.")
            }

            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                input(PropertyContract.ID, inputState)
                input(PropertyContract.ID, inputState)
                output(PropertyContract.ID, outputState)
                failsWith("Only one input state should be consumed.")
            }
        }
    }

    @Test
    fun `Only one output state should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                input(PropertyContract.ID, inputState)
                failsWith("Only one output state should be produced.")
            }

            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                input(PropertyContract.ID, inputState)
                output(PropertyContract.ID, outputState)
                output(PropertyContract.ID, outputState)
                failsWith("Only one output state should be produced.")
            }
        }
    }

    @Test
    fun `One or more tenant should be deleted`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                input(PropertyContract.ID, outputState)
                output(PropertyContract.ID, inputState)
                failsWith("One or more tenants should be deleted.")
            }
        }
    }

    @Test
    fun `Only the tenants number should change`() {
        ledgerServices.ledger {
            transaction {
                command(firstOwner.publicKey, PropertyContract.Commands.CancelRent())
                input(PropertyContract.ID, inputState)
                output(PropertyContract.ID, outputState.copy(area = 200))
                failsWith("Only the tenants number should change.")
            }
        }
    }
}
