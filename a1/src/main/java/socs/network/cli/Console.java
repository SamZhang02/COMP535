package socs.network.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * Single source of truth for handling all console UI.
 * It can take a "prompt" which interrupts the current user flow to display a message and ask for an input.
 */
public class Console implements Runnable {

  private final BufferedReader reader =
          new BufferedReader(new InputStreamReader(System.in));

  private final BlockingQueue<String> commandQueue =
          new LinkedBlockingQueue<>();

  private volatile boolean running = true;
  private volatile Prompt pendingPrompt = null;
  private Thread consoleThread = null;

  public synchronized Thread start() {
    if (consoleThread != null && consoleThread.isAlive()) {
      return consoleThread;
    }

    running = true;
    consoleThread = new Thread(this, "console-thread");
    consoleThread.setDaemon(true);
    consoleThread.start();

    return consoleThread;
  }

  public BlockingQueue<String> getCommandQueue() {
    return commandQueue;
  }

  /**
   * Prioritize using Console.log instead of the usual SOUT;
   * Console.log clears the console beforehand so the CLI's UI remains not-messy.
   */
  public synchronized void log(String msg) {
    this.log(msg, true);
  }

  public synchronized void log(String msg, boolean redraw) {
    System.out.print("\r");
    System.out.print("\033[K");

    System.out.println(msg);

    if (!redraw) {
      return;
    }

    if (pendingPrompt == null) {
      System.out.print(">> ");
    } else {
      System.out.print("$ ");
    }
  }


  public boolean hasPrompt() {
    return pendingPrompt != null;
  }

  public CompletableFuture<String> requestPromptAsync(String message) {
    CompletableFuture<String> future = new CompletableFuture<>();

    synchronized (this) {
      if (pendingPrompt != null)
        throw new IllegalStateException("Prompt already active");

      this.log(message, false);
      System.out.print("$ ");

      pendingPrompt = input -> {
        future.complete(input);
        return true;
      };
    }
    return future;
  }

  public void stop() {
    running = false;
    Thread t = consoleThread;
    if (t != null) {
      t.interrupt();
    }
  }


  @Override
  public void run() {
    while (running) {
      System.out.print(">> ");
      try {
        String line = reader.readLine();
        if (line == null) break; // stdin closed

        Prompt prompt;
        synchronized (this) {
          prompt = pendingPrompt;
        }

        if (prompt != null) {
          boolean done = prompt.callBack(line);
          if (done) {
            synchronized (this) {
              pendingPrompt = null;
            }
          }
        } else {
          commandQueue.put(line);
        }

      } catch (Exception e) {
        e.printStackTrace();
        break;
      }
    }
  }
}
