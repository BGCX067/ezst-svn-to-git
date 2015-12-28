
package org.xeonchen.ezst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.security.MessageDigest;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EZSTFileOutputStream extends OutputStream implements EZSTProgressEvent, Runnable {
	private ZipInputStream in = null;
	private OutputStream out = null;

	private String baseDir = null;
	private Vector<Long> pool = null;
	private IOException exception = null;

	private int fileCount = 0;
	private long filePosition = 0;
	private long fileSize = 0;
	private long fileTotalPosition = 0;
	private long fileTotalSize = 0;

	{ init(); }

	public EZSTFileOutputStream(File path) throws IOException {
		path.mkdirs();
		setBaseDir(path.getAbsolutePath());

		processFiles();
	}

	public EZSTFileOutputStream(String pathname) throws IOException {
		this(new File(pathname));
	}

	public void close() throws IOException {
		if (out != null)
			out.close();
	}

	public void flush() throws IOException {
		if (out != null)
			out.flush();
	}

	public String getBaseDir() {
		return baseDir;
	}

	public int getCount() {
		return fileCount;
	}

	private String getFileName(String name) {
		StringBuilder str = new StringBuilder(name);

		for (int i = 0; i < str.length(); i++)
			if (str.charAt(i) == '/')
				str.setCharAt(i, File.separatorChar);

		return str.toString();
	}

	public long getPosition() {
		return filePosition;
	}

	public long getSize() {
		return fileSize;
	}

	public long getTotalPosition() {
		return fileTotalPosition;
	}

	public long getTotalSize() {
		return fileTotalSize;
	}

	private void init() throws IOException {
		pool = new Vector<Long>();

		makePipe();
	}

	private void makePipe() throws IOException {
		PipedInputStream is = new PipedInputStream();
		PipedOutputStream os = new PipedOutputStream(is);
		in = new ZipInputStream(new BufferedInputStream(is));
		out = new BufferedOutputStream(os);
	}

	private void processFiles() throws IOException {
		(new Thread(this)).start();
	}

	private void recvFile(String filename) throws IOException {
		File file = new File(getBaseDir() + File.separatorChar + filename);
		file.getParentFile().mkdirs();

		System.out.println("Writing: " + file.getAbsolutePath());
		setPosition(0);
		setSize(pool.get(0));
		pool.remove(0);

		System.out.println(getSize());

		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));

		// Calculate Message Digest
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");

			int len;
			byte[] buf = new byte[1024];
			while ((len = in.read(buf)) != -1) {
				os.write(buf, 0, len);
				md.update(buf, 0, len);

				setPosition(getPosition() + len);
				setTotalPosition(getTotalPosition() + len);
			}

			// System.out.println("Digest: " + EZSTSocket.toHex(md.digest()));
			// System.out.println("-Position: " + getPosition());
		} catch (java.security.NoSuchAlgorithmException e) {}

		os.close();
		in.closeEntry();
	}

	private void recvHeader() {
		try {
			DataInputStream is = new DataInputStream(in);

			setCount(is.readInt());
			setTotalSize(is.readLong());

			for (int i = 0; i < getCount(); ++i)
				pool.add(is.readLong());
		} catch (IOException e) {
		}
	}

	public void run() {
		try {
			ZipEntry ze = null;

			if ((ze = in.getNextEntry()) != null)
				recvHeader();

			while ((ze = in.getNextEntry()) != null)
				recvFile(getFileName(ze.getName()));

		} catch (IOException e) {
			exception = e;
		}
	}

	private void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	private void setCount(int count) {
		fileCount = count;
	}

	private void setPosition(long position) {
		filePosition = position;
	}

	private void setSize(long size) {
		fileSize = size;
	}

	private void setTotalPosition(long position) {
		fileTotalPosition = position;
	}

	private void setTotalSize(long size) {
		fileTotalSize = size;
	}

	public void write(int b) throws IOException {
		if (exception != null)
			throw exception;
		out.write(b);
	}
}

