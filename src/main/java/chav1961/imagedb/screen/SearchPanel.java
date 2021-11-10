package chav1961.imagedb.screen;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.ui.inner.InternalConstants;
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

	private final Localizer							localizer;
	private final LoggerFacade						logger;
	private final Connection						conn;
	private final Consumer<SearchResult>			selectCallback;
	private final JLabel							searchCaption = new JLabel("", JLabel.CENTER);
	private final JButton							closeButton = new JButton(InternalConstants.ICON_CLOSE);
	private final JLabel							searchLabel = new JLabel("");
	private final JTextField						searchString = new JTextField();
	private final JButton							searchButton = new JButton(InternalConstants.ICON_SEARCH);
	private final DefaultListModel<SearchResult>	listModel = new DefaultListModel<>();
	private final JList<SearchResult>				list = new JList<>(listModel);
	
	public SearchPanel(final Localizer localizer, final LoggerFacade logger, final Connection conn, final Consumer<SearchPanel> closeCallback, final Consumer<SearchResult> selectCallback) throws NullPointerException, LocalizationException {
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
			this.selectCallback = selectCallback;
			
			closeButton.addActionListener((e)->closeCallback.accept(SearchPanel.this));
			
			setLayout(new BorderLayout());
	
			closeButton.setPreferredSize(new Dimension(InternalConstants.ICON_CLOSE.getIconWidth(),InternalConstants.ICON_CLOSE.getIconHeight()));
			searchString.setColumns(30);
			searchButton.setPreferredSize(new Dimension(InternalConstants.ICON_SEARCH.getIconWidth()+4,InternalConstants.ICON_SEARCH.getIconHeight()+4));
			searchButton.addActionListener((e)->fillList(searchString.getText()));
		
			list.setCellRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					final JLabel		label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					final SearchResult	sr = (SearchResult)value;
					
					label.setText(sr.text);
					return label;
				}
			});		
			
			final JPanel	caption = new JPanel(new BorderLayout()), close = new JPanel(), search = new JPanel(), top = new JPanel(new GridLayout(2,1));  
			
			close.add(closeButton);
			caption.add(searchCaption, BorderLayout.CENTER);
			caption.add(close, BorderLayout.EAST);
			caption.setBorder(new LineBorder(Color.BLACK));
			search.add(searchLabel);
			search.add(searchString);
			search.add(searchButton);
			top.add(caption);
			top.add(search);
			
			add(top, BorderLayout.NORTH);
			add(new JScrollPane(list), BorderLayout.CENTER);
			
			SwingUtils.assignActionKey(this, JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, SwingUtils.KS_ACCEPT, (e)->fillList(searchString.getText()), SwingUtils.ACTION_ACCEPT);
			SwingUtils.assignActionKey(this, JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, SwingUtils.KS_EXIT, (e)->closeCallback.accept(SearchPanel.this), SwingUtils.ACTION_EXIT);
			SwingUtils.assignActionKey(list, SwingUtils.KS_ACCEPT, (e)->gotoItem(), SwingUtils.ACTION_ACCEPT);
			
			fillLocalizedStrings();
			SwingUtilities.invokeLater(()->searchString.grabFocus());
		}
	}

	@Override
	public void localeChanged(Locale oldLocale, Locale newLocale) throws LocalizationException {
		fillLocalizedStrings();
	}

	private void fillList(final String text) {
		final String	truncated = text.trim();
		
		if (!truncated.isEmpty()) {
			try{final List<SearchResult>	result = new ArrayList<>();
	
				try(final PreparedStatement	ps = conn.prepareStatement("select ct_id, ct_Name from helen.contenttree where ct_Name @@ to_tsquery(?)")) {
					
					ps.setString(1, truncated);
					try(final ResultSet	rs = ps.executeQuery()) {
						while (rs.next()) {
							result.add(new SearchResult(rs.getLong("ct_Id"), SearchResult.Where.IN_TREE_NAME, rs.getString("ct_Name")));
						}
					}
				}
	
				try(final PreparedStatement	ps = conn.prepareStatement("select ct_id, ct_Comment from helen.contenttree where ct_Comment @@ to_tsquery(?)")) {
					
					ps.setString(1, truncated);
					try(final ResultSet	rs = ps.executeQuery()) {
						while (rs.next()) {
							result.add(new SearchResult(rs.getLong("ct_Id"), SearchResult.Where.IN_TREE_COMMENT, rs.getString("ct_Comment")));
						}
					}
				}
	
				try(final PreparedStatement	ps = conn.prepareStatement("select ci_id, ci_Comment from helen.contentimage where ci_Comment @@ to_tsquery(?)")) {
					
					ps.setString(1, truncated);
					try(final ResultSet	rs = ps.executeQuery()) {
						while (rs.next()) {
							result.add(new SearchResult(rs.getLong("ci_Id"), SearchResult.Where.IN_IMAGE_COMMENT, rs.getString("ci_Comment")));
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
		public final String	text;
		
		public SearchResult(final long id, final Where location, final String text) {
			this.id = id;
			this.location = location;
			this.text = text;
		}

		@Override
		public String toString() {
			return "SearchResult [id=" + id + ", location=" + location + ", text=" + text + "]";
		}
	}
}
