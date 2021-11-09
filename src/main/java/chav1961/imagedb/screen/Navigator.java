package chav1961.imagedb.screen;


import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import chav1961.imagedb.Application;
import chav1961.imagedb.dialogs.ImageItem;
import chav1961.imagedb.dialogs.TreeItem;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SimpleTimerTask;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.model.interfaces.ContentMetadataInterface.ContentNodeMetadata;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.interfaces.OnAction;
import chav1961.purelib.ui.swing.useful.JFileItemDescriptor;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog;
import chav1961.purelib.ui.swing.useful.JFileTree;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog.FilterCallback;
import chav1961.purelib.ui.swing.useful.JLocalizedOptionPane;
import chav1961.purelib.ui.swing.useful.LocalizedFormatter;

public class Navigator extends JSplitPane implements LocaleChangeListener {
	private static final long 				serialVersionUID = 5699595320863149294L;
	public static final String				KEY_CONFIRM_REMOVE_TITLE = "navigator.confirm.remove.title";
	public static final String				KEY_CONFIRM_REMOVE_MESSAGE = "navigator.confirm.remove.message";
	
	private final Localizer					localizer;
	private final LoggerFacade				logger;
	private final Connection				conn;
	private final ContentMetadataInterface	xda;
	private final Application				app;
	private final DefaultMutableTreeNode	root = new DefaultMutableTreeNode(new TreeItem(PureLibSettings.CURRENT_LOGGER, -1,0,"CONTENT:","tree content"));
	private final DefaultTreeModel			model = new DefaultTreeModel(root);
	private final JTree						tree = new JTree() {public String getToolTipText(MouseEvent e) {return getTreeToolTipText(e);}};
	private final JList<ImageItem>		list = new JList<>();
	private final JPopupMenu				treeMenu;
	private TimerTask						tt = null;
	private DefaultMutableTreeNode			currentItem;
	private long							currentId = -1;

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
			SwingUtils.assignActionListeners(this.treeMenu,this);
			
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.getSelectionModel().addTreeSelectionListener((e)->{
				if (e.getNewLeadSelectionPath() != null) {
					final TreeItem	item = (TreeItem)((DefaultMutableTreeNode)e.getNewLeadSelectionPath().getLastPathComponent()).getUserObject();
					
					restartTimerTask(()->{
						fillList(item.id);
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
			SwingUtils.assignActionKey(tree, SwingUtils.KS_INSERT, (e)->processKeys(e.getActionCommand()), SwingUtils.ACTION_INSERT);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_ACCEPT, (e)->processKeys(e.getActionCommand()), SwingUtils.ACTION_ACCEPT);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_DELETE, (e)->processKeys(e.getActionCommand()), SwingUtils.ACTION_DELETE);
			SwingUtils.assignActionKey(tree, SwingUtils.KS_CONTEXTMENU, (e)->processKeys(e.getActionCommand()), SwingUtils.ACTION_CONTEXTMENU);
			ToolTipManager.sharedInstance().registerComponent(tree);
	        fillTreeModel(root, conn);
			tree.setModel(model);
			tree.requestFocusInWindow();
			
			setLeftComponent(new JScrollPane(tree));
			setRightComponent(new JScrollPane(list));
			setBorder(new EtchedBorder(EtchedBorder.LOWERED));
			setDividerLocation(250);
		}
	}

	@Override
	public void localeChanged(final Locale oldLocale, final Locale newLocale) throws LocalizationException {
		// TODO Auto-generated method stub
		
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
		try(final FileSystemInterface	fsi = FileSystemFactory.createFileSystem(URI.create("fsys:file:/"))) {
			for (String item : JFileSelectionDialog.select((Frame)null, localizer, fsi, JFileSelectionDialog.OPTIONS_FOR_OPEN | JFileSelectionDialog.OPTIONS_CAN_SELECT_FILE | JFileSelectionDialog.OPTIONS_FILE_MUST_EXISTS, FilterCallback.of("Image files", "*.png"))) {
				try(final FileSystemInterface	file = fsi.open(item);
					final InputStream			is = file.read()) {
					
					insertImage(ImageIO.read(is));
					return;
				}
			}
		} catch (IOException | LocalizationException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	@OnAction("action:/tree.insertclipboard")
	private void insertClipboard() {
		try{final Clipboard 	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
			insertImage((BufferedImage)clipboard.getData(DataFlavor.imageFlavor));
		} catch (UnsupportedFlavorException | IOException e) {
			logger.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	private void insertImage(final BufferedImage image) {
		ImageObserver	io = (img, infoflags, x, y, width, height)->false;
		
		System.err.println("Image: "+image.getWidth(io)+"/"+image.getHeight(io));
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
		try{if (new JLocalizedOptionPane(localizer).confirm(this, KEY_CONFIRM_REMOVE_MESSAGE, KEY_CONFIRM_REMOVE_TITLE, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
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
	
	private void processKeys(final String actionCommand) {
		final TreePath	path = tree.getSelectionModel().getSelectionPath();
		
		if (path != null) {
			currentItem = (DefaultMutableTreeNode)path.getLastPathComponent();
			currentId = ((TreeItem)currentItem.getUserObject()).id;
			
			switch (actionCommand) {
				case SwingUtils.ACTION_INSERT 		:
					insertChild();
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

	private void fillList(final long ownerId) {
		System.err.println("Fill list for: "+ownerId);
	}

	private void restartTimerTask(final Runnable runnable) {
		if (tt != null) {
			tt.cancel();
			tt = null;
		}
		tt = SimpleTimerTask.start(()->{runnable.run(); tt = null;}, 300);
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
}
