package com.template.contracts;

import com.template.states.ToDoState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class ToDoContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.TemplateContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);

        if (cmd.getValue() instanceof Commands.CreateCommand) {
            requireThat(require -> {
                require.using("No input states required", tx.getInputs().isEmpty());
                require.using("One output state required", tx.getOutputs().size() == 1);
                require.using("Output state must be of type ToDoState", tx.outputsOfType(ToDoState.class).size() == 1);
                final ToDoState todo = (ToDoState) tx.getOutput(0);
                require.using("Description cannot be empty", todo.getTaskDescription() != null);
                require.using("Description length must be < 40", todo.getTaskDescription().length() < 40);
                return null;
            });
        }
        else if (cmd.getValue() instanceof Commands.AssignCommand) {
            requireThat(require -> {
                require.using("One input state is required", tx.getInputs().size() == 1);
                require.using("Input state must be of type ToDoState", tx.inputsOfType(ToDoState.class).size() == 1);
                require.using("One output state is required", tx.getOutputs().size() == 1);
                require.using("Output state must be of type ToDoState", tx.outputsOfType(ToDoState.class).size() == 1);
                final ToDoState in = (ToDoState) tx.getInput(0);
                final ToDoState out = (ToDoState) tx.getOutput(0);
                final ToDoState checkNew = out.withNewAssignedTo(in.getassignedTo());
                require.using("Only the assignedTo can change",
                        (checkNew.getAssignedBy().equals(out.getAssignedBy()) &&
                         checkNew.getTaskDescription().equals(out.getTaskDescription()) &&
                         checkNew.getDateOfCreation().equals(out.getDateOfCreation())));
                require.using("The assignedTo property must change in an assignment.",
                        !out.getassignedTo().getOwningKey().equals(in.getassignedTo().getOwningKey()));
                require.using("All participants must be signers",
                        cmd.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                return null;
            });
        }
        else {
            throw new IllegalArgumentException("Unrecognised command");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class CreateCommand implements Commands {}
        class AssignCommand implements Commands {}
    }
}