package chav1961.imagedb;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.SystemTray;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import chav1961.imagedb.db.DbManager;
import chav1961.imagedb.db.DbUtil;
import chav1961.imagedb.dialogs.AskPassword;
import chav1961.imagedb.dialogs.FilterItem;
import chav1961.imagedb.dialogs.Settings;
import chav1961.imagedb.screen.Navigator;
import chav1961.imagedb.screen.SearchPanel;
import chav1961.imagedb.screen.SearchPanel.SearchResult;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SimpleURLClassLoader;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.exceptions.PreparationException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.i18n.LocalizerFactory;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.i18n.interfaces.SupportedLanguages;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.nanoservice.NanoServiceFactory;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.ItemAndSelection;
import chav1961.purelib.ui.swing.AutoBuiltForm;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.interfaces.OnAction;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog;
import chav1961.purelib.ui.swing.useful.JSimpleSplash;
import chav1961.purelib.ui.swing.useful.JStateString;
import chav1961.purelib.ui.swing.useful.JSystemTray;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog.FilterCallback;

public class Application extends JFrame implements LocaleChangeListener {
	private static final long 				serialVersionUID = -2663340436788182341L;
	public static final String				ARG_HELP_PORT = "helpPort";
	
	public static final String				KEY_TITLE_APPLICATION = "application.title";
	public static final String				KEY_TITLE_HELP_ABOUT_APPLICATION = "application.help.title";
	public static final String				KEY_HELP_ABOUT_APPLICATION = "application.help";
	public static final String				KEY_APPLICATION_READY = "application.ready";
	public static final String				KEY_APPLICATION_COMPLETED = "application.completed";
	public static final String				KEY_SETTINGS_SAVED = "application.settings.saved";
	
	public static final String				PROP_INI_FILE = ".imagedb.props";
	
	public final Localizer			 		localizer;
	private final ContentMetadataInterface	xda;
	private final SubstitutableProperties	props = new SubstitutableProperties();
	private final JMenuBar					menu;
	private final JPopupMenu				trayMenu;
	private final int						localHelpPort;
	private final Settings					settings;
	private final CountDownLatch			latch;
	private final JStateString				stateString;
	private final JLabel					background = new JLabel();
	private FilterItem						filter = null;
	private SimpleURLClassLoader			classLoader = null;
	private Connection						conn = null;
	private Navigator						navigator = null;
	private SearchPanel						searchPanel = null;
	
	public Application(final ContentMetadataInterface xda, final int helpPort, final Localizer parentLocalizer, final LoggerFacade logger, final CountDownLatch latch) throws NullPointerException, IllegalArgumentException, EnvironmentException, IOException, FlowException, SyntaxException, PreparationException, ContentException {
		if (xda == null) {
			throw new NullPointerException("Application descriptor can't be null");
		}
		else if (parentLocalizer == null) {
			throw new NullPointerException("Localizer can't be null");
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null");
		}
		else if (latch == null) {
			throw new NullPointerException("Latch can't be null");
		}
		else {
			this.xda = xda;
			this.localizer = LocalizerFactory.getLocalizer(xda.getRoot().getLocalizerAssociated());
			this.localHelpPort = helpPort;
			this.latch = latch;
			this.stateString = new JStateString(this.localizer,10,true);
			this.settings = new Settings(stateString, this);

			stateString.setAutomaticClearTime(Severity.error,1,TimeUnit.MINUTES);
			stateString.setAutomaticClearTime(Severity.warning,15,TimeUnit.SECONDS);
			stateString.setAutomaticClearTime(Severity.info,5,TimeUnit.SECONDS);
			
			parentLocalizer.push(localizer);
			localizer.addLocaleChangeListener(this);
			
			if (props.tryLoad(new File(PROP_INI_FILE), stateString)) {
				settings.load(props);
			}
			
			this.menu = SwingUtils.toJComponent(xda.byUIPath(URI.create("ui:/model/navigation.top.mainmenu")),JMenuBar.class); 
			SwingUtils.assignActionListeners(this.menu,this);
			this.trayMenu = SwingUtils.toJComponent(xda.byUIPath(URI.create("ui:/model/navigation.top.traymenu")),JPopupMenu.class); 
			SwingUtils.assignActionListeners(this.trayMenu,this);
			
			background.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
			getContentPane().add(this.menu, BorderLayout.NORTH);
			getContentPane().add(background, BorderLayout.CENTER);
			getContentPane().add(stateString, BorderLayout.SOUTH);

			SwingUtils.assignExitMethod4MainWindow(this,()->exitApplication());
			SwingUtils.centerMainWindow(this,0.75f);

			((JMenuItem)SwingUtils.findComponentByName(menu, "menu.file.disconnect")).setEnabled(false);
			((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.search")).setEnabled(false);
			((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.filter")).setEnabled(false);
			((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.clearfilter")).setEnabled(false);
			fillLocalizedStrings(localizer.currentLocale().getLocale(),localizer.currentLocale().getLocale());
			stateString.message(Severity.info, ()->localizer.getValue(KEY_APPLICATION_READY));
			pack();
		}
	}
	
	@Override
	public void localeChanged(final Locale oldLocale, final Locale newLocale) throws LocalizationException {
		fillLocalizedStrings(oldLocale,newLocale);
		SwingUtils.refreshLocale(menu, oldLocale, newLocale);
		if (navigator != null) {
			SwingUtils.refreshLocale(navigator, oldLocale, newLocale);
		}
		if (searchPanel != null) {
			SwingUtils.refreshLocale(searchPanel, oldLocale, newLocale);
		}
	}

	public <T> boolean ask(final T instance, final int width, final int height) {
		try{final ContentMetadataInterface	mdi = ContentModelFactory.forAnnotatedClass(instance.getClass());
		
			try(final AutoBuiltForm<T>		abf = new AutoBuiltForm<T>(mdi,localizer,PureLibSettings.INTERNAL_LOADER,instance,(FormManager<Object,T>)instance)) {
				
				for (Module m : abf.getUnnamedModules()) {
					instance.getClass().getModule().addExports(instance.getClass().getPackageName(),m);
				}
				abf.setPreferredSize(new Dimension(width,height));
				return AutoBuiltForm.ask(this,localizer,abf);
			}
		} catch (LocalizationException | ContentException e) {
			stateString.message(Severity.error,e.getLocalizedMessage());
			return false;
		} 
	}

	public void withConnection(final Function<Connection, Void> func) {
		try(final SimpleURLClassLoader	sucl = new SimpleURLClassLoader(new URL[] {settings.driver.toURI().toURL()})) {
			final Properties	props = Utils.mkProps("user", settings.user);
			boolean				passwordFilled = false;
			
			for(Driver	drv : ServiceLoader.load(Driver.class, sucl)) {
				try{
					if (drv.acceptsURL(settings.connectionString.toString())) {
						if (!passwordFilled) {
							final AskPassword	ap = new AskPassword(stateString);
							
							if (ask(ap, 200, 40)) {
								props.setProperty("password", new String(ap.password));
								passwordFilled = true;
							}
							else {
								return;
							}
						}
						try(final Connection	conn = drv.connect(settings.connectionString.toString(), props)) {
							conn.setAutoCommit(true);
							func.apply(conn);
						}
						return;
					}
					stateString.message(Severity.error, ()->localizer.getValue(Settings.KEY_ILLEGAL_DRIVER, settings.driver.getAbsolutePath(), settings.connectionString.toString()));
				} catch (SQLException e) {
					stateString.message(Severity.error, ()->localizer.getValue(Settings.KEY_DATABASE_ERROR, e.getLocalizedMessage()));
				}
			}
		} catch (IOException e) {
			stateString.message(Severity.error, ()->localizer.getValue(Settings.KEY_IO_ERROR, e.getLocalizedMessage()));
		}
	}
	
	@OnAction("action:/file.connect")
	private void connect() throws MalformedURLException {
		final Properties	props = Utils.mkProps("user", settings.user);
		boolean				passwordFilled = false;
		
		classLoader = new SimpleURLClassLoader(new URL[] {settings.driver.toURI().toURL()});
		for (Driver	drv : ServiceLoader.load(Driver.class, classLoader)) {
			try{
				if (drv.acceptsURL(settings.connectionString.toString())) {
					if (!passwordFilled) {
						final AskPassword	ap = new AskPassword(stateString);
						
						if (ask(ap, 200, 50)) {
							props.setProperty("password", new String(ap.password));
							passwordFilled = true;
						}
						else {
							classLoader.close();
							classLoader = null;
							return;
						}
					}
					conn = drv.connect(settings.connectionString.toString(), props);
					conn.setAutoCommit(true);
					getContentPane().remove(background);
					getContentPane().add(navigator = new Navigator(localizer, stateString, conn, xda, this), BorderLayout.CENTER);
					
					((JMenuItem)SwingUtils.findComponentByName(menu, "menu.file.connect")).setEnabled(false);
					((JMenuItem)SwingUtils.findComponentByName(menu, "menu.file.disconnect")).setEnabled(true);
					((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.search")).setEnabled(true);
					((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.filter")).setEnabled(true);
					((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.clearfilter")).setEnabled(true);
					stateString.message(Severity.info, ()->localizer.getValue(KEY_APPLICATION_COMPLETED));
					return;
				}
				stateString.message(Severity.error, ()->localizer.getValue(Settings.KEY_ILLEGAL_DRIVER, settings.driver.getAbsolutePath(), settings.connectionString.toString()));
			} catch (SQLException | IOException e) {
				stateString.message(Severity.error, ()->localizer.getValue(Settings.KEY_DATABASE_ERROR, e.getLocalizedMessage()));
			}
		}
	}
	
	@OnAction("action:/file.disconnect")
	private void disconnect() throws SQLException, IOException {
		getContentPane().remove(navigator);
		getContentPane().add(background, BorderLayout.CENTER);
		navigator = null;
		conn.close();
		conn = null;
		classLoader.close();
		classLoader = null;
		if (searchPanel != null) {
			getContentPane().remove(searchPanel);
			pack();
			searchPanel = null;
		}
		((JMenuItem)SwingUtils.findComponentByName(menu, "menu.file.connect")).setEnabled(true);
		((JMenuItem)SwingUtils.findComponentByName(menu, "menu.file.disconnect")).setEnabled(false);
		((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.search")).setEnabled(false);
		((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.filter")).setEnabled(false);
		((JMenuItem)SwingUtils.findComponentByName(menu, "menu.tools.clearfilter")).setEnabled(false);
		stateString.message(Severity.info, ()->localizer.getValue(KEY_APPLICATION_COMPLETED));
	}
	
	@OnAction("action:/file.hide")
	private void hideApplication () {
		setVisible(false);
	}
	
	@OnAction("action:/file.quit")
	private void exitApplication() {
		try{if (conn != null) {
				try{conn.close();
					conn = null;
				} catch (SQLException e) {
				}
			}
			if (classLoader != null) {
				try{classLoader.close();
					classLoader = null;
				} catch (IOException e) {
				}
			}
			setVisible(false);
			dispose();
		} finally {
			latch.countDown();
		}
	}
	
	@OnAction("action:/tools.search")
	private void search() {
		try{getContentPane().add(searchPanel = new SearchPanel(localizer, stateString, conn, (p)->{
				getContentPane().remove(p);
				searchPanel = null;
				pack();
			}, (r) ->select(r)), BorderLayout.EAST);
			pack();
		} catch (LocalizationException | SQLException e) {
			stateString.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	@OnAction("action:/tools.filter")
	private void filter() {
		try{if (this.filter == null) {
				this.filter = new FilterItem(stateString, ItemAndSelection.of(DbUtil.extractUniqueTags(conn)));
			}
			final FilterItem	clone = filter.clone();
			
			this.filter.join(ItemAndSelection.of(DbUtil.extractUniqueTags(conn)));
			if (!ask(filter, 200, 200)) {
				this.filter = clone;
			}
		} catch (SQLException e) {
			stateString.message(Severity.error, e.getLocalizedMessage(), e);
		}
	}

	@OnAction("action:/tools.clearfilter")
	private void clearFilter() {
		filter = null;
	}
	
	@OnAction("action:/tools.db.install")
	private void dbInstall() {
		withConnection((conn)->{
			try{new DbManager(conn).createDatabaseByModel();
				stateString.message(Severity.info, ()->localizer.getValue(KEY_APPLICATION_COMPLETED));
			} catch (SQLException e) {
				stateString.message(Severity.error, e.getLocalizedMessage(), e);
			}
			return null;
		});
	}
	
	@OnAction("action:/tools.db.upgrade")
	private void dbUpgrade() {
		
	}
	
	@OnAction("action:/tools.db.backup")
	private void dbBackup() {
		withConnection((conn)->{
			try(final FileSystemInterface	fsi = FileSystemFactory.createFileSystem(URI.create("fsys:file:/"))) {
				for (String item : JFileSelectionDialog.select((Frame)null, localizer, fsi, JFileSelectionDialog.OPTIONS_FOR_SAVE | JFileSelectionDialog.OPTIONS_CAN_SELECT_FILE, FilterCallback.of("ZIP files", "*.zip"))) {
					try(final FileSystemInterface	file = fsi.open(item.endsWith(".zip") ? item : item+".zip").create();
						final OutputStream			os = file.write();
						final ZipOutputStream		zos = new ZipOutputStream(os)) {
						
						new DbManager(conn).backupDatabaseByModel(zos);
						stateString.message(Severity.info, ()->localizer.getValue(KEY_APPLICATION_COMPLETED));
						break;
					}
				}
			} catch (IOException | SQLException | LocalizationException e) {
				stateString.message(Severity.error, e.getLocalizedMessage(), e);
			}
			return null;
		});
	}
	
	@OnAction("action:/tools.db.restore")
	private void dbRestore() {
		
	}

	@OnAction("action:/tools.db.uninstall")
	private void dbRemove() {
		withConnection((conn)->{
			try{new DbManager(conn).removeDatabaseByModel();
				stateString.message(Severity.info, ()->localizer.getValue(KEY_APPLICATION_COMPLETED));
			} catch (SQLException e) {
				stateString.message(Severity.error, e.getLocalizedMessage(), e);
			}
			return null;
		});
	}
	
	@OnAction("action:/tools.settings")
	private void settings() {
		if (ask(settings,350,120)) {
			settings.save(props);
			try(final FileOutputStream	fos = new FileOutputStream(new File(PROP_INI_FILE))) {
				
				props.store(fos, "");
				stateString.message(Severity.info, ()->localizer.getValue(KEY_SETTINGS_SAVED));
			} catch (IOException e) {
				stateString.message(Severity.error, e.getLocalizedMessage());
			}
		}
		else {
			settings.load(props);
		}
	}

	@OnAction("action:/help.guide")
	private void guide() {
	}

	@OnAction("action:/help.update")
	private void update() {
	}

	@OnAction("action:/help.about")
	private void about() throws URISyntaxException {
		SwingUtils.showAboutScreen(this, localizer, KEY_TITLE_HELP_ABOUT_APPLICATION, KEY_HELP_ABOUT_APPLICATION, this.getClass().getResource("avatar.jpg").toURI(), new Dimension(300,300));
	}
	
	@OnAction("action:builtin:/builtin.languages")
	private void changeLang (final Hashtable<String,String[]> langs) throws LocalizationException {
		localizer.setCurrentLocale(SupportedLanguages.valueOf(langs.get("lang")[0]).getLocale());
	}

	private void select(final SearchResult sr) {
		switch (sr.location) {
			case IN_IMAGE_COMMENT	:
				navigator.selectImage(sr.id);
				break;
			case IN_TREE_NAME : case IN_TREE_COMMENT :
				navigator.selectTree(sr.id);
				break;
			default:
				break;
		}
	}
	
	private void fillLocalizedStrings(Locale oldLocale, Locale newLocale) throws LocalizationException {
		setTitle(localizer.getValue(KEY_TITLE_APPLICATION));
	}
	
	
	public static void main(final String[] args) throws IOException, EnvironmentException, FlowException, ContentException, HeadlessException, URISyntaxException {
		final ArgParser		parser = new ApplicationArgParser().parse(args);
		final SubstitutableProperties		props = new SubstitutableProperties(Utils.mkProps(
												 NanoServiceFactory.NANOSERVICE_PORT, parser.getValue(ARG_HELP_PORT,String.class)
												,NanoServiceFactory.NANOSERVICE_ROOT, "fsys:xmlReadOnly:root://chav1961.imagedb.Application/chav1961/imagedb/helptree.xml"
												,NanoServiceFactory.NANOSERVICE_CREOLE_PROLOGUE_URI, Application.class.getResource("prolog.cre").toString() 
												,NanoServiceFactory.NANOSERVICE_CREOLE_EPILOGUE_URI, Application.class.getResource("epilog.cre").toString() 
											));
		
		try(final JSimpleSplash					jss = new JSimpleSplash(Application.class.getResource("splash.png"));) {
			jss.start("...");
			
			try(final InputStream				is = Application.class.getResourceAsStream("application.xml");
				final NanoServiceFactory		service = new NanoServiceFactory(PureLibSettings.CURRENT_LOGGER,props);
				final LoggerFacade				logger = PureLibSettings.CURRENT_LOGGER) {
				final ContentMetadataInterface	xda = ContentModelFactory.forXmlDescription(is);
				final CountDownLatch			latch = new CountDownLatch(1);
				final Application				app = new Application(xda,parser.getValue(ARG_HELP_PORT,int.class),PureLibSettings.PURELIB_LOCALIZER,logger,latch);
	
				if (SystemTray.isSupported()) {
					try(final JSystemTray		tray = new JSystemTray(LocalizerFactory.getLocalizer(xda.getRoot().getLocalizerAssociated()), "Image Database", app.getClass().getResource("tray.png").toURI(), KEY_TITLE_APPLICATION, app.trayMenu)) {
						final ActionListener	al = (e)->{
													app.setVisible(!app.isVisible());
												};
	
						tray.addActionListener(al);
						service.start();
						app.setVisible(true);
						jss.end();
						latch.await();
						service.stop();
						tray.removeActionListener(al);
					}
				}
				else {
					service.start();
					app.setVisible(true);
					jss.end();
					latch.await();
					service.stop();
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new IntegerArg(ARG_HELP_PORT, true, "Help port to use for help browser", 13667)
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
