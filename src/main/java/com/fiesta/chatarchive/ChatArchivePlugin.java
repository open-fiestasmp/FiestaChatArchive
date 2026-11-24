package com.fiesta.chatarchive;

import com.google.gson.Gson;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.ess3.api.events.PrivateMessageSentEvent;
import net.essentialsx.api.v2.events.HelpopMessageSendEvent;
import net.essentialsx.api.v2.events.UserMailEvent;
import net.essentialsx.api.v2.events.chat.GlobalChatEvent;
import net.essentialsx.api.v2.events.chat.LocalChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class ChatArchivePlugin extends JavaPlugin implements Listener {
    private static final Set<String> COMMANDS = Set.of("tell","msg","w","whisper","r","reply","m","t","pm","emsg","epm","etell","ewhisper","er","ereply","mail","email","eemail","memo","ememo","helpop","ehelpop","ac","eac","amsg","eamsg","me","eme","action","eaction","describe","edescribe","teammsg","tm","agyreply");
    private Path file;
    private final Object writeLock = new Object();
    private final Gson gson = new Gson();

    public static boolean chatCommand(String text) {
        if (text == null || !text.startsWith("/")) return false;
        String[] bits = text.substring(1).split("\\s+", 3);
        String label = bits[0].toLowerCase(Locale.ROOT);
        if (label.contains(":")) {
            String[] namespace = label.split(":",2);
            if (!Set.of("essentials","minecraft","agybridge").contains(namespace[0])) return false;
            label = namespace[1];
        }
        if (!COMMANDS.contains(label)) return false;
        if (label.equals("email") || label.equals("eemail")) {
            return bits.length > 1 && Set.of("send","sendtemp","sendall").contains(bits[1].toLowerCase(Locale.ROOT));
        }
        return true;
    }
    @Override public void onEnable() {
        try {
            Files.createDirectories(getDataFolder().toPath());
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not create the chat archive directory", error);
        }
        file = getDataFolder().toPath().resolve("chat-events.jsonl");
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("Synchronous chat event archival enabled; direct messages from other plugins may require integration.");
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onChat(AsyncChatEvent e) {
        record("player_chat",e.getPlayer().getName(),null,PlainTextComponentSerializer.plainText().serialize(e.message()));
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (chatCommand(e.getMessage())) record("chat_command",e.getPlayer().getName(),null,e.getMessage());
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onConsoleCommand(ServerCommandEvent e) {
        if (chatCommand("/"+e.getCommand())) record("console_chat_command",e.getSender().getName(),null,e.getCommand());
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBroadcast(BroadcastMessageEvent e) {
        record("broadcast","server_or_plugin",null,PlainTextComponentSerializer.plainText().serialize(e.message()));
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPrivate(PrivateMessageSentEvent e) {
        if (e.getResponse().isSuccess()) record("essentials_private",e.getSender().getName(),e.getRecipient().getName(),e.getMessage());
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onMail(UserMailEvent e) {
        record("essentials_mail",e.getMessage().getSenderUsername(),e.getRecipient().getName(),e.getMessage().getMessage());
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onHelp(HelpopMessageSendEvent e) {
        record("essentials_helpop",e.getSender().getName(),null,e.getMessage());
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onLocal(LocalChatEvent e) { record("essentials_local_chat",e.getPlayer().getName(),null,e.getMessage()); }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onGlobal(GlobalChatEvent e) { record("essentials_global_chat",e.getPlayer().getName(),null,e.getMessage()); }

    public static void durableAppend(Path path, String line) throws java.io.IOException {
        byte[] data=(line+"\n").getBytes(StandardCharsets.UTF_8);
        try(FileChannel file=FileChannel.open(path,StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.APPEND)) {
            ByteBuffer buf=ByteBuffer.wrap(data);
            while(buf.hasRemaining()) file.write(buf);
            file.force(true);
        }
    }
    private void record(String type,String sender,String recipient,String message) {
        Map<String,String> item=new LinkedHashMap<>();
        item.put("time_utc",Instant.now().toString());item.put("type",type);item.put("sender",sender);
        if(recipient!=null)item.put("recipient",recipient);
        item.put("message",message);
        synchronized(writeLock) {
            try { durableAppend(file,gson.toJson(item)); }
            catch(Exception ex) { getLogger().log(Level.SEVERE,"Chat archive write failed; event was not preserved",ex); }
        }
    }
}
