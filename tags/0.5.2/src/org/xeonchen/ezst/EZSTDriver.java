
package org.xeonchen.ezst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Observable;

public class EZSTDriver extends Observable implements EZSTProgressEvent, Runnable {
	private InputStream is = null;
	private OutputStream os = null;

	private boolean enable = false;
	private boolean good = false;

	EZSTProgressEvent event;

	public EZSTDriver(File[] files, OutputStream out) throws FileNotFoundException, IOException {
		EZSTFileInputStream in = new EZSTFileInputStream(files);

		init(in, out);
		event = in;
	}

	public EZSTDriver(String[] names, OutputStream out) throws FileNotFoundException, IOException {
		EZSTFileInputStream in = new EZSTFileInputStream(names);

		init(in, out);
		event = in;
	}

	public EZSTDriver(File file, InputStream in) throws IOException {
		EZSTFileOutputStream out = new EZSTFileOutputStream(file);

		init(in, out);
		event = out;
	}

	public EZSTDriver(String name, InputStream in) throws IOException {
		EZSTFileOutputStream out = new EZSTFileOutputStream(name);

		init(in, out);
		event = out;
	}

	public int getCount() {
		return event.getCount();
	}

	public long getPosition() {
		return event.getPosition();
	}

	public long getSize() {
		return event.getSize();
	}

	public long getTotalPosition() {
		return event.getTotalPosition();
	}

	public long getTotalSize() {
		return event.getTotalSize();
	}

	private void init(InputStream in, OutputStream out) {
		is = new BufferedInputStream(in);
		os = new BufferedOutputStream(out);
	}

	public boolean isEnabled() {
		return enable;
	}

	public boolean isGood() {
		return good;
	}

	public void run() {
		byte[] buf = new byte[1024];
		int len = 0;

		setGood(false);
		try {
			while ((len = is.read(buf)) != -1) {
				os.write(buf, 0, len);
				os.flush();
				setChanged();
				notifyObservers();
			}

			os.close();
			is.close();

			setGood(true);
		} catch (Exception e) {
		}
	}

	public void setEnabled(boolean enable) {
		this.enable = enable;
	}

	private void setGood(boolean good) {
		this.good = good;
	}
}

