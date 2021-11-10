package chav1961.imagedb.screen;



import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import chav1961.imagedb.Application;
import chav1961.imagedb.dialogs.ImageItem;
import chav1961.imagedb.dialogs.TreeItem;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SimpleTimerTask;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.interfaces.OnAction;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog.FilterCallback;
import chav1961.purelib.ui.swing.useful.JLocalizedOptionPane;

public class Navigator extends JSplitPane implements LocaleChangeListener {
	private static final long 					serialVersionUID = 5699595320863149294L;
	public static final String					KEY_CONFIRM_REMOVE_TITLE = "navigator.confirm.remove.title";
	public static final String					KEY_CONFIRM_REMOVE_MESSAGE_TREE = "navigator.confirm.remove.message.tree";
	public static final String					KEY_CONFIRM_REMOVE_MESSAGE_LIST = "navigator.confirm.remove.message.list";
	
	private final Localizer						localizer;
	private final LoggerFacade					logger;
	private final Connection					conn;
	private final ContentMetadataInterface		xda;
	private final Application					app;
	private final DefaultMutableTreeNode		root = new DefaultMutableTreeNode(new TreeItem(PureLibSettings.CURRENT_LOGGER, -1,0,"CONTENT:","tree content"));
	private final DefaultTreeModel				model = new DefaultTreeModel(root);
	private final JTree							tree = new JDroppableTree(model) {
																private static final long serialVersionUID = 1L;
																@Override
																public String getToolTipText(MouseEvent e) {
																	return getTreeToolTipText(e);
																}
														};
	private final DefaultListModel<ImageItem>	listModel = new DefaultListModel<>();
	private final JList<ImageItem>				list = new JDroppableList<>(listModel) {
																private static final long serialVersionUID = 1L;
																@Override
																public String getToolTipText(MouseEvent e) {
																	return getListToolTipText(e);
																}
															};
	private final JPopupMenu					treeMenu;
	private final JPopupMenu					newListMenu;
	private final JPopupMenu					existentListMenu;
	private TimerTask							tt = null;
	private DefaultMutableTreeNode				currentItem;
	private long								currentId = -1;
	private int									currentImageId = -1;

	public Navigator(final Localizer localizer, final LoggerFacade logger, final Connection conn, final ContentMetadataInterface xda, final Application app) throws SQLException {
		if (localizer == null) {
			throw new NullPointerException("Localizer can't be null"); 
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null"); 
		}
		else if (conn == null) {
			throw new NullPointerException("Connection can't be null"); 
		}
		else if (xda == null) {
			throw new NullPointerException("Metadata interface can't be null"); 
		}
		else if (app == null) {
			throw new NullPointerException("Applicaiton can't be null"); 
		}
		else {
			this.localizer = localizer;
			this.logger = logger;
			this.conn = conn;
			this.xda = xda;
			this.app = app;
			
			this.treeMenu = SwingUtils.toJComponent(xda.byUIPath(URI.create("ui:/model/navigation.top.treemenu")),JPopupMenu.class); 
			SwingUtils.assignActionListeners(this.treeMenu, this);
			this.newListMenu = SwingUtils.toJComponent(xda.byUIPath(URI.create("ui:/model/navigation.top.newimagemenu")),JPopupMenu.class); 
			SwingUtils.assignActionListeners(this.newListMenu, this);
			this.existentListMenu = SwingUtils.toJComponent(xda.byUIPath(URI.create("ui:/model/navigation.top.imagemenu")),JPopupMenu.class); 
			SwingUtils.assignActionListeners(this.existentListMenu, this);
			
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.getSelectionModel().addTreeSelectionListener((e)->{
				if (e.getNewLeadSelectionPath() != null) {
					final TreeItem	item = (TreeItem)((DefaultMutableTreeNode)e.getNewLeadSelectionPath().getLastPathComponent()).getUserObject();
					
					restartTimerTask(()->{
						try{fillList(item.id);
						} catch (SQLException exc) {
							logger.message(Severity.error, exc.getLocalizedMessage(), exc);
						}
					});
				}
			});
			tree.setCellRenderer(new DefaultTreeCellRenderer() {
						private static final long serialVersionUID = 1L;

						public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
							final JLabel	result = (JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
							final Object	obj = ((DefaultMutableTreeNode)value).getUserObject();

							if (obj instanceof TreeItem) {
								final TreeItem 	val = (TreeItem)obj;
								
								result.setText(val.name);
								if (val.comment != null) {
									result.setToolTipText(val.comment);
								}
							}
							return result;
						};
					}
			);
			tree.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					final TreePath	path = tree.getPathForLocation(e.getX(), e.getY());
					
					if (path != null) {
						currentItem = (DefaultMutableTreeNode)path.getLastPathComponent();
						currentId = ((TreeItem)currentItem.getUserObject()).id;
						
						if (e.getButton() == MouseEvent.BUTTON3) {
							showPopupMenu(currentItem, (TreeItem)currentItem.getUserObject(), e.getX(), e.getY());
						}
						else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
							edit();
						}
					}
				}

				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) {}
				@Override public void mouseExited(MouseEvent e) {}
			});

			SwingUtils.assignActionKey(tree, SwingUtils.KS_INSERT, (e)->processTreeKeys(e.getActionCommand()), SwingUtils.ACTION_INSERT);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_ACCEPT, (e)->processTreeKeys(e.getActionCommand()), SwingUtils.ACTION_ACCEPT);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_DELETE, (e)->processTreeKeys(e.getActionCommand()), SwingUtils.ACTION_DELETE);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_PASTE, (e)->processTreeKeys(e.getActionCommand()), SwingUtils.ACTION_PASTE);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_CONTEXTMENU, (e)->processTreeKeys(e.getActionCommand()), SwingUtils.ACTION_CONTEXTMENU);
			
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.setCellRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					final JLabel	result = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

					result.setText("");
					result.setBorder(new LineBorder(Color.BLACK));
					result.setIcon(new ImageIcon(((ImageItem)value).image));
					return result;
				}
			});
			list.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					final int		location = list.locationToIndex(new Point(e.getX(), e.getY()));
					
					if (location >= 0) {
						currentImageId = location;
						
						if (e.getButton() == MouseEvent.BUTTON3) {
							showPopupMenu(currentImageId, listModel.elementAt(currentImageId), e.getX(), e.getY());
						}
						else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2 && currentImageId < listModel.getSize() - 1) {
							editImage();
						}
					}
				}

				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) {}
				@Override public void mouseExited(MouseEvent e) {}
			});

			SwingUtils.assignActionKey(list, SwingUtils.KS_INSERT, (e)->processListKeys(e.getActionCommand()), SwingUtils.ACTION_INSERT);
			SwingUtils.assignActionKey(list, SwingUtils.KS_ACCEPT, (e)->processListKeys(e.getActionCommand()), SwingUtils.ACTION_ACCEPT);
			SwingUtils.assignActionKey(list, SwingUtils.KS_DELETE, (e)->processListKeys(e.getActionCommand()), SwingUtils.ACTION_DELETE);
			SwingUtils.assignActionKey(list, SwingUtils.KS_COPY, (e)->processListKeys(e.getActionCommand()), SwingUtils.ACTION_COPY);
			SwingUtils.assignActionKey(list, SwingUtils.KS_PASTE, (e)->processListKeys(e.getActionCommand()), SwingUtils.ACTION_PASTE);
			SwingUtils.assignActionKey(list, SwingUtils.KS_CONTEXTMENU, (e)->processListKeys(e.getActionCommand()), SwingUtils.ACTION_CONTEXTMENU);
			
			ToolTipManager.sharedInstance().registerComponent(tree);
	        fillTreeModel(root, conn);
			tree.requestFocusInWindow();
			
			setLeftComponent(new JScrollPane(tree));
			setRightComponent(new JScrollPane(list));
			setBorder(new EtchedBorder(EtchedBorder.LOWERED));
			setDividerLocation(250);
			
			SwingUtilities.invokeLater(()->{
				tree.grabFocus();
				tree.setSelectionRow(0);
			});
		}
	}

	@Override
	public void localeChanged(final Locale oldLocale, final Locale newLocale) throws LocalizationException {
		// TODO Auto-generated method stub
	}

	public void selectTree(final long id) {
		walkTree(root,id);
	}

	public void selectImage(final long id) {
		try(final Statement	stmt = conn.createStatement();
			final ResultSet	rs = stmt.executeQuery("select ct_Id from helen.contentimage where ci_Id = "+id)) {

			if (rs.next()) {
				final long		owner = rs.getLong(1);
				final TimerTask	temp = tt;
				
				selectTree(owner);
				if (temp != null) {
					temp.cancel();
				}
				fillList(owner);

				for (int index = 0, maxIndex = listModel.getSize(); index < maxIndex; index++) {
					if (listModel.elementAt(index).id == id) {
						list.ensureIndexIsVisible(index);
						list.setSelectedIndex(index);
						break;
					}
				}
			}
		} catch (SQLException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}
	
	private String getTreeToolTipText(final MouseEvent event) {
		final TreePath	path = tree.getPathForLocation(event.getX(), event.getY());
		
		if (path != null) {
			final Object	obj = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
			
			if (obj instanceof TreeItem) {
				return ((TreeItem)obj).comment; 
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	private String getListToolTipText(final MouseEvent event) {
		final int		location = list.locationToIndex(new Point(event.getX(), event.getY()));
		
		if (location >= 0) {
			final ImageItem		item = listModel.elementAt(location);
			
			return item.comment; 
		}
		else {
			return null;
		}
	}
	
	@OnAction("action:/tree.insertchild")
	private void insertChild() {
		try(final Statement	stmt = conn.createStatement()) {
			final long		uniqueId = getUniqueId(conn);
			
			stmt.executeUpdate("insert into helen.contenttree (ct_Id, ct_Parent, ct_Name, ct_Comment) values ("+uniqueId+", "+currentId+", 'child', 'new child')");
			try(final ResultSet	rs = stmt.executeQuery("select * from helen.contenttree where ct_Id = "+uniqueId)) {
				if (rs.next()) {
					final TreeItem					item = new TreeItem(logger, rs.getLong("ct_Id"), rs.getLong("ct_Parent"), rs.getString("ct_Name"), rs.getString("ct_Comment"));
					final DefaultMutableTreeNode	child = new DefaultMutableTreeNode(item);
					
					model.insertNodeInto(child, currentItem, currentItem.getChildCount());
					return;
				}
			}
		} catch (SQLException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	@OnAction("action:/tree.insertimage")
	private void insertImage() {
		withImage((img)->{insertImage(img); return null;});
	}

	@OnAction("action:/tree.insertclipboard")
	private void insertClipboard() {
		try{final Clipboard 	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
			if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
				insertImage((BufferedImage)clipboard.getData(DataFlavor.imageFlavor));
			}
		} catch (UnsupportedFlavorException | IOException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	private void insertImage(final BufferedImage image) {
		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			
			ImageIO.write(image, "png", baos);
			
			try(final PreparedStatement	ps = conn.prepareStatement("insert into helen.contentimage(ci_Id, ct_Id, ci_Comment, ci_Image) values (?, ?, ?, ?)")) {
				ps.setLong(1, getUniqueId(conn));
				ps.setLong(2, currentId);
				ps.setString(3, "New image");
				ps.setBinaryStream(4, new ByteArrayInputStream(baos.toByteArray()));
				ps.executeUpdate();
			}
			fillList(currentId);
		} catch (SQLException | IOException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	private void updateImage(final BufferedImage image) {
		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			final ImageItem				ii = listModel.elementAt(currentImageId);
			
			ii.image = image;
			ImageIO.write(image, "png", baos);
			
			try(final PreparedStatement	ps = conn.prepareStatement("update helen.contentimage set ci_Image = ? where ci_Id = ?")) {
				ps.setBinaryStream(1, new ByteArrayInputStream(baos.toByteArray()));
				ps.setLong(2, ii.id);
				ps.executeUpdate();
			}
			listModel.set(currentImageId, ii);
		} catch (SQLException | IOException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}
	
	@OnAction("action:/tree.edit")
	private void edit() {
		try{final TreeItem	ti = ((TreeItem)currentItem.getUserObject()).clone();
			
			if (app.ask(ti, 200, 70)) {
				try(final PreparedStatement	ps = conn.prepareStatement("update helen.contenttree set ct_Name = ?, ct_Comment = ? where ct_Id = ?")) {
					ps.setString(1, ti.name);
					ps.setString(2, ti.comment);
					ps.setLong(3, ti.id);
					ps.executeUpdate();
				}
				currentItem.setUserObject(ti);
				model.nodeChanged(currentItem);
			}
		} catch (SQLException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	@OnAction("action:/tree.remove.subtree")
	private void removeSubtree() {
		try{if (new JLocalizedOptionPane(localizer).confirm(this, KEY_CONFIRM_REMOVE_MESSAGE_TREE, KEY_CONFIRM_REMOVE_TITLE, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				try(final PreparedStatement	ps = conn.prepareStatement("delete from helen.contenttree where ct_Id = ?")) {
					final List<Long>		ids = new ArrayList<>();
					
					collectIds(currentItem, ids);
					for(long item : ids) {
						ps.setLong(1, item);
						ps.executeUpdate();
					}
					model.removeNodeFromParent(currentItem);
				}
			}
		} catch (SQLException | LocalizationException exc) {
			logger.message(Severity.error, exc.getLocalizedMessage(), exc);
		}
	}

	@OnAction("action:/list.insertimage")
	private void insertImageList() {
		withImage((img)->{insertImage(img); return null;});
	}	
	
	@OnAction("action:/list.insertclipboard")
	private void insertClipboardList() {
		try{final Clipboard 	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

			if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
				insertImage((BufferedImage)clipboard.getData(DataFlavor.imageFlavor));
			}
		} catch (UnsupportedFlavorException | IOException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}	

	@OnAction("action:/list.copytoclipboard")
	private void copyImageList() {
		final Clipboard 		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		final BufferedImage		bi = listModel.elementAt(currentImageId).image;
		final Transferable		t = new Transferable() {
										@Override public boolean isDataFlavorSupported(final DataFlavor flavor) {return flavor == DataFlavor.imageFlavor;}
										@Override public DataFlavor[] getTransferDataFlavors() {return new DataFlavor[] {DataFlavor.imageFlavor};}
										@Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {return flavor == DataFlavor.imageFlavor ? bi : null;}
									};
		final ClipboardOwner	co = new ClipboardOwner() {
									@Override public void lostOwnership(Clipboard clipboard, Transferable contents) {}
								};

		clipboard.setContents(t, co);
	}	
	
	@OnAction("action:/list.updateimage")
	private void updateImageList() {
		withImage((img)->{updateImage(img); return null;});
	}	

	@OnAction("action:/list.updatefromclipboard")
	private void updateClipboardList() {
		try{final Clipboard 	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
			if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
				updateImage((BufferedImage)clipboard.getData(DataFlavor.imageFlavor));
			}
		} catch (UnsupportedFlavorException | IOException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}	
	
	@OnAction("action:/list.edit")
	private void editImage() {
		try{final ImageItem	ii = listModel.elementAt(currentImageId).clone();
		
			if (app.ask(ii, 200, 70)) {
				try(final PreparedStatement	ps = conn.prepareStatement("update helen.contentimage set ci_Comment = ? where ci_Id = ?")) {
					ps.setString(1, ii.comment);
					ps.setLong(2, ii.id);
					ps.executeUpdate();
				}
				listModel.set(currentImageId, ii);
			}
		} catch (SQLException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}
	
	@OnAction("action:/list.remove")
	private void removeList() {
		try{if (new JLocalizedOptionPane(localizer).confirm(this, KEY_CONFIRM_REMOVE_MESSAGE_LIST, KEY_CONFIRM_REMOVE_TITLE, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				try(final PreparedStatement	ps = conn.prepareStatement("delete from helen.contentimage where ci_Id = ?")) {
					ps.setLong(1, listModel.elementAt(currentImageId).id);
					ps.executeUpdate();
					listModel.remove(currentImageId);
				}
			}
		} catch (SQLException | LocalizationException exc) {
			logger.message(Severity.error, exc.getLocalizedMessage(), exc);
		}
	}
	
	private void collectIds(final DefaultMutableTreeNode node, final List<Long> ids) {
		ids.add(((TreeItem)node.getUserObject()).id);
		for(int index = 0, maxIndex = node.getChildCount(); index < maxIndex; index++) {
			collectIds((DefaultMutableTreeNode)node.getChildAt(index), ids);
		}
	}
	
	private void showPopupMenu(final DefaultMutableTreeNode node, final TreeItem content, final int x, final int y) {
		if (content.id != -1) {
			final Clipboard 	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			
			((JMenuItem)SwingUtils.findComponentByName(treeMenu, "tree.insert.clipboard")).setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor));
			treeMenu.show(tree, x, y);
		}
	}

	private void showPopupMenu(final int index, final ImageItem value, final int x, final int y) {
		final Clipboard 	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
		if (index == listModel.getSize() - 1) {
			((JMenuItem)SwingUtils.findComponentByName(newListMenu, "list.insert.clipboard")).setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor));
			
			newListMenu.show(list, x, y);
		}
		else {
			((JMenuItem)SwingUtils.findComponentByName(existentListMenu, "list.update.clipboard")).setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor));
			
			existentListMenu.show(list, x, y);
		}
	}

	private void withImage(final Function<BufferedImage, Void> func) {
		try(final FileSystemInterface	fsi = FileSystemFactory.createFileSystem(URI.create("fsys:file:/"))) {
			for (String item : JFileSelectionDialog.select((Frame)null, localizer, fsi, JFileSelectionDialog.OPTIONS_FOR_OPEN | JFileSelectionDialog.OPTIONS_CAN_SELECT_FILE | JFileSelectionDialog.OPTIONS_FILE_MUST_EXISTS, FilterCallback.of("Image files", "*.png"))) {
				try(final FileSystemInterface	file = fsi.open(item);
					final InputStream			is = file.read()) {
					
					func.apply(ImageIO.read(is));
					return;
				}
			}
		} catch (IOException | LocalizationException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}
	
	private void processTreeKeys(final String actionCommand) {
		final TreePath	path = tree.getSelectionModel().getSelectionPath();
		
		if (path != null) {
			currentItem = (DefaultMutableTreeNode)path.getLastPathComponent();
			currentId = ((TreeItem)currentItem.getUserObject()).id;
			
			switch (actionCommand) {
				case SwingUtils.ACTION_INSERT 		:
					insertChild();
					break;
				case SwingUtils.ACTION_PASTE		:
					insertClipboard();
					break;
				case SwingUtils.ACTION_ACCEPT 		:
					edit();
					break;
				case SwingUtils.ACTION_DELETE		:
					removeSubtree();
					break;
				case SwingUtils.ACTION_CONTEXTMENU	:
					final Rectangle	rect = tree.getRowBounds(tree.getRowForPath(path));
					
					showPopupMenu(currentItem, (TreeItem)currentItem.getUserObject(), (int)rect.getCenterX(), (int)rect.getCenterX());
					break;
			}
		}		
	}

	private void processListKeys(final String actionCommand) {
		if (!list.isSelectionEmpty()) {
			final int	index = list.getSelectionModel().getAnchorSelectionIndex();
			currentImageId = index;
			
			switch (actionCommand) {
				case SwingUtils.ACTION_INSERT 		:
					insertImageList();
					break;
				case SwingUtils.ACTION_ACCEPT 		:
					if (currentImageId < listModel.getSize() - 1) {
						editImage();
					}
					break;
				case SwingUtils.ACTION_DELETE		:
					if (currentImageId < listModel.getSize() - 1) {
						removeList();
					}
					break;
				case SwingUtils.ACTION_COPY		:
					if (currentImageId < listModel.getSize() - 1) {
						copyImageList();
					}
					break;
				case SwingUtils.ACTION_PASTE		:
					if (currentImageId < listModel.getSize() - 1) {
						updateClipboardList();
					}
					break;
				case SwingUtils.ACTION_CONTEXTMENU	:
					final Point		p = list.indexToLocation(index);
					final Rectangle	r = list.getCellBounds(index, index);
					
					showPopupMenu(currentImageId, listModel.elementAt(currentImageId), p.x + r.width/2, p.y + r.height/2);
					break;
			}
		}		
	}
	
	private void fillTreeModel(final DefaultMutableTreeNode root, final Connection conn) throws SQLException {
		root.removeAllChildren();
		try(final Statement	stmt = conn.createStatement();
			final ResultSet	rs = stmt.executeQuery("select * from helen.contenttree")) {

			if (!rs.next()) {
				stmt.execute("insert into helen.contenttree (ct_Id, ct_Parent, ct_Name, ct_Comment) values (nextval('helen.contentseq'), -1, 'ROOT', '')");
			}
		}
		try (final PreparedStatement	ps = conn.prepareStatement("select * from helen.contenttree where ct_Parent = ?")) {
			fillTreeModel(root, ps, -1L);
		}
	}

	private void fillTreeModel(final DefaultMutableTreeNode root, final PreparedStatement ps, final long key) throws SQLException {
		final List<TreeItem>	temp = new ArrayList<>();
		
		ps.setLong(1, key);
		try(final ResultSet	rs = ps.executeQuery()) {
			while (rs.next()) {
				temp.add(new TreeItem(logger, rs.getLong("ct_Id"), rs.getLong("ct_Parent"), rs.getString("ct_Name"), rs.getString("ct_Comment")));
			}
		}
		for (TreeItem item : temp) {
			final DefaultMutableTreeNode	child = new DefaultMutableTreeNode(item);
			
			fillTreeModel(child, ps, item.id);
			((DefaultTreeModel)model).insertNodeInto(child, root, 0);
		}
	}

	private void fillList(final long ownerId) throws SQLException {
		final List<ImageItem>	result = new ArrayList<>();

		listModel.removeAllElements();
		if (ownerId >= 0) {
			try(final Statement	stmt = conn.createStatement();
				final ResultSet	rs = stmt.executeQuery("select * from helen.contentimage where ct_Id = "+ownerId+" order by ci_Id")) {
				
				while (rs.next()) {
					final BufferedImage	bi = ImageIO.read(rs.getBinaryStream("ci_Image"));
					
					result.add(new ImageItem(logger, rs.getLong("ci_Id"), rs.getLong("ct_Id"), rs.getString("ci_Comment"), bi));
				}
				result.add(new ImageItem(logger, -1L, ownerId, "Add new image", ImageIO.read(this.getClass().getResourceAsStream("addImage.png"))));
			} catch (IOException e) {
				throw new SQLException(e.getLocalizedMessage(), e);
			}
			listModel.addAll(result);
		}
	}

	private void restartTimerTask(final Runnable runnable) {
		if (tt != null) {
			tt.cancel();
			tt = null;
		}
		tt = SimpleTimerTask.start(()->{runnable.run(); tt = null;}, 300);
	}

	private void walkTree(final DefaultMutableTreeNode node, final long id) {
		if (((TreeItem)node.getUserObject()).id == id) {
			  final TreeNode[] 	nodes = model.getPathToRoot(node);
              final TreePath 	tpath = new TreePath(nodes);
              
              tree.scrollPathToVisible(tpath);
              tree.setSelectionPath(tpath);			
		}
		else {
			for (int index = 0, maxIndex = node.getChildCount(); index < maxIndex; index++) {
				walkTree((DefaultMutableTreeNode)node.getChildAt(index), id);
			}
		}
	}

	
	private long getUniqueId(final Connection conn) throws SQLException {
		try(final Statement	stmt = conn.createStatement();
			final ResultSet	rs = stmt.executeQuery("select nextval('helen.contentseq')")) {
		
			if (rs.next()) {
				return rs.getLong(1);
			}
			else {
				return 0;
			}
		}
	}
	

	private class JDroppableTree extends JTree implements DropTargetListener, DragGestureListener, DragSourceListener {
		private static final long 	serialVersionUID = 1L;

		private final DropTarget	dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this); 
		private final DragSource 	dragSource = DragSource.getDefaultDragSource();
		
		public JDroppableTree(final TreeModel model) {
			super(model);
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
		}
		
		@Override public void dragGestureRecognized(DragGestureEvent dge) {}
		@Override public void dragOver(DropTargetDragEvent dtde) {}
		@Override public void dropActionChanged(DropTargetDragEvent dtde) {}
		@Override public void dragExit(DropTargetEvent dte) {}
		@Override public void dragEnter(DragSourceDragEvent dsde) {}
		@Override public void dragOver(DragSourceDragEvent dsde) {}
		@Override public void dropActionChanged(DragSourceDragEvent dsde) {}
		@Override public void dragExit(DragSourceEvent dse) {}
		@Override public void dragDropEnd(DragSourceDropEvent dsde) {}
		
		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
				dtde.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
			}
		}

		@Override
		public void drop(final DropTargetDropEvent dtde) {
			try{final Transferable 	tr = dtde.getTransferable();
			
                if (tr.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    
					final TreePath	path = tree.getPathForLocation(dtde.getLocation().x, dtde.getLocation().y);
					
					if (path != null) {
						currentItem = (DefaultMutableTreeNode)path.getLastPathComponent();
						currentId = ((TreeItem)currentItem.getUserObject()).id;
						
	                    for (File f : (List<File>)tr.getTransferData(DataFlavor.javaFileListFlavor)) {
	                    	if (f.getName().endsWith(".png")) {
	                    		try(final InputStream	is = new FileInputStream(f)) {
	                    			
	                   				insertImage(ImageIO.read(is));
	                    		}
	                    	}
	                    }
	                    dtde.getDropTargetContext().dropComplete(true);
					}
					else {
	                	dtde.rejectDrop();
					}
                } 
                else {
                	dtde.rejectDrop();
                }
            } catch (IOException | UnsupportedFlavorException ufe) {
            	dtde.rejectDrop();
            }			
		}
	}
	
	private class JDroppableList<T> extends JList<T> implements DropTargetListener, DragGestureListener, DragSourceListener {
		private static final long 	serialVersionUID = 1L;

		private final DropTarget	dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this); 
		private final DragSource 	dragSource = DragSource.getDefaultDragSource();
	     
		public JDroppableList(ListModel<T> dataModel) {
			super(dataModel);
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
		}

		@Override public void dragGestureRecognized(DragGestureEvent dge) {}
		@Override public void dragOver(DropTargetDragEvent dtde) {}
		@Override public void dropActionChanged(DropTargetDragEvent dtde) {}
		@Override public void dragExit(DropTargetEvent dte) {}
		@Override public void dragEnter(DragSourceDragEvent dsde) {}
		@Override public void dragOver(DragSourceDragEvent dsde) {}
		@Override public void dropActionChanged(DragSourceDragEvent dsde) {}
		@Override public void dragExit(DragSourceEvent dse) {}
		@Override public void dragDropEnd(DragSourceDropEvent dsde) {}
		
		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
				dtde.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
			}
		}

		@Override
		public void drop(final DropTargetDropEvent dtde) {
			try{final Transferable 	tr = dtde.getTransferable();
			
                if (tr.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    
					currentImageId = locationToIndex(dtde.getLocation());
					final boolean	updateContent = currentImageId < getModel().getSize() - 1;
                    
                    for (File f : (List<File>)tr.getTransferData(DataFlavor.javaFileListFlavor)) {
                    	if (f.getName().endsWith(".png")) {
                    		try(final InputStream	is = new FileInputStream(f)) {
                    			
                    			if (updateContent) {
                    				updateImage(ImageIO.read(is));
                    			}
                    			else {
                    				insertImage(ImageIO.read(is));
                    			}
                    		}
                    	}
                    }
                    dtde.getDropTargetContext().dropComplete(true);
                } 
                else {
                	dtde.rejectDrop();
                }
            } catch (IOException | UnsupportedFlavorException ufe) {
            	dtde.rejectDrop();
            }			
		}
	}
}
