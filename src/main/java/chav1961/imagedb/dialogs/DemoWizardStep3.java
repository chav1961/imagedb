package chav1961.imagedb.dialogs;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.zip.ZipInputStream;

import chav1961.imagedb.db.DbManager;
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
import chav1961.purelib.ui.interfaces.ErrorProcessing;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.ItemAndSelection;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.interfaces.WizardStep;
import chav1961.purelib.ui.swing.AutoBuiltForm;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.DemoWizardStep3/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="demowizardstep3.title",tooltip="demowizardstep3.title.tt",help="demowizardstep3.title.help")
public class DemoWizardStep3 implements WizardStep<DemoWizard, DemoWizard.Errors, AutoBuiltForm<DemoWizardStep3>>, FormManager<Object, DemoWizardStep3> {
	private final LoggerFacade						logger;
	private final ContentMetadataInterface			mdi;
	private final AutoBuiltForm<DemoWizardStep3>	form; 
	
	@LocaleResource(value="demowizardstep3.processing",tooltip="demowizardstep3.processing.tt")
	@Format("30*10ro")
	public ItemAndSelection<String>[]	steps = ItemAndSelection.of("Create user and schema","Load demo database");
	
	
	public DemoWizardStep3(final Localizer localizer, final LoggerFacade logger, final SimpleURLClassLoader loader) throws LocalizationException, ContentException {
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
			form.setPreferredSize(new Dimension(420, 200));
		}
	}

	@Override
	public String getStepId() {
		return "step3";
	}

	@Override
	public StepType getStepType() {
		return StepType.PROCESSING;
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
	public AutoBuiltForm<DemoWizardStep3> getContent() {
		return form;
	}

	@Override
	public void beforeShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
	}

	@Override
	public boolean validate(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		// TODO Auto-generated method stub
		try(final SimpleURLClassLoader	loader = new SimpleURLClassLoader(new URL[] {content.driverLocation.toURI().toURL()})) {
			
			for (Driver item : ServiceLoader.load(Driver.class, loader)) {
				try(final Connection	conn = item.connect(content.connURI.toString(), Utils.mkProps("user", content.superUser, "password", new String(content.superPassword)))) {
					
					if (conn != null) {
						getLogger().message(Severity.info, "create user");
						try(final Statement	stmt = conn.createStatement()) {
							stmt.execute("create role \"" + content.user + "\" login password '" + new String(content.password) + "'");
							stmt.execute("create schema authorization \"" + content.user +"\"");
						}
					}
					else {
						continue;
					}
				}
				try(final Connection	conn = item.connect(content.connURI.toString(), Utils.mkProps("user", content.user, "password", new String(content.password)))) {
					
					if (conn != null) {
						getLogger().message(Severity.info, "load demo database");
						
						try(final InputStream		fis = new FileInputStream("./demo.zip");
							final ZipInputStream	zis = new ZipInputStream(fis)) {
							
							new DbManager(conn).restoreDatabaseByModel(zis);
						}
					}
					else {
						continue;
					}
				}
				return true;
			}
		} catch (SQLException | IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
		return false;
	}

	@Override
	public void afterShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
	}

	@Override
	public RefreshMode onField(final DemoWizardStep3 inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
}
