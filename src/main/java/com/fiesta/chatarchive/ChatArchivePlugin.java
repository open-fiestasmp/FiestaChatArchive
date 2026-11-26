package com.fiesta.chatarchive;

import com.google.gson.Gson;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class ChatArchivePlugin extends JavaPlugin implements Listener {
    private static final Set<String> COMMANDS = Set.of("tell","msg","w","whisper","r","reply","m","t","pm","emsg","epm","etell","ewhisper","er","ereply","mail","email","eemail","memo","ememo","helpop","ehelpop","ac","eac","amsg","eamsg","me","eme","action","eaction","describe","edescribe","teammsg","tm","agyreply");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);
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
        saveDefaultConfig();
        Path configured = Path.of(getConfig().getString("archive-file", "chat-events.jsonl"));
        file = (configured.isAbsolute() ? configured : getDataFolder().toPath().resolve(configured)).normalize();
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            if (Files.notExists(file)) createArchiveFile(file);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not create the chat archive file", error);
        }
        getServer().getPluginManager().registerEvents(this,this);
        if (getServer().getPluginManager().isPluginEnabled("Essentials")) {
            try {
                Class<?> type = Class.forName("com.fiesta.chatarchive.EssentialsArchiveListener", true, getClass().getClassLoader());
                Listener listener = (Listener) type.getConstructor(ChatArchivePlugin.class).newInstance(this);
                getServer().getPluginManager().registerEvents(listener, this);
                getLogger().info("Essentials private-message, mail, and helpop archival enabled.");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Could not load Essentials chat archive integration", error);
            }
        }
        getLogger().info("Durable Paper chat and command archival enabled: " + file.getFileName());
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
    public static void durableAppend(Path path, String line) throws java.io.IOException {
        byte[] data=(line+"\n").getBytes(StandardCharsets.UTF_8);
        try(FileChannel file=FileChannel.open(path,StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.APPEND)) {
            ByteBuffer buf=ByteBuffer.wrap(data);
            while(buf.hasRemaining()) file.write(buf);
            file.force(true);
        }
    }
    private static void createArchiveFile(Path path) throws java.io.IOException {
        try {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
        } catch (UnsupportedOperationException error) {
            Files.createFile(path);
        }
    }
    void record(String type,String sender,String recipient,String message) {
        Map<String,String> item=new LinkedHashMap<>();
        item.put("time_utc",Instant.now().toString());item.put("type",type);item.put("sender",sender);
        if(recipient!=null)item.put("recipient",recipient);
        item.put("message",message);
        synchronized(writeLock) {
            try { durableAppend(file,gson.toJson(item)); }
            catch(java.io.IOException ex) { getLogger().log(Level.SEVERE,"Chat archive write failed; event was not preserved",ex); }
        }
    }
}
