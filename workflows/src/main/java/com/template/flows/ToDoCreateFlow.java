package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.ToDoContract;
import com.template.states.ToDoState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class ToDoCreateFlow extends FlowLogic<Void> {
    private String taskDescription;

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public ToDoCreateFlow(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Initiator flow logic goes here.
        ServiceHub sh = this.getServiceHub();
        Party myIdentity = getOurIdentity();
        Date now = Date.from(Instant.now());
        ToDoState newState = new ToDoState(myIdentity, myIdentity, this.taskDescription, now);
        Party notary = sh.getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder tb = new TransactionBuilder(notary)
                .addOutputState(newState)
                .addCommand(new ToDoContract.Commands.CreateCommand(), newState.getParticipants().get(0).getOwningKey());
        SignedTransaction stx = sh.signInitialTransaction(tb);
        FlowSession assignedToSession = initiateFlow(myIdentity);
        subFlow(new FinalityFlow(stx, Arrays.asList(assignedToSession)));
        System.out.print("Newly created task ID: ");
        System.out.println(newState.getLinearId());
        return null;
    }
}
