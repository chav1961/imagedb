package chav1961.imagedb.dialogs;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import chav1961.imagedb.dialogs.DemoWizard.Errors;
import chav1961.purelib.basic.SimpleURLClassLoader;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.exceptions.PreparationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.ui.interfaces.Action;
import chav1961.purelib.ui.interfaces.ErrorProcessing;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.interfaces.WizardStep;
import chav1961.purelib.ui.swing.AutoBuiltForm;
import chav1961.purelib.ui.swing.SwingUtils;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.DemoWizardStep1/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="demowizardstep1.title",tooltip="demowizardstep1.title.tt",help="demowizardstep1.title.help")
@Action(resource=@LocaleResource(value="demowizardstep1.testconnection",tooltip="demowizardstep1.testconnection.tt"),actionString="testConnection")
public class DemoWizardStep1 implements WizardStep<DemoWizard, DemoWizard.Errors, AutoBuiltForm<DemoWizardStep1>>, FormManager<Object, DemoWizardStep1> {
	private static final Icon	SUCCESS = new ImageIcon(DemoWizardStep1.class.getResource("greenMark.png")); 
	private static final Icon	FAILED = new ImageIcon(DemoWizardStep1.class.getResource("redMark.png")); 
	
	private final LoggerFacade						logger;
	private final ContentMetadataInterface			mdi;
	private final AutoBuiltForm<DemoWizardStep1>	form; 
	
	@LocaleResource(value="demowizardstep1.driver",tooltip="demowizardstep1.driver.tt")
	@Format("30ms")
	private File	driverLocation;
	
	@LocaleResource(value="demowizardstep1.connurl",tooltip="demowizardstep1.connurl.tt")
	@Format("30ms")
	private URI		connURI;
	
	@LocaleResource(value="demowizardstep1.superuser",tooltip="demowizardstep1.superuser.tt")
	@Format("30ms")
	private String	superUser = "";
	
	@LocaleResource(value="demowizardstep1.password",tooltip="demowizardstep1.password.tt")
	@Format("30ms")
	public char[]	superPassword = null;
	
	public DemoWizardStep1(final Localizer localizer, final LoggerFacade logger, final SimpleURLClassLoader loader) throws LocalizationException, ContentException {
		if (localizer == null) {
			throw new NullPointerException("Localizer can't be null");
		}
		else if (logger == null) {
			throw new NullPointerException("Logger facade can't be null");
		}
		else if (loader == null) {
			throw new NullPointerException("Class loader can't be null");
		}
		else {
			this.logger = logger;
			this.mdi = ContentModelFactory.forAnnotatedClass(this.getClass());
			this.form = new AutoBuiltForm<>(mdi, localizer, loader, this, this);
			
			form.setPreferredSize(new Dimension(420, 150));
		}
	}

	@Override
	public String getStepId() {
		return "step1";
	}

	@Override
	public StepType getStepType() {
		return StepType.INITIAL;
	}

	@Override
	public String getCaption() {
		return mdi.getRoot().getLabelId();
	}

	@Override
	public String getDescription() {
		return mdi.getRoot().getTooltipId();
	}

	@Override
	public String getHelpId() {
		return mdi.getRoot().getHelpId();
	}

	@Override
	public AutoBuiltForm<DemoWizardStep1> getContent() {
		return form;
	}

	@Override
	public void beforeShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		final JButton	button = (JButton)SwingUtils.findComponentByName(form, "ui:/chav1961.imagedb.dialogs.DemoWizardStep1/DemoWizardStep1.testConnection");

		button.setIcon(null);
		this.driverLocation = content.driverLocation;
		this.connURI = content.connURI;
		this.superUser = content.superUser;
		this.superPassword = content.superPassword;
	}

	@Override
	public boolean validate(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		return validateConnection(driverLocation, connURI, superUser, superPassword);
	}

	@Override
	public void afterShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		content.driverLocation = this.driverLocation;
		content.connURI = this.connURI;
		content.superUser = this.superUser;
		content.superPassword = this.superPassword;
	}

	@Override
	public RefreshMode onField(final DemoWizardStep1 inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		switch (fieldName) {
			case "driverLocation"	:
				return validateDriver(driverLocation) ? RefreshMode.DEFAULT : RefreshMode.REJECT; 
			case "connURL"			:
				return validateConnectionString(connURI) ? RefreshMode.DEFAULT : RefreshMode.REJECT; 
			default : 
				return RefreshMode.DEFAULT;
		}
	}

	@Override
	public RefreshMode onAction(final DemoWizardStep1 inst, final Object id, final String actionName, final Object... parameter) throws FlowException, LocalizationException {
		final JButton	button = (JButton)SwingUtils.findComponentByName(form, "ui:/chav1961.imagedb.dialogs.DemoWizardStep1/DemoWizardStep1.testConnection");
		
		if (validateConnection(driverLocation, connURI, superUser, superPassword)) {
			button.setIcon(SUCCESS);
			return  RefreshMode.DEFAULT;
		}
		else {
			button.setIcon(FAILED);
			return  RefreshMode.REJECT;
		}
	}
	
	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
	
	
	private boolean validateDriver(final File driver) {
		if (driver.exists() && driver.canRead()) {
			if (driver.isFile()) {
				try(final SimpleURLClassLoader	loader = new SimpleURLClassLoader(new URL[] {driver.toURI().toURL()})) {
					
					for (Driver item : ServiceLoader.load(Driver.class, loader)) {
						return true;
					}
					getLogger().message(Severity.warning, "Driver file ["+driver.getAbsolutePath()+"] doens't contain JDBC driver required");
				} catch (IOException e) {
					getLogger().message(Severity.warning, "Driver file ["+driver.getAbsolutePath()+"] I/O error: "+e.getLocalizedMessage());
				}
			}
			else {
				getLogger().message(Severity.warning, "Driver path ["+driver.getAbsolutePath()+"] points to directory, not file");
			}
		}
		else {
			getLogger().message(Severity.warning, "Driver file ["+driver.getAbsolutePath()+"] not exists or is not accessible");
		}
		return false;
	}
	
	private boolean validateConnectionString(final URI conn) {
		try{if (conn.isAbsolute() && "jdbc".equalsIgnoreCase(conn.getScheme())) {
				return true;
			}
			else {
				getLogger().message(Severity.warning, "Connection string ["+conn+"] is not a valid URI: schema is missing or is not a 'jdbc'");
			}
		} catch (IllegalArgumentException exc) {
			getLogger().message(Severity.warning, "Connection string ["+conn+"] is not a valid URI: "+exc.getLocalizedMessage());
		}
		return false;
	}
	
	private boolean validateConnection(final File driver, final URI connURI, final String user, final char[] password) {
		if (validateDriver(driver) && validateConnectionString(connURI)) {
			try(final SimpleURLClassLoader	loader = new SimpleURLClassLoader(new URL[] {driver.toURI().toURL()})) {
				
				for (Driver item : ServiceLoader.load(Driver.class, loader)) {
					final Properties		props = Utils.mkProps("user", user, "password", new String(password));
					
					try(final Connection	conn = item.connect(connURI.toString(), props)) {
						return conn != null;
					} catch (SQLException e) {
						getLogger().message(Severity.warning, "Test connection failed: "+e.getLocalizedMessage());
					}
				}
			} catch (IOException e) {
			}
		}
		return false;
	}
}
