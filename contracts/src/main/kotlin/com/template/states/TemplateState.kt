package com.template.states

import com.template.contracts.PropertyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import java.time.Instant

// *********
// * State *
// *********
@BelongsToContract(PropertyContract::class)
data class PropertyState(
    val owners: List<Party>,
    val tenants: List<Party>,
    val constructedAt: Instant,
    val area: Int,
    val address: String,
    override val participants: List<Party> = owners
) : ContractState
