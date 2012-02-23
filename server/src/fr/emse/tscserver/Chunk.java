package fr.emse.tscserver;

import java.net.*;

public class Chunk {
  // The data
  public byte data[];
  // The socket this data came from
  public Socket socket;

  // Store the bit of data
  public Chunk( Socket socket, byte data[] ) {
    this.data = data;
    this.socket = socket;
  }

  // An informative string, for debugging
  public String toString() {
    return "Chunk "+super.toString()+": "+data.length+" bytes, from "+
      socket;
  }
}
