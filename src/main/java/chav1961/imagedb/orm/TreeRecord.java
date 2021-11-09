package chav1961.imagedb.orm;

import java.util.List;

import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.AskPassword/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="askpassword.title",tooltip="askpassword.title.tt",help="askpassword.title.help")
public class TreeRecord {
	public TreeRecord 		parent;
	public String			name;
	public String			comment;
	public List<TreeRecord>	images;
}
