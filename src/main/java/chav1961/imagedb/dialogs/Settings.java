package chav1961.imagedb.dialogs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ServiceLoader;

import chav1961.imagedb.Application;
import chav1961.imagedb.db.DbManager;
import chav1961.purelib.basic.SimpleURLClassLoader;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.Action;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.swing.useful.JLocalizedOptionPane;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.Settings/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="settings.title",tooltip="settings.title.tt",help="settings.title.help")
@Action(resource=@LocaleResource(value="settings.action.test",tooltip="settings.action.test.tt"),actionString="test")
public class Settings implements FormManager<Object,Settings>, ModuleAccessor {
	public static final String	PROP_DRIVER = "driver";
	public static final String	PROP_CONN_STRING = "connString";
	public static final String	PROP_USER = "user";
	
	public static final String	KEY_ILLEGAL_DRIVER = "settings.illegal.driver";
	public static final String	KEY_DATABASE_ERROR = "settings.database.error";
	public static final String	KEY_IO_ERROR = "settings.io.error";
	public static final String	KEY_TEST_OK = "settings.test.ok";
	
	private final LoggerFacade 	logger;
	private final Application 	app;
	
	@LocaleResource(value="settings.driver",tooltip="settings.driver.tt")
	@Format("30ms")
	public File		driver = new File("./current.jar");
	@LocaleResource(value="settings.connectionstring",tooltip="settings.connectionstring.tt")
	@Format("30ms")
	public URI		connectionString = URI.create("jdbc:/");
	@LocaleResource(value="settings.user",tooltip="settings.user.tt")
	@Format("30ms")
	public String	user = "unknown";

	public Settings(final LoggerFacade 	logger, final Application app) {
		this.logger = logger;
		this.app = app;
	}
	
	@Override
	public RefreshMode onField(final Settings inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		// TODO Auto-generated method stub
		switch (fieldName) {
			case "driver" :
				if (driver.exists() && driver.isFile() && driver.getName().endsWith(".jar") && driver.canRead()) {
					return RefreshMode.DEFAULT;
				}
				else {
					getLogger().message(Severity.warning, "Driver jar selected ["+driver+"] not exists, is not a file, doesn't end with '*.jar' or has no access rights for you");
					return RefreshMode.REJECT;
				}
			case "connectionString" :
			case "user" :
			default :
		}
		return RefreshMode.DEFAULT;
	}
	
	@Override
	public void allowUnnamedModuleAccess(final Module... unnamedModules) {
		for (Module item : unnamedModules) {
			this.getClass().getModule().addExports(this.getClass().getPackageName(),item);
		}
	}
	
	@Override
	public RefreshMode onAction(final Settings inst, final Object id, final String actionName, final Object parameter) throws FlowException, LocalizationException {
		switch (actionName) {
			case "app:action:/Settings.test" :
				try(final SimpleURLClassLoader	sucl = new SimpleURLClassLoader(new URL[] {driver.toURI().toURL()})) {
					final Properties	props = Utils.mkProps("user", user);
					boolean				passwordFilled = false;
					
					for(Driver	drv : ServiceLoader.load(Driver.class, sucl)) {
						try{
							if (drv.acceptsURL(connectionString.toString())) {
								if (!passwordFilled) {
									final AskPassword	ap = new AskPassword(logger);
									
									if (app.ask(ap, 200, 60)) {
										props.setProperty("password", new String(ap.password));
										passwordFilled = true;
									}
									else {
										return RefreshMode.DEFAULT;
									}
								}
								try(final Connection	conn = drv.connect(connectionString.toString(), props)) {
									final DbManager		dbm = new DbManager(conn);
									
									switch (dbm.getDbState()) {
										case CLEAN					:
											getLogger().message(Severity.warning, ()->app.localizer.getValue(KEY_TEST_OK, app.localizer.getValue(DbManager.KEY_CONN_CLEAR)));
											break;
										case STRUCTURE_CORRUPTED	:
											getLogger().message(Severity.warning, ()->app.localizer.getValue(KEY_TEST_OK, app.localizer.getValue(DbManager.KEY_CONN_CORRUPTED)));
											break;
										case STRUCTURE_OLD			:
											getLogger().message(Severity.warning, ()->app.localizer.getValue(KEY_TEST_OK, app.localizer.getValue(DbManager.KEY_CONN_OLD)));
											break;
										case STRUCTURE_VALID		:
											getLogger().message(Severity.info, ()->app.localizer.getValue(KEY_TEST_OK, app.localizer.getValue(DbManager.KEY_CONN_OK)));
											break;
										default: throw new UnsupportedOperationException("Database state ["+dbm.getDbState()+"] is not supported yet");
									}
									return RefreshMode.DEFAULT;
								}
							}
							getLogger().message(Severity.error, ()->app.localizer.getValue(KEY_ILLEGAL_DRIVER, driver.getAbsolutePath(), connectionString.toString()));
						} catch (SQLException e) {
							getLogger().message(Severity.error, ()->app.localizer.getValue(KEY_DATABASE_ERROR, e.getLocalizedMessage()));
						}
					}
				} catch (IOException e) {
					getLogger().message(Severity.error, ()->app.localizer.getValue(KEY_IO_ERROR, e.getLocalizedMessage()));
				}
				return RefreshMode.DEFAULT;
			default : throw new UnsupportedOperationException("Action name ["+actionName+"] is not supported yet");
		}
	}
	
	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
	
	public void load(final SubstitutableProperties props) throws NullPointerException {
		if (props == null) {
			throw new NullPointerException("Properties to load content from can't be null"); 
		}
		else {
			driver = props.getProperty(PROP_DRIVER, File.class, "./current.jar");
			connectionString = props.getProperty(PROP_CONN_STRING, URI.class, "jdbc:/");
			user = props.getProperty(PROP_USER, String.class, "unknown");
		}
	}
	
	public void save(final SubstitutableProperties props) throws NullPointerException {
		if (props == null) {
			throw new NullPointerException("Properties to load content from can't be null"); 
		}
		else {
			props.setProperty(PROP_DRIVER, driver.getAbsolutePath());
			props.setProperty(PROP_CONN_STRING, connectionString.toString());
			props.setProperty(PROP_USER, user);
		}
	}
}
