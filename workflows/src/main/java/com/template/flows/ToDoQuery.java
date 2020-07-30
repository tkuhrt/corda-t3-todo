package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.ToDoState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class ToDoQuery extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Initiator flow logic goes here.
        Vault.Page results = getServiceHub().getVaultService().queryBy(ToDoState.class);

        List<StateAndRef> inputStatesAndRefs = results.getStates();

        List<ToDoState> states = inputStatesAndRefs.stream()
                .filter(elt -> elt != null)
                .map(elt -> (ToDoState)elt.getState().getData())
                .collect(Collectors.toList());

        states.forEach(state -> System.out.printf(" %s\t%s\t%s\n",
                state.getLinearId().toString(),
                state.getTaskDescription(),
                state.getAssignedTo().toString()));
        return null;
    }
}
