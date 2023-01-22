package de.hdg.keklist.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WhereAmICommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if(invocation.source() instanceof Player player){
            player.sendMessage(Component.text("Knowbody really knows? Maybe on the main server, maybe not. I hope you enjoy this server! Maybe just build something here."));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of("knowbody", "know", "that", "exactly"));
    }
}
