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

  public BlockingQueue<String> getCommandQueue() {
    return commandQueue;
  }

  public void println(String s) {
    System.out.println(s);
  }

  public void print(String s){
    System.out.print(s);
  }

  public boolean hasPrompt() {
    return pendingPrompt != null;
  }

  public CompletableFuture<String> requestPromptAsync(String message) {
    CompletableFuture<String> future = new CompletableFuture<>();

    synchronized (this) {
      if (pendingPrompt != null)
        throw new IllegalStateException("Prompt already active");

      System.out.println(message);
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
  }

  @Override
  public void run() {
    while (running) {
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
