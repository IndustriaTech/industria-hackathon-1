package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PropertyContract
import com.template.states.PropertyState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CreatePropertyFlowInitiator(
    private val owners: List<Party>,
    private val tenants: List<Party>,
    private val area: Int,
    private val address: String,
    private val notaryToUse: Party
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val propertyState = PropertyState(
            owners = owners,
            tenants = tenants,
            area = area,
            address = address,
            constructedAt = Instant.now()
        )

        val createPropertyCommand = Command(
            PropertyContract.Commands.Create(),
            listOf(owners.first().owningKey)
        )

        val transactionBuilder = TransactionBuilder(notaryToUse)
            .addOutputState(propertyState, PropertyContract.ID)
            .addCommand(createPropertyCommand)

        transactionBuilder.verify(serviceHub)

        val initialTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        return subFlow(FinalityFlow(initialTransaction, emptySet<FlowSession>()))
    }
}
