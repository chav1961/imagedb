package chav1961.imagedb.dialogs;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.AskPassword/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="askpassword.title",tooltip="askpassword.title.tt",help="askpassword.title.help")
public class AskPassword implements FormManager<Object, AskPassword> {
	private final LoggerFacade	facade;
	
	@LocaleResource(value="askpassword.password",tooltip="askpassword.password.tt")
	@Format("30ms")
	public char[]				password = null;

	public AskPassword(final LoggerFacade facade) {
		if (facade == null) {
			throw new NullPointerException("Logger facade can't be null"); 
		}
		else {
			this.facade = facade;
		}
	}
	
	@Override
	public RefreshMode onField(final AskPassword inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return facade;
	}
}
