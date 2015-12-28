
package org.xeonchen.ezst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.security.MessageDigest;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EZSTFileInputStream extends InputStream implements EZSTProgressEvent, Runnable {
	private InputStream in = null;
	private ZipOutputStream out = null;

	private Vector<EZSTFileEntryInputStream> pool = null;
	private IOException exception = null;

	private int fileCount = 0;
	private long filePosition = 0;
	private long fileSize = 0;
	private long fileTotalPosition = 0;
	private long fileTotalSize = 0;

	{ init(); }

	public EZSTFileInputStream(File[] files) throws FileNotFoundException, IOException {
		setCount(addFiles(files));

		processFiles();
	}

	public EZSTFileInputStream(String[] names) throws FileNotFoundException, IOException {
		setCount(addFiles(names));

		processFiles();
	}

	private int addFile(File file, String path) throws IOException {
		if (!file.canRead())
			throw new IOException("Can't read file: " + file.getAbsolutePath());

		if (file.isDirectory()) {
			return addFiles(file.listFiles(), path + '/' + file.getName());
		} else if (file.isFile()) {
			pool.add(new EZSTFileEntryInputStream(file, path));
			setTotalSize(getTotalSize() + file.length());
			return 1;
		}

		return 0;
	}

	private int addFiles(File[] files) throws IOException {
		return addFiles(files, ".");
	}

	private int addFiles(File[] files, String path) throws IOException {
		int count = 0;

		for (File file : files)
			count += addFile(file, path);

		return count;
	}

	private int addFiles(String[] names) throws IOException {
		return addFiles(names, ".");
	}

	private int addFiles(String[] names, String path) throws IOException {
		int count = 0;

		for (String name : names)
			count += addFile(new File(name), path);

		return count;
	}

	public void close() throws IOException {
		if (in != null)
			in.close();
	}

	public int getCount() {
		return fileCount;
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
		pool = new Vector<EZSTFileEntryInputStream>();

		makePipe();
	}

	private void makePipe() throws IOException {
		PipedInputStream is = new PipedInputStream();
		PipedOutputStream os = new PipedOutputStream(is);
		in = new BufferedInputStream(is);
		out = new ZipOutputStream(new BufferedOutputStream(os));
		out.setLevel(0);
	}

	private void processFiles() throws IOException {
		(new Thread(this)).start();
	}

	public int read() throws IOException {
		if (exception != null)
			throw exception;
		return in.read();
	}

	public void run() {
		try {
			sendHeader();
			for (EZSTFileEntryInputStream is : pool)
				sendFile(is);

			out.flush();
			out.close();
		} catch (IOException e) {
			exception = e;
		}
	}

	private void sendFile(EZSTFileEntryInputStream is) throws IOException {
		File file = is.getFile();

		System.out.println("Reading: " + file.getAbsolutePath());
		setPosition(0);
		setSize(file.length());

		out.putNextEntry(new ZipEntry((is.getPath() + '/' + file.getName()).substring(2)));

		// Calculate Message Digest
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");

			int len;
			byte[] buf = new byte[1024];
			while ((len = is.read(buf)) != -1) {
				out.write(buf, 0, len);
				out.flush();
				md.update(buf, 0, len);

				setPosition(getPosition() + len);
				setTotalPosition(getTotalPosition() + len);
			}

			// System.out.println("Digest: " + EZSTSocket.toHex(md.digest()));
			// System.out.println("+Position: " + getPosition());
		} catch (java.security.NoSuchAlgorithmException e) {}

		is.close();
		out.closeEntry();
	}

	private void sendHeader() throws IOException {
		DataOutputStream os = new DataOutputStream(out);

		out.putNextEntry(new ZipEntry("Header"));

		try {
			os.writeInt(getCount());
			os.writeLong(getTotalSize());

			for (EZSTFileEntryInputStream is : pool)
				os.writeLong(is.getFile().length());
			os.flush();
		} catch (IOException e) {}

		out.closeEntry();
	}

	private void setCount(int count) {
		fileCount = count;
	}

	private void setPosition(long position) {
		filePosition = position;
	}

	private void setTotalPosition(long position) {
		filePosition = position;
	}

	private void setSize(long size) {
		fileSize = size;
	}

	private void setTotalSize(long size) {
		fileTotalSize = size;
	}
}

