//
//  jezstServer.java
//  jezst
//
//  Created by Chen, Liang-heng on 2006/09/19.
//  Copyright 2006 TFCIS. All rights reserved.
//

import java.io.*;
import java.net.*;
import java.util.*;

public class jezstServer extends Thread {
	public static final int defaultPort = 3257;
	public static final int maxConnections = 128;
	protected ServerSocket serverSocket;
	protected boolean listening = true;
	protected PrintWriter logStream;
	protected ConnectionManager connectionManager;
	protected ThreadGroup threadGroup;
	private HashMap<String, Socket> pool = null;

	public jezstServer(OutputStream os, int port) {
		super("jezstServer");
		setLogStream(os);

		try {
			serverSocket = new ServerSocket(port);
			// serverSocket.setSoTimeout(120000);
		} catch (IOException e) {
			log("Can't listen on port " + port);
			System.exit(-1);
		}

		threadGroup = new ThreadGroup("ezst");
		connectionManager = new ConnectionManager(threadGroup, maxConnections);
		connectionManager.start();

		pool = new HashMap<String, Socket>();

		log("EZST Server is listening on port " + port);
	}

	protected void setLogStream(OutputStream os) {
		if (os != null)
			logStream = new PrintWriter(new OutputStreamWriter(os));
		else
			logStream = null;
	}

	protected synchronized void log(String msg) {
		if (logStream != null) {
			logStream.println("[" + new Date() + "] " + msg);
			logStream.flush();
		}
	}

	protected void log(Object o) {
		log(o.toString());
	}

	public void run() {
		while (listening) {
			try {
				connectionManager.addConnection(serverSocket.accept());
			} catch (IOException e) {
				log(e);
			}
		}
	}

	public class ConnectionManager extends Thread {
		int maxConnections;
		Vector<jezstServerThread> connections;

		public ConnectionManager(ThreadGroup group, int maxConnections) {
			super(group, "ConnectionManager");
			this.setDaemon(true);
			this.maxConnections = maxConnections;
			connections = new Vector<jezstServerThread>(maxConnections);
			log("Starting ConnectionManager with maximum " + maxConnections + " connections");
		}

		public synchronized void addConnection(Socket s) {
			try {
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				if (connections.size() >= maxConnections) {
					dos.writeUTF("Server is full.");
					dos.flush();

					s.close();
					log("Refuse " + s.getInetAddress().getHostAddress() + ": max connections reached.");
				} else {
					dos.writeUTF("Welcome!");
					dos.flush();

					jezstServerThread c = new jezstServerThread(s);
					connections.addElement(c);
					log(s.toString() + " connected. (" + connections.size() + ")");
					c.start();
				}
			} catch (Exception e) {
				log(e);
			}
		}

		public synchronized void endConnection() {
			this.notify();
		}

		public void run() {
			while (true) {
				for (int i = 0; i < connections.size(); i++) {
					jezstServerThread c = (jezstServerThread)connections.elementAt(i);
					if (!c.isAlive()) {
						connections.removeElementAt(i);
						log(c.getName() + " disconnected. (" + connections.size() + ")");
					}
				}
				try {
					synchronized(this) {
						this.wait();
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public class jezstServerThread extends Thread {
		private Socket sck = null;
		private String ident;

		public jezstServerThread(Socket sck) {
			super(sck.toString());
			this.sck = sck;
		}

		public void run() {
			try {
				String ident = getIdent();
				if (addClient(ident, sck) == 1) {
					log("Tunnel of " + ident + " is created.");
				} else {
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private String getIdent() throws IOException {
			return (ident = new DataInputStream(sck.getInputStream()).readUTF());
		}

		public synchronized int addClient(String phrase, Socket sck1) {
			log("Phrase: " + phrase);

			if (pool.containsKey(phrase)) {
				Socket sck2 = (Socket)pool.get(phrase);
				pool.remove(phrase);

				if (!sck2.isOutputShutdown()) {
					try {
						new DataOutputStream(sck2.getOutputStream()).writeUTF("Connected! with [" + sck1.getInetAddress().getHostAddress() + "] (2)");
						new DataOutputStream(sck1.getOutputStream()).writeUTF("Connected! with [" + sck2.getInetAddress().getHostAddress() + "] (1)");
						new jezstServerHandler(sck1, sck2).start();
						return 1;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			pool.put(phrase, sck1);
			return 0;
		}
	}

	public class jezstServerHandler extends Thread {
		private Socket sck1, sck2;

		public jezstServerHandler(Socket sck1, Socket sck2) {
			super("jezstServerHandler");
			this.sck1 = sck1;
			this.sck2 = sck2;
		}

		public void run() {
			try {
				jezstServerBridger th1 = new jezstServerBridger(sck1, sck1.getInputStream(), sck2.getOutputStream());
				jezstServerBridger th2 = new jezstServerBridger(sck2, sck2.getInputStream(), sck1.getOutputStream());

				th1.start();
				th2.start();

				th1.join();
				th2.join();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				connectionManager.endConnection();
			}
		}
	}

	public class jezstServerBridger extends Thread {
		private static final int BUFSIZ = 4096;
		private Socket sck;
		private InputStream is;
		private OutputStream os;

		public jezstServerBridger(Socket sck, InputStream is, OutputStream os) {
			super("jezstServerBridger");
			this.sck = sck;
			this.is = new BufferedInputStream(is);
			this.os = new BufferedOutputStream(os);
		}

		public void run() {
			byte[] buf = new byte[BUFSIZ];
			int len;

			try {
				try {
					try {
						try {
							while ((len = is.read(buf)) != -1) {
								os.write(buf, 0, len);
							}
							os.flush();
						} catch (Exception e) {
							// e.printStackTrace();
							log("Interrupt");
						} finally {
							os.close();
						}
					} catch (Exception e) {
						log("Error closing Receiver");
					} finally {
						is.close();
					}
				} catch (Exception e) {
					log("Error closing Sender");
				} finally {
					sck.close();
				}
			} catch (Exception e) {
				log("Error closing Socket");
			} finally {
				connectionManager.endConnection();
			}
		}
	}

	public static void main(String[] args) {
		try {
			new jezstServer(new FileOutputStream("jezstServer.log"), defaultPort).run();
		} catch (FileNotFoundException e) {
		}
	}
}

