import com.fiesta.chatarchive.ChatArchivePlugin;
public class CommandTest {
 public static void main(String[] args) {
  String[] yes={"/msg friend hello","/essentials:epm friend hello","/email send friend hi","/teammsg hello"};
  String[] no={"/login example","/register example example","/email setpassword example","/unknown:msg friend hello"};
  for(String s:yes) if(!ChatArchivePlugin.chatCommand(s)) throw new AssertionError(s);
  for(String s:no) if(ChatArchivePlugin.chatCommand(s)) throw new AssertionError(s);
  System.out.println("Chat-command and authentication exclusion tests passed.");
 }
}
