//
//  jezstClient.java
//  jezst
//
//  Created by Chen, Liang-heng on 2006/09/26.
//  Copyright 2006 TFCIS. All rights reserved.
//

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.xeonchen.ezst.EZSTSocket;

public class jezstClient extends JFrame implements DropTargetListener, Observer {
	// Internal Vars
	protected ResourceBundle resbundle;
	protected static final int defaultPhraseLength = 8;
	protected Vector<File> sendingVector;
	protected EZSTSocket ezstsocket;
	private boolean isSuccess, isConnected;
	private Thread sender, recver;

	// MenuBar
	protected JMenuBar mainMenuBar;
	protected JMenu fileMenu, actionMenu, helpMenu;
	protected JMenuItem addMenuItem, closeMenuItem;
	protected JMenuItem sendMenuItem, recvMenuItem, clearMenuItem, cancelMenuItem;
	protected JMenuItem aboutMenuItem;
	protected Action addAction, closeAction;
	protected Action sendAction, recvAction, clearAction, cancelAction;
	protected Action aboutAction;

	// Phrase Panel
	protected JTextField phraseTextField;
	protected JButton connectButton;
	protected JComboBox serverHostComboBox;

	// Main Panel
	protected JList dndList;
	protected JScrollPane dndScrollPane;
	protected JButton sendButton, recvButton, clearButton;
	protected JProgressBar progressbar;
	protected DropTarget dt;

	// For ProgressBar
	private long progressbarStartTime;

	public jezstClient() {
		super("");

		resbundle = ResourceBundle.getBundle("jezstClient", Locale.getDefault());
		setTitle(resbundle.getString("frameConstructor"));
		getContentPane().setLayout(new BorderLayout());

		createActions();
		addMenus();

		addPhrasePanel(BorderLayout.NORTH);
		addMainPanel(BorderLayout.CENTER);

		clear();
		pack();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
	}

	public void createActions() {
		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		addAction = new addActionClass( resbundle.getString("file.addItem"),
				KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKeyMask), this );
		closeAction = new closeActionClass( resbundle.getString("file.closeItem"),
				KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutKeyMask) );
		sendAction = new sendActionClass( resbundle.getString("action.sendItem"),
				KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask) );
		recvAction = new recvActionClass( resbundle.getString("action.recvItem"),
				KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutKeyMask) );
		clearAction = new clearActionClass( resbundle.getString("action.clearItem"),
				KeyStroke.getKeyStroke(KeyEvent.VK_L, shortcutKeyMask) );
		cancelAction = new cancelActionClass( resbundle.getString("action.cancelItem"),
				KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutKeyMask) );
		aboutAction = new aboutActionClass( resbundle.getString("help.aboutItem"), this);
	}

	public void addMenus() {
		mainMenuBar = new JMenuBar();
		fileMenu = new JMenu(resbundle.getString("fileMenu"));
		actionMenu = new JMenu(resbundle.getString("actionMenu"));
		helpMenu = new JMenu(resbundle.getString("helpMenu"));

		addMenuItem = new JMenuItem(addAction);
		closeMenuItem = new JMenuItem(closeAction);
		sendMenuItem = new JMenuItem(sendAction);
		recvMenuItem = new JMenuItem(recvAction);
		clearMenuItem = new JMenuItem(clearAction);
		cancelMenuItem = new JMenuItem(cancelAction);
		aboutMenuItem = new JMenuItem(aboutAction);

		fileMenu.add(addMenuItem);
		fileMenu.add(closeMenuItem);

		actionMenu.add(sendMenuItem);
		actionMenu.add(clearMenuItem);
		actionMenu.add(recvMenuItem);
		actionMenu.add(cancelMenuItem);

		helpMenu.add(aboutMenuItem);

		mainMenuBar.add(fileMenu);
		mainMenuBar.add(actionMenu);
		mainMenuBar.add(helpMenu);

		setJMenuBar(mainMenuBar);
	}

	public void addPhrasePanel(String position) {
		JPanel panel = new JPanel(new FlowLayout());

		phraseTextField = new JTextField(8);
		phraseTextField.addKeyListener(new TextFieldKeyAdapter());

		panel.add(new JLabel(resbundle.getString("phraseLabel")));
		panel.add(phraseTextField);

		serverHostComboBox = new JComboBox();
		serverHostComboBox.addItem(resbundle.getString("defaultServerHost"));
		serverHostComboBox.setEditable(true);

		panel.add(new JLabel(resbundle.getString("serverLabel")));
		panel.add(serverHostComboBox);

		getContentPane().add(panel, position);
	}

	public void addMainPanel(String position) {
		JPanel panel = new JPanel(new BorderLayout());

		dndList = new JList();
		dndList.addKeyListener(new ListKeyAdapter());

		dndScrollPane = new JScrollPane(dndList);
		sendButton = new JButton(sendAction);
		clearButton = new JButton(clearAction);
		recvButton = new JButton(recvAction);
		sendingVector = new Vector<File>();

		progressbar = new JProgressBar();
		jezstCompressor.getInstance().addObserver(this);
		progressbar.setStringPainted(true);

		dt = new DropTarget(dndList, this);
		dt.setActive(true);

		panel.add(dndScrollPane, BorderLayout.NORTH);
		panel.add(sendButton, BorderLayout.WEST);
		panel.add(clearButton, BorderLayout.CENTER);
		panel.add(recvButton, BorderLayout.EAST);
		panel.add(progressbar, BorderLayout.SOUTH);

		getContentPane().add(panel, position);
	}

	public static String genPhrase(int length) {
		String pool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~!@#$%^&*()_+-=[]{}<>,.:;?";

		StringBuilder str = new StringBuilder(length);

		for (int i = 0; i < length; i++)
			str.append(pool.charAt((int)(Math.random() * pool.length())));

		return str.toString();
	}

	public void send() {
		phraseTextField.setText(phraseTextField.getText().trim());
		setEnableButtonFunctions(false);

		if (!sendingVector.isEmpty()) {
			sender = new Thread() {
				public void run() {
					isSuccess = isConnected = false;

					String serverHost = ((JTextComponent)(serverHostComboBox.getEditor().getEditorComponent())).getText();
					try {
						ezstsocket = new EZSTSocket(serverHost, jezstServer.defaultPort);
						ezstsocket.startHandshake(phraseTextField.getText());
						isConnected = ezstsocket.isConnected();

						progressbarStartTime = (new Date()).getTime();

						jezstCompressor.getInstance().encode((File[])sendingVector.toArray(new File[sendingVector.size()]), new BufferedOutputStream(ezstsocket.getOutputStream()));
						isSuccess = true;
					} catch (Exception e) {}

					if (isSuccess) {
						try {
							System.out.println(new DataInputStream(ezstsocket.getInputStream()).readUTF());
						} catch (Exception e) {
							System.out.println("Remote client disconnected");
						} finally {
							System.out.println("Done Finally");
							try {
								ezstsocket.close();
							} catch (IOException e) {}
						}
					}

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							setEnableButtonFunctions(true);
							if (jezstCompressor.getInstance().isEnabled()) {
								if (isSuccess)
									showResourceMessage("Status.FilesAreSent");
								else
									showResourceMessage("Status.FilesAreNotSent");
							} else {
								if (isConnected)
									showResourceMessage("Status.UserCancelled");
								else
									showResourceMessage("Status.ConnFail");
							}
							update();
						}
					});
				}
			};
			sender.start();
			sender = null;
			// } else
			//	setEnableButtonFunctions(true);
		} else {
			showResourceMessage("Status.ListIsEmpty");
			setEnableButtonFunctions(true);
		}
	}

	public void receive() {
		phraseTextField.setText(phraseTextField.getText().trim());

		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		setEnableButtonFunctions(false);
		int returnVal = fc.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			clearList();
			recver = new Thread() {
				public void run() {
					isSuccess = isConnected = false;

					String serverHost = ((JTextComponent)(serverHostComboBox.getEditor().getEditorComponent())).getText();
					try {
						ezstsocket = new EZSTSocket(serverHost, jezstServer.defaultPort);
						ezstsocket.startHandshake(phraseTextField.getText());
						isConnected = ezstsocket.isConnected();

						progressbarStartTime = (new Date()).getTime();

						jezstCompressor.getInstance().decode(fc.getSelectedFile(), new BufferedInputStream(ezstsocket.getInputStream()));
						isSuccess = true;
					} catch (Exception e) {}

					if (isSuccess) {
						try {
							new DataOutputStream(ezstsocket.getOutputStream()).writeUTF("Done.");
						} catch (Exception e) {
							System.out.println("Remote client disconnected");
						} finally {
							System.out.println("Done Finally");
							try {
								ezstsocket.close();
							} catch (IOException e) {}
						}
					}

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							setEnableButtonFunctions(true);

							if (jezstCompressor.getInstance().isEnabled()) {
								if (isSuccess)
									showResourceMessage("Status.FilesAreReceived");
								else
									showResourceMessage("Status.FilesAreNotReceived");
							} else {
								if (isConnected)
									showResourceMessage("Status.UserCancelled");
								else
									showResourceMessage("Status.ConnFail");
							}
							update();
						}
					});
				}
			};
			recver.start();
			recver = null;
		} else
			setEnableButtonFunctions(true);
	}

	public void cancel() {
		if (jezstCompressor.getInstance().isEnabled()) {
			jezstCompressor.getInstance().setEnabled(!confirmCancel());
		}
	}

	protected boolean confirmCancel() {
		return JOptionPane.showConfirmDialog(this, resbundle.getString("AreYouSure")) == JOptionPane.YES_OPTION;
	}

	protected void setEnableButtonFunctions(boolean enable) {
		setEnableButtonFunctions(enable, enable, enable);
	}

	protected void setEnableButtonFunctions(boolean send, boolean recv, boolean clear) {
		sendButton.setEnabled(send);
		sendMenuItem.setEnabled(send);

		recvButton.setEnabled(recv);
		recvMenuItem.setEnabled(recv);

		clearButton.setEnabled(clear);
		clearMenuItem.setEnabled(clear);
	}

	public void clear() {
		clearPhraseTextField();
		clearList();
		clearProgressbar();
	}

	public void clearPhraseTextField() {
		phraseTextField.setText(genPhrase(defaultPhraseLength));
		phraseTextField.selectAll();
	}

	public void clearList() {
		sendingVector.clear();
		updateList(sendingVector);
	}

	public void clearProgressbar() {
		progressbar.setValue(0);
		progressbar.setString("");
	}

	public void updateList(Vector v) {
		dndList.setListData(v);
	}

	public String byte2unit(long bytes) {
		String a = "";
		return a;
	}

	public void updateProgressbar(long currentSize, long totalSize) {
		if (totalSize == 0) {
			progressbar.setValue(0);
			progressbar.setString("");
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					long currentSize = jezstCompressor.getInstance().getCurrentSize();
					long totalSize = jezstCompressor.getInstance().getTotalSize();
					long seconds = ((new Date()).getTime() - progressbarStartTime) / 1000;
					double speed = ((seconds == 0) ? 0 : (double)currentSize / seconds);
					long remain = (speed == 0) ? Long.MAX_VALUE : (long)((totalSize - currentSize) / speed);

					progressbar.setMinimum(0);
					progressbar.setMaximum(10000);
					progressbar.setValue((int)(currentSize * 10000 / totalSize));

					progressbar.setString(formatSize(currentSize) + " / " + formatSize(totalSize) + " @ " + formatSpeed(speed) + " ( " + String.valueOf(remain) + " " + resbundle.getString("seconds") + " ) ");
				}
			});

		}
	}

	public static String formatSize(long bytes) {
		final int dec = 2;
		if (bytes < (1L << 9))
			return (new BigDecimal(String.valueOf(bytes))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " B";
		else if (bytes < (1L << 19))
			return (new BigDecimal(String.valueOf((double)bytes / (1L << 10)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " KB";
		else if (bytes < (1L << 29))
			return (new BigDecimal(String.valueOf((double)bytes / (1L << 20)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " MB";
		else if (bytes < (1L << 39))
			return (new BigDecimal(String.valueOf((double)bytes / (1L << 30)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " GB";
		else
			return (new BigDecimal(String.valueOf((double)bytes / (1L << 40)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " TB";
	}

	public static String formatSpeed(double bytePerSecond) {
		final int dec = 2;

		if (bytePerSecond < (1L << 10))
			return (new BigDecimal(String.valueOf(bytePerSecond))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " B";
		else if (bytePerSecond < (1L << 20))
			return (new BigDecimal(String.valueOf(bytePerSecond / (1L << 10)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " KB";
		else if (bytePerSecond < (1L << 30))
			return (new BigDecimal(String.valueOf(bytePerSecond / (1L << 20)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " MB";
		else if (bytePerSecond < (1L << 40))
			return (new BigDecimal(String.valueOf(bytePerSecond / (1L << 30)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " GB";
		else
			return (new BigDecimal(String.valueOf(bytePerSecond / (1L << 40)))).setScale(dec, BigDecimal.ROUND_HALF_UP).toString() + " TB";
	}

	public void update() {
		update(null, null);
	}

	public void update(Observable o, Object arg) {
		jezstCompressor c = jezstCompressor.getInstance();

		if (o == c) {
			if (arg != null) {
				addFile((String)arg);
			}
			updateProgressbar(c.getCurrentSize(), c.getTotalSize());
		} else {
			updateProgressbar(0, 0);
		}
	}

	public String getVersion() {
		return resbundle.getString("appVersion");
	}

	public void addFile(String pathname) {
		addFile(new File(pathname));
	}

	public void addFile(File file) {
		if (!sendingVector.contains(file) /*&& file.isFile()*/ && file.canRead()) {
			sendingVector.add(file);
			updateList(sendingVector);
			System.out.println("Added " + file.getName());
		}
	}

	public void addFiles(java.util.List files) {
		for (int i = 0; i < files.size(); i++)
			addFile(files.get(i).toString());
	}

	public void delFile(String pathname) {
		delFile(new File(pathname));
	}

	public void delFile(File file) {
		if (sendingVector.contains(file)) {
			sendingVector.remove(file);
			updateList(sendingVector);
			System.out.println("Deleted " + file.getName());
		}
	}

	public void showResourceMessage(String resString) {
		JOptionPane.showMessageDialog(this, resbundle.getString(resString));
	}

	public void dragEnter(DropTargetDragEvent event) {
		// System.out.println("dragEnter");
		dragOver(event);
	} 

	public void dragExit(DropTargetEvent event) {
		// System.out.println("dragExit");
		// System.out.println("Source: " + event.getSource());
	}

	public void dragOver(DropTargetDragEvent event) {
		// System.out.println("dragOver");
		event.acceptDrag(event.getDropAction());
	}

	private static java.util.List textURIListToFileList(String data) {
		java.util.List<java.io.File> list = new java.util.ArrayList<java.io.File>(1);
		for (java.util.StringTokenizer st = new java.util.StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
			String s = st.nextToken();
			if (s.startsWith("#")) {
				continue;
			}
			try {
				java.net.URI uri = new java.net.URI(s);
				java.io.File file = new java.io.File(uri);
				list.add(file);
			} catch (Exception e) {
			}
		}
		return list;
	}

	public void drop(DropTargetDropEvent event) {
		try {
			DataFlavor[] flavor = event.getTransferable().getTransferDataFlavors();

			for (int i = 0; i < flavor.length; i++) {
				if (flavor[i].isFlavorJavaFileListType()) {
					// event.acceptDrop(DnDConstants.ACTION_COPY);
					event.acceptDrop(event.getDropAction());

					addFiles((java.util.List)event.getTransferable().getTransferData(flavor[i]));

					event.dropComplete(true);
					return;
				} else if (flavor[i].equals(DataFlavor.stringFlavor)) {
					event.acceptDrop(DnDConstants.ACTION_COPY);

					addFiles(textURIListToFileList((String)event.getTransferable().getTransferData(DataFlavor.stringFlavor)));

					event.dropComplete(true);
					return;
				} else {
					// System.out.println("Unknow flavor: " + flavor[i]);
				}
			}

			// Hmm, the user must not have dropped a file list
			System.out.println("Drop failed: " + event);
			event.rejectDrop();
		} catch (Exception e) {
			e.printStackTrace();
			event.rejectDrop();
		}
	}

	public void dropActionChanged(DropTargetDragEvent event) {
		// System.out.println("dropActionChanged");
	}

	public class sendActionClass extends AbstractAction {
		public sendActionClass(String text, KeyStroke shortcut) {
			super(text);
			putValue(ACCELERATOR_KEY, shortcut);
		}
		public void actionPerformed(ActionEvent e) {
			send();
		}
	}

	public class clearActionClass extends AbstractAction {
		public clearActionClass(String text, KeyStroke shortcut) {
			super(text);
			putValue(ACCELERATOR_KEY, shortcut);
		}
		public void actionPerformed(ActionEvent e) {
			clearList();
			clearProgressbar();
		}
	}

	public class recvActionClass extends AbstractAction {
		public recvActionClass(String text, KeyStroke shortcut) {
			super(text);
			putValue(ACCELERATOR_KEY, shortcut);
		}
		public void actionPerformed(ActionEvent e) {
			receive();
		}
	}

	public class addActionClass extends AbstractAction {
		private jezstClient client = null;

		public addActionClass(String text, KeyStroke shortcut, jezstClient client) {
			super(text);
			putValue(ACCELERATOR_KEY, shortcut);
			this.client = client;
		}
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			int returnVal = fc.showOpenDialog(client);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				client.addFile(fc.getSelectedFile());
			}
		}
	}

	public class closeActionClass extends AbstractAction {
		public closeActionClass(String text, KeyStroke shortcut) {
			super(text);
			putValue(ACCELERATOR_KEY, shortcut);
		}
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	public class cancelActionClass extends AbstractAction {
		public cancelActionClass(String text, KeyStroke shortcut) {
			super(text);
			putValue(ACCELERATOR_KEY, shortcut);
		}
		public void actionPerformed(ActionEvent e) {
			cancel();
		}
	}

	public class aboutActionClass extends AbstractAction {
		private jezstClient client = null;

		public aboutActionClass(String text, jezstClient client) {
			super(text);
			this.client = client;
		}
		public void actionPerformed(ActionEvent e) {
			if (client != null) {
				JOptionPane.showMessageDialog(client, "jezstClient - A Java based EZST client\nVersion " + client.getVersion());
			}
		}
	}	

	public class ListKeyAdapter extends KeyAdapter {
		public ListKeyAdapter() {
			super();
		}

		public void keyPressed(KeyEvent e) {
			// System.out.println("murmur(ListKeyAdapter): " + e.getKeyCode());
			if (e.getKeyCode() == KeyEvent.VK_DELETE) {
				Object[] files = dndList.getSelectedValues();

				for (int i = 0; i < files.length; i++)
					delFile((File)files[i]);
			}
		}
	}

	public class TextFieldKeyAdapter extends KeyAdapter {
		public TextFieldKeyAdapter() {
			super();
		}

		public void keyPressed(KeyEvent e) {
			// System.out.println("murmur(TextFieldKeyAdapter): " + e.getKeyCode());
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				cancel();
			}
		}
	}

	public static void main(String[] args) {
		if (System.getProperty("os.name").indexOf("Mac") != -1)
			System.setProperty("apple.laf.useScreenMenuBar", "true");

		new jezstClient();
	}
}
