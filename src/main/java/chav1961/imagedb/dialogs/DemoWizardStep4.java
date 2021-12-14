package chav1961.imagedb.dialogs;

import java.util.Map;

import javax.swing.JLabel;

import chav1961.imagedb.dialogs.DemoWizard.Errors;
import chav1961.purelib.basic.SimpleURLClassLoader;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.exceptions.PreparationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.ui.interfaces.ErrorProcessing;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.ItemAndSelection;
import chav1961.purelib.ui.interfaces.WizardStep;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.DemoWizardStep4/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="demowizardstep4.title",tooltip="demowizardstep4.title.tt",help="demowizardstep4.title.help")
public class DemoWizardStep4 implements WizardStep<DemoWizard, DemoWizard.Errors, JLabel> {
	private final LoggerFacade				logger;
	private final ContentMetadataInterface	mdi;
	private final JLabel					label = new JLabel("Complete");
	
	
	@LocaleResource(value="demowizardstep3.processing",tooltip="demowizardstep3.processing.tt")
	@Format("30*10s")
	public ItemAndSelection<String>[]	steps;
	
	public DemoWizardStep4(final Localizer localizer, final LoggerFacade logger, final SimpleURLClassLoader loader) throws LocalizationException, ContentException {
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
		}
	}

	@Override
	public String getStepId() {
		return "step4";
	}

	@Override
	public String getPrevStep() {
		return "step2";
	}

	@Override
	public StepType getStepType() {
		return StepType.TERM_SUCCESS;
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
	public JLabel getContent() {
		return label;
	}

	@Override
	public void prepare(final DemoWizard content, final Map<String, Object> temporary) throws PreparationException, LocalizationException, NullPointerException {
	}

	@Override
	public void beforeShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
	}

	@Override
	public boolean validate(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
		return true;
	}

	@Override
	public void afterShow(final DemoWizard content, final Map<String, Object> temporary, final ErrorProcessing<DemoWizard, Errors> err) throws FlowException, LocalizationException, NullPointerException {
	}

	@Override
	public void unprepare(final DemoWizard content, final Map<String, Object> temporary) throws LocalizationException, NullPointerException {
	}
}
