package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.ToDoContract;
import com.template.states.ToDoState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class ToDoAssignInitiator extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final UniqueIdentifier stateLinearId;
    private final Party assignedTo;

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public ToDoAssignInitiator(UniqueIdentifier stateLinearId, Party assignedTo) {
        this.stateLinearId = stateLinearId;
        this.assignedTo = assignedTo;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // 1. Retrieve the ToDoState from the vault using LinearStateQueryCriteria
        List<UUID> listOfLinearIds = new ArrayList<>();
        listOfLinearIds.add(stateLinearId.getId());
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);

        // 2. Get a reference to the inputState data that we are going to settle.
        Vault.Page results = getServiceHub().getVaultService().queryBy(ToDoState.class, queryCriteria);
        StateAndRef inputStateAndRefToTransfer = (StateAndRef) results.getStates().get(0);
        ToDoState inputStateToTransfer = (ToDoState) inputStateAndRefToTransfer.getState().getData();

        // 3. We should now get some of the components required for to execute the transaction
        // Here we get a reference to the default notary and instantiate a transaction builder.

        // Obtain a reference to a notary we wish to use.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1

        TransactionBuilder tb = new TransactionBuilder(notary);

        // 4. Construct a Assign command to be added to the transaction.
        List<PublicKey> listOfRequiredSigners = inputStateToTransfer.getParticipants()
                .stream().map(AbstractParty::getOwningKey)
                .collect(Collectors.toList());
        listOfRequiredSigners.add(assignedTo.getOwningKey());

        Command<ToDoContract.Commands.AssignCommand> command = new Command<>(
                new ToDoContract.Commands.AssignCommand(),
                listOfRequiredSigners
        );

        // 5. Add the command to the transaction using the TransactionBuilder.
        tb.addCommand(command);

        // 6. Add input and output states to flow using the TransactionBuilder.
        tb.addInputState(inputStateAndRefToTransfer);
        tb.addOutputState(inputStateToTransfer.withNewAssignedTo(assignedTo), ToDoContract.ID);

        // 7. Ensure that this flow is being executed by the current assignedTo.
        if (!inputStateToTransfer.getassignedTo().getOwningKey().equals(getOurIdentity().getOwningKey())) {
            throw new IllegalArgumentException("This flow must be run by the current lender.");
        }

        // 8. Verify and sign the transaction
        tb.verify(getServiceHub());
        SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(tb);

        // 9. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
        List<FlowSession> sessions = new ArrayList<>();

        for (AbstractParty participant: inputStateToTransfer.getParticipants()) {
            Party partyToInitiateFlow = (Party) participant;
            if (!partyToInitiateFlow.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                sessions.add(initiateFlow(partyToInitiateFlow));
            }
        }
        sessions.add(initiateFlow(assignedTo));
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, sessions));
        /* 10. Return the output of the FinalityFlow which sends the transaction to the notary for verification
         *     and the causes it to be persisted to the vault of appropriate nodes.
         */
        subFlow(new FinalityFlow(fullySignedTransaction, sessions));
        return null;
    }
}
