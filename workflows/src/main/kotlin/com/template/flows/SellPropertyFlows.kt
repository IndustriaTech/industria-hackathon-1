package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PropertyContract
import com.template.states.PropertyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SellPropertyFlowInitiator(
    private val buyers: List<Party>,
    private val notaryToUse: Party,
    private val linearId : UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            linearId = listOf(linearId))

        val results = serviceHub.vaultService.queryBy<PropertyState>(queryCriteria)
        val inputProperty = results.states.singleOrNull()
                            ?: throw FlowException("No such state")
        val sellers = inputProperty.state.data.owners

        val outputProperty = inputProperty.state.data.copy(owners = buyers)

        val inputState = inputProperty.state.data

        val participants = (inputState.participants + outputProperty.participants).toSet()
        val signers = participants.map { it.owningKey }

        val sellPropertyCommand = Command(
            PropertyContract.Commands.Sell(),
            signers
        )

        val transactionBuilder = TransactionBuilder(notaryToUse)
            .addInputState(inputProperty)
            .addOutputState(outputProperty, PropertyContract.ID)
            .addCommand(sellPropertyCommand)

        transactionBuilder.verify(serviceHub)

        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = (participants - ourIdentity).map { initiateFlow(it) }.toSet()

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(
            partiallySignedTransaction, sessions
        ))
        return subFlow(FinalityFlow(fullySignedTransaction, sessions))

    }
}

@InitiatedBy(SellPropertyFlowInitiator::class)
class SellPropertyFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {}
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}
