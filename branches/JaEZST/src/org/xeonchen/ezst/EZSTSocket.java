
package org.xeonchen.ezst;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import java.net.Socket;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class extends Sockets and provides EZST socket using EZST protocol.
 *
 * @author Liang-Heng Chen
 * @version 0.1.0, 03/25/07
 */
public class EZSTSocket extends Socket {
	private final String CRYPT = "AES";
	private final String DIGEST = "MD5";

	private boolean handshaked = false;

	private CipherInputStream is = null;
	private CipherOutputStream os = null;

	{ init(); }

	private void init() {
	}

	public static String toHex (byte[] buf) {
		StringBuilder strbuf = new StringBuilder(buf.length * 2);

		for (int i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10)
				strbuf.append("0");
			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}

		return strbuf.toString();
	}

	private static byte[] getDigest(String algorithm, byte[] input) throws java.security.NoSuchAlgorithmException {
		MessageDigest MD = MessageDigest.getInstance(algorithm);
		MD.update(input);
		return MD.digest();
	}

	/**
	 * Creates a EZST stream socket and connects it to the specified port number on the named host.
	 * 
	 * @param host the host name, or null for the loopback address.
	 * @param port the port number.
	 * @throws UnknownHostException if the IP address of the host could not be determined.
	 * @throws IOException if an I/O error occurs when creating the socket.
	 */
	public EZSTSocket(String host, int port) throws UnknownHostException, IOException {
		super(host, port);
	}

	/**
	 * Starts an EZST handshake on this connection.
	 * <p>
	 * You have to call this before {@link #getInputStream()} and {@link #getOutputStream()}.
	 *
	 * @throws IOException on a network level error.
	 * @throws UnknownServiceException if the service is unknown.
	 */
	public synchronized void startHandshake(String phrase) throws IOException, UnknownServiceException {
		if (handshaked)
			return;

		try {
			InputStream is = super.getInputStream();
			OutputStream os = super.getOutputStream();

			byte[] key = getDigest(DIGEST, phrase.getBytes());
			byte[] ident = getDigest(DIGEST, key);

			if(handshake(is, os, toHex(ident))) {
				Cipher enc = Cipher.getInstance(CRYPT);
				Cipher dec = Cipher.getInstance(CRYPT);

				enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, enc.getAlgorithm()));
				dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, dec.getAlgorithm()));

				this.is = new CipherInputStream(is, enc);
				this.os = new CipherOutputStream(os, dec);

				handshaked = true;
			} else
				throw new UnknownServiceException("Handshake failed");
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			throw e;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private boolean handshake(InputStream is, OutputStream os, String ident) throws IOException {
		/*
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
		
		if (!reader.ready() || !reader.readLine().equalsIgnoreCase("Bonjour"))
			return false;

		writer.write("LOGIN " + ident, 0, 7);
		writer.newLine();
		writer.flush();
		*/
		java.io.DataInputStream dis = new java.io.DataInputStream(is);
		java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

		dis.readUTF();
		dos.writeUTF(ident);
		dis.readUTF();

		return true;
	}

	/**
	 * Returns an input stream for this socket.
	 *
	 * @return an input stream for reading bytes from this socket.
	 * @throws IOException if an I/O error occurs when creating the input stream, the socket is closed, the socket is not connected, or the socket input has been shutdown using shutdownInput()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return is;
	}

	/**
	 * Returns an output stream for this socket.
	 *
	 * @return an output stream for writing bytes to this socket.
	 * @throws IOException if an I/O error occurs when creating the output stream or if the socket is not connected.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return os;
	}
}

