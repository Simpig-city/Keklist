package de.hdg.keklist.commands.type;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

public interface BrigadierCommand {

    @NotNull LiteralCommandNode<CommandSourceStack> getCommand();

        // Does not work for arguments => need to register .executes() self
    default LiteralArgumentBuilder<CommandSourceStack> addExecutes(@NotNull LiteralArgumentBuilder<CommandSourceStack> node, @NotNull Command<CommandSourceStack> command) {
        // Add a default executes() to the current command
        node.executes(command);

        // Traverse through child nodes if they exist
        for (CommandNode<CommandSourceStack> child : node.getArguments()) {
            if (child instanceof LiteralCommandNode literalNode) {
                // Rebuild the child node with the execute method
                LiteralArgumentBuilder<CommandSourceStack> childBuilder = Commands.literal(literalNode.getLiteral());
                addExecutes(childBuilder, command);
                node.then(childBuilder);
            }
        }

        return node;
    }

    default LiteralArgumentBuilder<CommandSourceStack> addExecutesLast(@NotNull LiteralArgumentBuilder<CommandSourceStack> node, @NotNull Command<CommandSourceStack> command) {
        // If there are no child arguments, add the execute handler.
        if (node.getArguments().isEmpty()) {
            node.executes(command);
        } else {
            // Otherwise, traverse through child nodes and rebuild them.
            for (CommandNode<CommandSourceStack> child : node.getArguments()) {
                if (child instanceof LiteralCommandNode literalNode) {
                    LiteralArgumentBuilder<CommandSourceStack> childBuilder = Commands.literal(literalNode.getLiteral());
                    addExecutes(childBuilder, command);
                    node.then(childBuilder);
                }
            }
        }
        return node;
    }

}
