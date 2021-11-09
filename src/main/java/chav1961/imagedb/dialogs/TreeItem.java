package chav1961.imagedb.dialogs;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.TreeItem/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="treeitem.title",tooltip="treeitem.title.tt",help="treeitem.title.help")
public class TreeItem implements FormManager<Object,TreeItem>, Cloneable {
	public final LoggerFacade	logger;
	public long					id;
	public long					parent;
	
	@LocaleResource(value="treeitem.name",tooltip="treeitem.name.tt")
	@Format("30ms")
	public String	name;

	@LocaleResource(value="treeitem.comment",tooltip="treeitem.comment.tt")
	@Format("30ms")
	public String	comment;
	
	public TreeItem(final LoggerFacade logger, final long id, final long parent, final String name, final String comment) {
		this.logger = logger;
		this.id = id;
		this.parent = parent;
		this.name = name;
		this.comment = comment;
	}

	@Override
	public String toString() {
		return "TreeItem [id=" + id + ", parent=" + parent + ", name=" + name + ", comment=" + comment + "]";
	}

	@Override
	public RefreshMode onField(TreeItem inst, Object id, String fieldName, Object oldValue, boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return logger;
	}
	
	@Override
	public TreeItem clone() {
		try{return (TreeItem)super.clone();
		} catch (CloneNotSupportedException e) {
			return new TreeItem(logger, id, parent, name, comment);
		}
	}
}