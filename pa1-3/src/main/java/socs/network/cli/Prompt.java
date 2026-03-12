package socs.network.cli;

@FunctionalInterface
public interface Prompt {
  boolean callBack(String input);
}
