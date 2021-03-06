package com.template

import com.template.flows.CancelRentPropertyFlowInitiator
import com.template.flows.CreatePropertyFlowInitiator
import com.template.flows.RentPropertyFlowInitiator
import com.template.flows.SellPropertyFlowInitiator
import com.template.states.PropertyState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )))
    private val nodeA = network.createNode()
    private val nodeB = network.createNode()


    private fun createProperty(): List<PropertyState> {
        val flow = CreatePropertyFlowInitiator(
            nodeA.info.legalIdentities,
            emptyList(),
            area=100,
            address="Uzundjovska 7-9",
            notaryToUse = network.defaultNotaryIdentity
        )

        val resultFuture = nodeA.startFlow(flow)
        network.runNetwork()
        return resultFuture.get().tx.outputsOfType<PropertyState>()
    }

    private fun sellProperty(
        buyers: List<Party>,
        linearId: UniqueIdentifier
    ): List<PropertyState> {
        val flow = SellPropertyFlowInitiator(
            buyers = buyers,
            notaryToUse = network.defaultNotaryIdentity,
            linearId = linearId
        )

        val resultFuture = nodeA.startFlow(flow)
        network.runNetwork()
        return resultFuture.get().tx.outputsOfType<PropertyState>()
    }

    private fun rentProperty(
            tenants: List<Party>,
            linearId: UniqueIdentifier
    ): List<PropertyState> {
        val flow = RentPropertyFlowInitiator(
                tenants = tenants,
                notaryToUse = network.defaultNotaryIdentity,
                linearId = linearId
        )

        val resultFuture = nodeA.startFlow(flow)
        network.runNetwork()
        return resultFuture.get().tx.outputsOfType<PropertyState>()
    }

    private fun cancelRentProperty(
            tenants: List<Party>,
            linearId: UniqueIdentifier
    ): List<PropertyState> {
        val flow = RentPropertyFlowInitiator(
                tenants = tenants,
                notaryToUse = network.defaultNotaryIdentity,
                linearId = linearId
        )

        val resultFuture = nodeA.startFlow(flow)
        network.runNetwork()
        return resultFuture.get().tx.outputsOfType<PropertyState>()
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `Test creating property`() {
        createProperty()

        nodeA.transaction {
            val propertyStatesAndRef = nodeA.services.vaultService.queryBy<PropertyState>().states

            assertEquals(1, propertyStatesAndRef.size)

            val propertyState = propertyStatesAndRef.single().state.data
            assertEquals(listOf(nodeA.info.chooseIdentity()), propertyState.owners)
        }
    }

    @Test
    fun `Test selling property`() {
        val state = createProperty().single()
        sellProperty(nodeB.info.legalIdentities, state.linearId)

        nodeA.transaction {
            val propertyStatesAndRef = nodeA.services.vaultService.queryBy<PropertyState>().states

            assertEquals(1, propertyStatesAndRef.size)
            val state = propertyStatesAndRef.single().state.data

            assertEquals(state.owners, nodeB.info.legalIdentities)
        }
    }

    @Test
    fun `Test renting property`() {
        val state = createProperty().single()
        rentProperty(nodeB.info.legalIdentities, state.linearId)

        nodeA.transaction {
            val propertyStatesAndRef = nodeA.services.vaultService.queryBy<PropertyState>().states

            assertEquals(1, propertyStatesAndRef.size)
            val state = propertyStatesAndRef.single().state.data

            assertEquals(state.tenants, nodeB.info.legalIdentities)
        }
    }

    @Test
    fun `Test cancel renting property`() {
        val state = createProperty().single()
        cancelRentProperty(nodeB.info.legalIdentities, state.linearId)

        nodeA.transaction {
            val propertyStatesAndRef = nodeA.services.vaultService.queryBy<PropertyState>().states

            assertEquals(1, propertyStatesAndRef.size)
            val state = propertyStatesAndRef.single().state.data

            assertEquals(state.tenants, nodeB.info.legalIdentities)
        }
    }
}
