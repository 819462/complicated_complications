/*
 * Just so I don't get confused later on, here's what I learned from this because everything here is NEW :)))
 *
 * PrintWriter is Java's tool to write text to a stream. 
 * Other comments in the actual code itself to make more sense (with context)
 */

import java.io.*;

public class WriteOut
{
  private PrintWriter writer;

  public WriteOut(OutputStream out)
  {
    writer = new PrintWriter(new OutputStreamWriter(out), true); // true means to auto-flush, so the game wouldn't sit there endlessly! -_- boy.
  }

  public void send(String input) // GUICentral.java's calling method!
  {
    writer.println(input);
  }
}
