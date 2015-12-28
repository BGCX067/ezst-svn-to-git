
package org.xeonchen.ezst;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

public class EZSTFileEntryInputStream extends InputStream {
	private File file;
	private String path;
	private InputStream in;

	public EZSTFileEntryInputStream(File file, String path) throws FileNotFoundException {
		this.file = file;
		this.path = path;
		in = new BufferedInputStream(new FileInputStream(file));
	}

	public void close() throws IOException {
		in.close();
	}

	public File getFile() {
		return file;
	}

	public String getPath() {
		return path;
	}

	public int read() throws IOException {
		return in.read();
	}
}

