package com.template.contracts;

import com.template.states.ToDoState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

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
                require.using("No input states expected", tx.getInputs().size() != 0);
                require.using("One output state expected", tx.getOutputs().size() == 1);
                ToDoState todo = (ToDoState) tx.getOutputStates().get(0);
                require.using("Description cannot be empty", todo.getTaskDescription().isEmpty());
                require.using("Description length must be < 40", todo.getTaskDescription().length() < 40);
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
    }
}