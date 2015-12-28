
package org.xeonchen.ezst;

public interface EZSTProgressEvent {
	int getCount();
	long getPosition();
	long getSize();
	long getTotalPosition();
	long getTotalSize();
}

