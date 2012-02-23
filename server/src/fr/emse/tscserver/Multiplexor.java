package fr.emse.tscserver;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class Multiplexor  {
	private Vector<Chunk> queue = new Vector<Chunk>();
	
	public void addSocket( Socket socket ) {
		new ThreadMultiplexor( this, socket );
	}
	
	// Return a Chunk that has already been read from a socket.
	// Note that we synchronize on queue to make sure that we
	// don't mess things up while adding and removing.
	public Chunk read() {
		synchronized( queue ) {
			// Wait for there to be an available Chunk
			while (true) {
				if (queue.isEmpty()) {
					try {
						queue.wait();
					} catch( InterruptedException ie ) {}
				} else {
					break;
				}
			}

			// Okay, there's a chunk!  Remove it from the queue, and
			// return it to the caller
			Chunk chunk = queue.elementAt( 0 );
			queue.removeElementAt( 0 );
			return chunk;
		}
	}

	// Place a newly-read Chunk onto the queue, to make it available
	// to users of this class. The subclass should call this method
	// when it has read a Chunk from a socket.
	protected void addChunk( Chunk chunk ) {
		synchronized( queue ) {
			queue.addElement( chunk );
			queue.notifyAll();
		}
	}
	
}

class ThreadMultiplexor implements Runnable {
	// A queue used to hold Chunks that have been grabbed from sockets.
	// When .read() is called, the oldest chunk from this list is returned
	
	Multiplexor tm;
	
	// The socket we are reading from
	Socket socket;

	// Thread for doing block reads in
	Thread thread;

	// Buffer for reading data from the socket
	static private final int BUFFERSIZE=16384;
	byte buffer[] = new byte[BUFFERSIZE];

	// Start a background thread to do the reading
	public ThreadMultiplexor( Multiplexor tm, Socket socket ) {
		this.tm = tm;
	    this.socket = socket;
	    thread = new Thread( this );
	    thread.start();
	  }

	// This is run in the background thread
	public void run() {
		try {
			InputStream in = socket.getInputStream();

			// Repeatedly read data from the client
			while (true) {
				int bytesRead = in.read( buffer );
				if (bytesRead<0) {
					throw new EOFException();
				}

				// Copy the new data into a buffer that's just large
				// enough, make a new Chunk, and put it on the queue
				byte nbuffer[] = new byte[bytesRead];
				System.arraycopy( buffer, 0, nbuffer, 0, bytesRead );
				Chunk chunk = new Chunk( socket, nbuffer );
				tm.addChunk( chunk );
			}
			// Deal with broken connection, if any
		} catch( IOException ie ) {
			System.out.println( "Closing: "+thread );
		} finally {
			try {
				socket.close();
			} catch( Exception e ) { System.out.println( e ); }
		}
	}

}

