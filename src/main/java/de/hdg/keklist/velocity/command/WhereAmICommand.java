package de.hdg.keklist.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.hdg.keklist.velocity.KeklistVelocity;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WhereAmICommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if(invocation.source() instanceof Player player){
            player.sendMessage(Component.text(KeklistVelocity.getTranslations().get("limbo.where-am-i")));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of("nobody", "knows", "that", "exactly"));
    }
}
