package chav1961.imagedb.orm;

import java.util.List;

import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;

public class TreeRecord {
	public TreeRecord 		parent;
	public String			name;
	public String			comment;
	public List<TreeRecord>	images;
}
