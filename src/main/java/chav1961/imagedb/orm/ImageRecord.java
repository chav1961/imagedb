package chav1961.imagedb.orm;

import java.awt.Image;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.orm.ImageRecord/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="imagerecord.title",tooltip="imagerecord.title.tt",help="imagerecord.title.help")
public class ImageRecord implements FormManager<Object, ImageRecord> {
	private final LoggerFacade	logger;
	
	@LocaleResource(value="imagerecord.parent",tooltip="imagerecord.parent.tt")
	@Format("30ms")
	public TreeRecord			parent;

	@LocaleResource(value="imagerecord.name",tooltip="imagerecord.name.tt")
	@Format("30ms")
	public String				name;

	@LocaleResource(value="imagerecord.comment",tooltip="imagerecord.comment.tt")
	@Format("30ms")
	public String				comment;

	@LocaleResource(value="imagerecord.image",tooltip="imagerecord.image.tt")
	@Format("30ms")
	public Image				image;
	
	public ImageRecord(final LoggerFacade logger) {
		this.logger = logger;
	}
	
	
	@Override
	public RefreshMode onField(ImageRecord inst, Object id, String fieldName, Object oldValue, boolean beforeCommit) throws FlowException, LocalizationException {
		// TODO Auto-generated method stub
		return RefreshMode.DEFAULT;
	}
	
	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
}
