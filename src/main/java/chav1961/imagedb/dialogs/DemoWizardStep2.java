package chav1961.imagedb.dialogs;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Map;

import chav1961.imagedb.dialogs.DemoWizard.Errors;
import chav1961.purelib.basic.SimpleURLClassLoader;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
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
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.interfaces.WizardStep;
import chav1961.purelib.ui.swing.AutoBuiltForm;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.DemoWizardStep2/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="demowizardstep2.title",tooltip="demowizardstep2.title.tt",help="demowizardstep2.title.help")
public class DemoWizardStep2 implements WizardStep<DemoWizard, DemoWizard.Errors, AutoBuiltForm<DemoWizardStep2,?>>, FormManager<Object, DemoWizardStep2> {
	private final LoggerFacade						logger;
	private final ContentMetadataInterface			mdi;
	private final AutoBuiltForm<DemoWizardStep2,?>	form; 
	
	@LocaleResource(value="demowizardstep2.user",tooltip="demowizardstep2.user.tt")
	@Format("30ms")
	private String	user = "";
	
	@LocaleResource(value="demowizardstep2.password",tooltip="demowizardstep2.password.tt")
	@Format("30ms")
	public char[]	password = null;

	@LocaleResource(value="demowizardstep2.password.retype",tooltip="demowizardstep2.password.retype.tt")
	@Format("30ms")
	public char[]	retypePassword = null;
	
	
	public DemoWizardStep2(final Localizer localizer, final LoggerFacade logger, final SimpleURLClassLoader loader) throws LocalizationException, ContentException {
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
			form.setPreferredSize(new Dimension(420, 130));
		}
	}

	@Override
	public String getStepId() {
		return "step2";
	}

	@Override
	public StepType getStepType() {
		return StepType.ORDINAL;
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
	public AutoBuiltForm<DemoWizardStep2,?> getContent() {
		return form;
	}

	@Override
	public void beforeShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		this.user = content.user;
		this.password = content.password;
		this.retypePassword = content.password;
	}

	@Override
	public boolean validate(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		if (Arrays.equals(password, retypePassword)) {
			return true;
		}
		else {
			getLogger().message(Severity.warning, "Passwords typed are not equals");
			return false;
		}
	}

	@Override
	public void afterShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		content.user = this.user;
		content.password = this.password;
	}

	@Override
	public RefreshMode onField(final DemoWizardStep2 inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
}
