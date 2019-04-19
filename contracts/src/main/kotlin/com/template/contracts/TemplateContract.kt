package com.template.contracts

import com.template.states.PropertyState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class PropertyContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.PropertyContract"
    }

    interface Commands : CommandData {
        class Create : Commands
        class Sell : Commands
        class Rent : Commands
        class CancelRent: Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val inputs = tx.inputsOfType<PropertyState>()
        val outputs = tx.outputsOfType<PropertyState>()
        val command = tx.commandsOfType<PropertyContract.Commands>().single()

        when (command.value) {
            is Commands.Create -> requireThat {
                // TODO: Validate unique addresses
                "No PropertyState inputs should be consumed." using (inputs.isEmpty())
                "One PropertyState output should be produced." using (outputs.size == 1)

                val outputProperty = outputs.single()

                "There should be at least one owner." using (outputProperty.owners.isNotEmpty())
                "Area cannot be less than or equal to zero." using (outputProperty.area > 0)
            }
            is Commands.Sell -> requireThat {
                "Only one PropertyState input should be consumed." using (inputs.size == 1)
                "Only one PropertyState output should be produced." using (outputs.size == 1)

                val inputProperty = inputs.single()
                val outputProperty = outputs.single()

                "Only the owner should change." using (
                        inputProperty == outputProperty.copy(owners = inputProperty.owners))
                "There should be at least one buyer." using (outputProperty.owners.isNotEmpty())
            }
        }
    }
}
