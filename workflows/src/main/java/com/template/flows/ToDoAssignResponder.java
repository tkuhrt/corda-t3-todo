package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.ToDoState;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(ToDoAssignInitiator.class)
public class ToDoAssignResponder extends FlowLogic<Void> {
    private final FlowSession otherPartyFlow;
    private SecureHash txWeJustSignedId;

    public ToDoAssignResponder(FlowSession otherPartyFlow) {
        this.otherPartyFlow = otherPartyFlow;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                super(otherPartyFlow, progressTracker);
            }

            @Override
            protected void checkTransaction(SignedTransaction stx) {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be an ToDo transaction", output instanceof ToDoState);
                    return null;
                });
                // Once the transaction has verified, initialize txWeJustSignedID variable.
                txWeJustSignedId = stx.getId();
            }
        }

        // Create a sign transaction flow
        SignTxFlow signTxFlow = new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker());

        // Run the sign transaction flow to sign the transaction
        subFlow(signTxFlow);

        // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
        subFlow(new ReceiveFinalityFlow(otherPartyFlow, txWeJustSignedId));
        return null;
    }
}
