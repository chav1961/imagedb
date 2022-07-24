package chav1961.imagedb.screen;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.LineBorder;

import chav1961.imagedb.db.DbUtil;
import chav1961.imagedb.dialogs.FilterItem;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.ui.inner.InternalConstants;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.ItemAndSelection;
import chav1961.purelib.ui.swing.AutoBuiltForm;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.useful.JCloseButton;

public class SearchPanel extends JPanel implements LocaleChangeListener{
	private static final long serialVersionUID = -257313636223098767L;
	public static final String			KEY_SEARCH_CAPTION = "search.caption";				
	public static final String			KEY_SEARCH_CLOSE_TT = "search.close.tt";				
	public static final String			KEY_SEARCH_ENTER = "search.enter";				
	public static final String			KEY_SEARCH_ENTER_TT = "search.enter.tt";				
	public static final String			KEY_SEARCH_FIND = "search.find.tt";				
	public static final String			KEY_SEARCH_NOTFOUND = "search.notfound";				
	public static final String			KEY_SEARCH_FOUND = "search.found";				
	public static final String			KEY_SEARCH_TOOLTIP = "search.tooltip";				

	private final Localizer							localizer;
	private final LoggerFacade						logger;
	private final Connection						conn;
	private final Consumer<SearchResult>			selectCallback;
	private final JLabel							searchCaption = new JLabel("", JLabel.CENTER);
	private final JButton							closeButton = new JButton(InternalConstants.ICON_CLOSE);
	private final JLabel							searchLabel = new JLabel("");
	private final JTextField						searchString = new JTextField();
	private final JButton							searchButton = new JButton(InternalConstants.ICON_SEARCH);
	private final JToggleButton						facetButton = new JToggleButton(InternalConstants.ICON_CHECK);
	private final DefaultListModel<SearchResult>	listModel = new DefaultListModel<>();
	private final JList<SearchResult>				list;
	private final FilterItem						filter;
	private final AutoBuiltForm<FilterItem,?>		abf;
	
	public SearchPanel(final Localizer localizer, final LoggerFacade logger, final Connection conn, final Consumer<SearchPanel> closeCallback, final Consumer<SearchResult> selectCallback) throws NullPointerException, LocalizationException, SQLException {
		if (localizer == null) {
			throw new NullPointerException("Localizer can't be null"); 
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null"); 
		}
		else if (conn == null) {
			throw new NullPointerException("Connection can't be null"); 
		}
		else if (closeCallback == null) {
			throw new NullPointerException("Close callback can't be null"); 
		}
		else if (selectCallback == null) {
			throw new NullPointerException("Select callback can't be null"); 
		}
		else {
			this.localizer = localizer;
			this.logger = logger;
			this.conn = conn;
			this.list = new JListWithTooltips(localizer, listModel);
			this.selectCallback = selectCallback;
			this.filter = new FilterItem(logger, ItemAndSelection.of(DbUtil.extractUniqueTags(conn)));
			this.abf = buildFacets(filter, 100, 200);
			
			closeButton.addActionListener((e)->closeCallback.accept(SearchPanel.this));
			setLayout(new BorderLayout());
	
			closeButton.setPreferredSize(new Dimension(InternalConstants.ICON_CLOSE.getIconWidth(),InternalConstants.ICON_CLOSE.getIconHeight()));
			searchString.setColumns(30);
			
			searchButton.setPreferredSize(new Dimension(InternalConstants.ICON_SEARCH.getIconWidth()+4,InternalConstants.ICON_SEARCH.getIconHeight()+4));
			searchButton.addActionListener((e)->fillList(searchString.getText()));
			facetButton.setPreferredSize(new Dimension(InternalConstants.ICON_CHECK.getIconWidth()+4,InternalConstants.ICON_CHECK.getIconHeight()+4));
			facetButton.addActionListener((e)->showFacet(facetButton.isSelected()));
			showFacet(false);
		
			list.setCellRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					final JLabel		label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					final SearchResult	sr = (SearchResult)value;

					switch (sr.location) {
						case IN_IMAGE_COMMENT	:
							label.setText(sr.comment);
							break;
						case IN_TREE_COMMENT	:
							label.setText(sr.comment);
							break;
						case IN_TREE_NAME		:
							label.setText(sr.name);
							break;
						default:
							break;
					}
					return label;
				}
			});
			list.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						gotoItem();
					}
				}
				
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseExited(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) {}
			});
			
			final JPanel	caption = new JPanel(new BorderLayout()), close = new JPanel(), search = new JPanel(), top = new JPanel(new GridLayout(2,1));  
			
			close.add(closeButton);
			caption.add(searchCaption, BorderLayout.CENTER);
			caption.add(close, BorderLayout.EAST);
			caption.setBorder(new LineBorder(Color.BLACK));
			search.add(searchLabel);
			search.add(searchString);
			search.add(searchButton);
			search.add(facetButton);
			top.add(caption);
			top.add(search);
			
			add(top, BorderLayout.NORTH);
			add(new JScrollPane(list), BorderLayout.CENTER);
			add(abf, BorderLayout.SOUTH);
			
			SwingUtils.assignActionKey(this, JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, SwingUtils.KS_ACCEPT, (e)->fillList(searchString.getText()), SwingUtils.ACTION_ACCEPT);
			SwingUtils.assignActionKey(this, JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, SwingUtils.KS_EXIT, (e)->closeCallback.accept(SearchPanel.this), SwingUtils.ACTION_EXIT);
			SwingUtils.assignActionKey(list, SwingUtils.KS_ACCEPT, (e)->gotoItem(), SwingUtils.ACTION_ACCEPT);
			
			list.setPreferredSize(new Dimension(100,100));
			fillLocalizedStrings();
			ToolTipManager.sharedInstance().registerComponent(list);
			SwingUtilities.invokeLater(()->searchString.grabFocus());
		}
	}

	@Override
	public void localeChanged(Locale oldLocale, Locale newLocale) throws LocalizationException {
		fillLocalizedStrings();
	}

	private void showFacet(final boolean visible) {
		if (visible) {
			try{this.filter.join(ItemAndSelection.of(DbUtil.extractUniqueTags(conn)));
			} catch (SQLException e) {
				logger.message(Severity.error,e.getLocalizedMessage());
			}
		}
		this.abf.setVisible(visible);
	}

	public <T> AutoBuiltForm<T,?> buildFacets(final T instance, final int width, final int height) {
		try{final ContentMetadataInterface	mdi = ContentModelFactory.forAnnotatedClass(instance.getClass());
			final AutoBuiltForm<T,?>		abf = new AutoBuiltForm<>(mdi,localizer,PureLibSettings.INTERNAL_LOADER,instance,(FormManager<Object,T>)instance);
			
			for (Module m : abf.getUnnamedModules()) {
				instance.getClass().getModule().addExports(instance.getClass().getPackageName(),m);
			}
			abf.setPreferredSize(new Dimension(width,height));
			return abf;
		} catch (LocalizationException | ContentException e) {
			logger.message(Severity.error,e.getLocalizedMessage());
			return null;
		} 
	}
	
	private void fillList(final String text) {
		final String	truncated = text.trim();
		
		if (!truncated.isEmpty() || abf.isVisible()) {
			try{final List<SearchResult>	result = new ArrayList<>();
				final String[]				selected = ItemAndSelection.extract(true, filter.tags);
				final Set<Long>				idsFound = new HashSet<>();
				
				try(final PreparedStatement	ps = conn.prepareStatement("select ct_id, ct_Name, ct_Comment, ct_Tags from helen.contenttree where " + buildWhere("ct_Name", !truncated.isEmpty(), "ct_Tags", selected.length))) {
					bindParameters(ps, truncated, selected);
					try(final ResultSet	rs = ps.executeQuery()) {
						while (rs.next()) {
							final long	id = rs.getLong("ct_Id");
							
							if (!idsFound.contains(id)) {
								idsFound.add(id);
								result.add(new SearchResult(id, SearchResult.Where.IN_TREE_NAME, rs.getString("ct_Name"), rs.getString("ct_Comment"), rs.getString("ct_Tags")));
							}
						}
					}
				}
	
				try(final PreparedStatement	ps = conn.prepareStatement("select ct_id, ct_Name, ct_Comment, ct_Tags from helen.contenttree where " + buildWhere("ct_Comment", !truncated.isEmpty(), "ct_Tags", selected.length))) {
					bindParameters(ps, truncated, selected);
					try(final ResultSet	rs = ps.executeQuery()) {
						while (rs.next()) {
							final long	id = rs.getLong("ct_Id");
							
							if (!idsFound.contains(id)) {
								idsFound.add(id);
								result.add(new SearchResult(id, SearchResult.Where.IN_TREE_COMMENT, rs.getString("ct_Name"), rs.getString("ct_Comment"), rs.getString("ct_Tags")));
							}
						}
					}
				}
	
				try(final PreparedStatement	ps = conn.prepareStatement("select ci_id, '' as ci_Name, ci_Comment, ci_Tags from helen.contentimage where " + buildWhere("ci_Comment", !truncated.isEmpty(), "ci_Tags", selected.length))) {
					bindParameters(ps, truncated, selected);
					try(final ResultSet	rs = ps.executeQuery()) {
						while (rs.next()) {
							final long	id = rs.getLong("ci_Id");
							
							if (!idsFound.contains(id)) {
								idsFound.add(id);
								result.add(new SearchResult(id, SearchResult.Where.IN_IMAGE_COMMENT, rs.getString("ci_Name"), rs.getString("ci_Comment"), rs.getString("ci_Tags")));
							}
						}
					}
				}
				
				if (result.isEmpty()) {
					logger.message(Severity.warning, ()->localizer.getValue(KEY_SEARCH_NOTFOUND));
				}
				else {
					listModel.removeAllElements();
					listModel.addAll(result);
					list.grabFocus();
					list.setSelectedIndex(0);
					logger.message(Severity.info, ()->localizer.getValue(KEY_SEARCH_FOUND, result.size()));
				}
			} catch (SQLException e) {
				logger.message(Severity.error, e.getLocalizedMessage(), e);
			}
		}
	}

	private String buildWhere(final String fieldTS, final boolean useTS, final String fieldFacet, final int facetSize) {
		final StringBuilder	sb = new StringBuilder();
		
		if (useTS) {
			sb.append(fieldTS).append(" @@ to_tsquery(?) ");
			if (facetSize > 0) {
				sb.append(" and ");
			}
		}
		if (facetSize > 0) {
			String	prefix = "";
			
			for (int index = 0; index < facetSize; index++) {
				sb.append(prefix).append("position( ? in ").append(fieldFacet).append(")");
				
				prefix = " + ";
			}
			sb.append(" > 0");
		}
		return sb.toString();
	}

	private void bindParameters(final PreparedStatement ps, final String valueTS, final String[] valuesFacet) throws SQLException {
		int		index = 1;
		
		if (!valueTS.isEmpty()) {
			ps.setString(index++, valueTS);
		}
		for (String item : valuesFacet) {
			ps.setString(index++, item);
		}
	}
	
	private void gotoItem() {
		if (!list.isSelectionEmpty()) {
			selectCallback.accept(listModel.getElementAt(list.getSelectedIndex()));
		}
	}

	
	private void fillLocalizedStrings() throws LocalizationException {
		searchCaption.setText(localizer.getValue(KEY_SEARCH_CAPTION));
		closeButton.setToolTipText(localizer.getValue(KEY_SEARCH_CLOSE_TT));
		searchLabel.setText(localizer.getValue(KEY_SEARCH_ENTER));
		searchString.setToolTipText(localizer.getValue(KEY_SEARCH_ENTER_TT));
		searchButton.setToolTipText(localizer.getValue(KEY_SEARCH_FIND));
	}

	public static class SearchResult {
		public static enum Where {
			IN_TREE_NAME, IN_TREE_COMMENT, IN_IMAGE_COMMENT
		}
		
		public final long	id;
		public final Where	location;
		public final String	name;
		public final String	comment;
		public final String	tags;
		
		public SearchResult(final long id, final Where location, final String name, final String comment, final String tags) {
			this.id = id;
			this.location = location;
			this.name = name;
			this.comment = comment;
			this.tags = tags;
		}

		@Override
		public String toString() {
			return "SearchResult [id=" + id + ", location=" + location + ", name=" + name + ", comment=" + comment + ", tags=" + tags + "]";
		}
	}

	private static class JListWithTooltips extends JList<SearchResult> {
		private static final long serialVersionUID = 1L;
		
		private final Localizer	localizer;
		
		public JListWithTooltips(final Localizer localizer, final ListModel<SearchResult> model) {
			super(model);
			this.localizer = localizer;
		}
		
		@Override
		public String getToolTipText(final MouseEvent event) {
			final int	index = locationToIndex(event.getPoint());
			
			if (index >= 0) {
				final SearchResult	sr = getModel().getElementAt(index);
				
				try{return localizer.getValue(KEY_SEARCH_TOOLTIP, sr.name, sr.comment, sr.tags);
				} catch (LocalizationException e) {
					return super.getToolTipText(event);
				}
			}
			else {
				return super.getToolTipText(event);
			}
		}
		
	}

}
