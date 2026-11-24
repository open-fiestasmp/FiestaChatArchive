import com.fiesta.chatarchive.ChatArchivePlugin;
import java.nio.file.Path;
public class DurabilityTest {
 public static void main(String[] a) throws Exception {
  if(!ChatArchivePlugin.chatCommand("/agyreply player hello")) throw new AssertionError("AGY reply");
  if(!ChatArchivePlugin.chatCommand("/essentials:pm player hello")) throw new AssertionError("PM");
  if(ChatArchivePlugin.chatCommand("/email setpassword example")) throw new AssertionError("password command");
  ChatArchivePlugin.durableAppend(Path.of(a[0]),"durable test");
  Runtime.getRuntime().halt(0);
 }
}
