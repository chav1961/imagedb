package chav1961.imagedb.dialogs;

import java.awt.image.BufferedImage;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.TreeItem/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="imageitem.title",tooltip="imageitem.title.tt",help="imageitem.title.help")
public class ImageItem implements FormManager<Object,ImageItem>, Cloneable {
	public final LoggerFacade	logger;
	public long					id;
	public long					owner;

	@LocaleResource(value="imageitem.tags",tooltip="imageitem.tags.tt")
	@Format("30s")
	public String	tags;
	
	@LocaleResource(value="imageitem.comment",tooltip="imageitem.comment.tt")
	@Format("30*10ms")
	public String				comment;
	public BufferedImage		image;
	
	public ImageItem(final LoggerFacade logger, final long id, final long owner, final String comment, final BufferedImage image, final String tags) {
		this.logger = logger;
		this.id = id;
		this.owner = owner;
		this.comment = comment;
		this.image = image;
		this.tags = tags;
	}

	@Override
	public String toString() {
		return "ImageItem [id=" + id + ", owner=" + owner + ", comment=" + comment + "]";
	}

	@Override
	public RefreshMode onField(final ImageItem inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return logger;
	}

	@Override
	public ImageItem clone() {
		try{return (ImageItem)super.clone();
		} catch (CloneNotSupportedException e) {
			return new ImageItem(logger, id, owner, comment, image, tags);
		}
	}
}