package chav1961.imagedb.db;


import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.exceptions.PreparationException;
import chav1961.purelib.enumerations.ContinueMode;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.ModelUtils;
import chav1961.purelib.model.SchemaContainer;
import chav1961.purelib.model.ModelUtils.ModelComparisonCallback.DifferenceLocalization;
import chav1961.purelib.model.ModelUtils.ModelComparisonCallback.DifferenceType;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.model.interfaces.ContentMetadataInterface.ContentNodeMetadata;
import chav1961.purelib.sql.model.SQLModelUtils;
import chav1961.purelib.streams.JsonStaxParser;
import chav1961.purelib.ui.swing.useful.JContentMetadataEditor;
import chav1961.purelib.ui.swing.useful.JContentMetadataEditor.EditorMode;

public class DbManager {
	public static final String	APPLICATION_SCHEMA = "helen";

	public static final String	KEY_CONN_CLEAR = "dbmanager.message.clear";
	public static final String	KEY_CONN_CORRUPTED = "dbmanager.message.corrupted";
	public static final String	KEY_CONN_OLD = "dbmanager.message.old";
	public static final String	KEY_CONN_OK = "dbmanager.message.ok";
	
	
	private final Connection	conn;

	public static enum DbState {
		CLEAN, STRUCTURE_CORRUPTED, STRUCTURE_OLD, STRUCTURE_VALID 
	}
	
	public DbManager(final Connection conn) throws NullPointerException {
		if (conn == null) {
			throw new NullPointerException("Connection can't be null"); 
		}
		else {
			this.conn = conn;
		}
	}
	
	public DbState getDbState() throws SQLException {
		try{return validateDbStructure(conn.getMetaData(), loadModel());
		} catch (IOException | ContentException e) {
			throw new SQLException(e.getLocalizedMessage(), e);
		}
	}
	
	public ContentNodeMetadata loadModel() throws IOException {
		try(final InputStream		is = this.getClass().getResourceAsStream("model.json");
			final Reader			rdr = new InputStreamReader(is);
			final JsonStaxParser	parser = new JsonStaxParser(rdr)) {

			parser.next();
			return ModelUtils.deserializeFromJson(parser);
		}
	}
	
	public void createDatabaseByModel() throws SQLException {
		try{
			SQLModelUtils.createDatabaseByModel(conn, loadModel());
		} catch (IOException e) {
			throw new SQLException(e.getLocalizedMessage(), e); 
		}
	}

	public void backupDatabaseByModel(final ZipOutputStream zos) throws SQLException {
		if (zos == null) {
			throw new NullPointerException("ZIP output stream can't be null");
		}
		else {
			try{
				SQLModelUtils.backupDatabaseByModel(conn, loadModel(), zos);
			} catch (IOException e) {
				throw new SQLException(e.getLocalizedMessage(), e); 
			}
		}
	}
	
	public void removeDatabaseByModel() throws SQLException {
		try{
			SQLModelUtils.removeDatabaseByModel(conn, loadModel());
		} catch (IOException e) {
			throw new SQLException(e.getLocalizedMessage(), e); 
		}
	}
	
	private DbState validateDbStructure(final DatabaseMetaData metaData, final ContentNodeMetadata model) throws ContentException {
		// TODO Auto-generated method stub
		final ContentMetadataInterface	mdi = ContentModelFactory.forDBContentDescription(metaData, null, model.getName());

		if (mdi != null) {
			
			try {
				final JContentMetadataEditor 	ed = new JContentMetadataEditor(PureLibSettings.PURELIB_LOCALIZER);
				
				ed.setEditorMode(EditorMode.NODE_AND_SUBTREE);
				ed.setPreferredSize(new Dimension(600,400));
				ed.setValue(mdi.getRoot());
				JOptionPane.showMessageDialog(null, new JScrollPane(ed));
			} catch (LocalizationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			ModelUtils.compare(mdi.getRoot(), model, (left, right, diff, details) -> {
				System.err.println(">>> left="+left+", right="+right+", diff="+diff+", details="+details);
				return ContinueMode.CONTINUE;
			});
			return DbState.CLEAN;
		}
		else {
			return DbState.CLEAN;
		}
	}
}
