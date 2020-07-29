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

import java.util.Collections;
import java.util.Date;
import java.util.Set;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class ToDoCreateFlow extends FlowLogic<Void> {
    private String taskDescription;

    private final ProgressTracker progressTracker = new ProgressTracker(

    );

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
        Date now = new Date();
        ToDoState newState = new ToDoState(myIdentity, myIdentity, this.taskDescription, now);
        System.out.println(newState.toString());
        Party notary = sh.getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder tb = new TransactionBuilder(notary)
                .addOutputState(newState)
                .addCommand(new ToDoContract.Commands.CreateCommand(), myIdentity.getOwningKey());
        SignedTransaction stx = sh.signInitialTransaction(tb);
        Set<FlowSession> emptySet = Collections.emptySet();
        subFlow(new FinalityFlow(stx, emptySet));
        return null;
    }
}
