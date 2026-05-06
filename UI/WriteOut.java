import java.io.*;

public class WriteOut
{
  private PrintWriter writer;

  public WriteOut(OutputStream out)
  {
    writer = new PrintWriter(new OutputStreamWriter(out), true);
  }

  public void send(String input)
  {
    writer.println(input);
  }
}
