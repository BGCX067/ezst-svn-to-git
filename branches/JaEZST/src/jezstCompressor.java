//
//  jezstCompressor.java
//  jezst
//
//  Created by Chen, Liang-heng on 2006/02/20.
//  Copyright 2006 TFCIS. All rights reserved.
//

import java.io.*;
import java.util.Observable;
import java.util.zip.*;

public class jezstCompressor extends Observable {
	private static final int BUFSIZ = 8192;
	private static jezstCompressor instance = null;
	protected boolean enable;
	protected long currentSize, totalSize;

	public jezstCompressor() {
		super();
	}

	public static jezstCompressor getInstance() {
		if (instance == null)
			instance = new jezstCompressor();
		return instance;
	}

	public long getCurrentSize() {
		return currentSize;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public synchronized void setEnabled(boolean enable) {
		this.enable = enable;
	}

	public boolean isEnabled() {
		return enable;
	}

	public void update() {
		update(null);
	}

	public void update(String filename) {
		setChanged();
		notifyObservers(filename);
	}

	public long countSize(File f) {
		if (f.isDirectory()) {
			File[] dir = f.listFiles();
			long size = 0;

			for (int i = 0; i < dir.length; i++)
				size += countSize(dir[i]);

			return size;
		} else {
			return f.length();
		}
	}

	private void sendFile(File file, String dir, ZipOutputStream zos) throws Exception {
		if (file.isDirectory()) {
			// Use '/' instead of File.separatorChar because you don't know what's the receiver uses.
			sendDir(file, dir + '/' + file.getName(), zos);
			return;
		}

		BufferedInputStream fs = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));

		// Use '/' instead of File.separatorChar because you don't know what's the receiver uses.
		ZipEntry ze = new ZipEntry((dir + '/' + file.getName()).substring(2));

		// Actually, setSize() doesn't work. getSize() will always return -1.
		ze.setSize(file.length());

		zos.putNextEntry(ze);

		System.out.println("Compressing " + file.getAbsolutePath() + " ...");
		System.out.println("Size: " + file.length());

		int len;
		byte[] buf = new byte[BUFSIZ];
		while (isEnabled() && (len = fs.read(buf)) != -1) {
			zos.write(buf, 0, len);
			currentSize += len;

			update();
		}
		zos.closeEntry();
		fs.close();
	}

	private void sendDir(File path, String dir, ZipOutputStream zos) throws Exception {
		if (path.isFile()) {
			sendFile(path, dir, zos);
			return;
		}

		File[] files = path.listFiles();

		for (int i = 0; i < files.length; i++)
			sendFile(files[i], dir, zos);
	}

	public void encode(File[] files, OutputStream stream) throws Exception {
		setEnabled(true);

		currentSize = totalSize = 0;
		for (int i = 0; i < files.length; i++)
			totalSize += countSize(files[i]);

		ZipOutputStream zos = new ZipOutputStream(stream);
		zos.setLevel(Deflater.DEFAULT_COMPRESSION);		// level - the compression level (0-9)

		DataOutputStream dos = new DataOutputStream(stream);
		dos.writeInt(files.length);
		dos.writeLong(totalSize);

		update();
		System.out.println("Sending " + totalSize + " bytes.");

		for (int i = 0; isEnabled() && i < files.length; i++)
			sendFile(files[i], ".", zos);

		try {
			zos.close();
			dos.close();
		} catch (Exception e) {
		}

		if (!isEnabled())	// User cancelled
			throw new Exception();
	}

	private void recvFile(ZipEntry ze, File dir, ZipInputStream zis) throws Exception {
		StringBuffer str = new StringBuffer(ze.getName());
		for (int i = 0; i < str.length(); i++)
			if (str.charAt(i) == '/')
				str.setCharAt(i, File.separatorChar);
		String filename = str.toString();

		// Bug of Java, always returns -1.
		long size = ze.getSize();

		File file = new File(dir.getAbsolutePath() + File.separatorChar + filename);
		file.getParentFile().mkdirs();
		BufferedOutputStream fs = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()));

		System.out.println("Extracting " + filename + " (" + size + " bytes) ...");

		int len;
		byte[] buf = new byte[BUFSIZ];
		while (isEnabled() && (len = zis.read(buf)) != -1) {
			fs.write(buf, 0, len);
			currentSize += len;

			update();
		}
		update(file.getAbsolutePath());

		fs.close();
		zis.closeEntry();
	}

	public void decode(File dir, InputStream stream) throws Exception {
		setEnabled(true);

		ZipInputStream zis = new ZipInputStream(stream);
		DataInputStream dis = new DataInputStream(stream);

		int count = dis.readInt();
		totalSize = dis.readLong();
		currentSize = 0;

		update();
		System.out.println("Receiving " + totalSize + " bytes.");

		ZipEntry ze;
		while (isEnabled() && (ze = zis.getNextEntry()) != null)
			recvFile(ze, dir, zis);

		try {
			zis.close();
			dis.close();
		} catch (Exception e) {}

		if (!isEnabled()) {
			throw new Exception();
		}
	}
}
