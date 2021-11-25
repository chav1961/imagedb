package chav1961.imagedb.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.ItemAndSelection;
import chav1961.purelib.ui.interfaces.RefreshMode;

@LocaleResourceLocation("i18n:xml:root://chav1961.imagedb.dialogs.FilterItem/chav1961/imagedb/i18n/i18n.xml")
@LocaleResource(value="filteritem.title",tooltip="filteritem.title.tt",help="filteritem.title.help")
public class FilterItem implements FormManager<Object,FilterItem>, Cloneable {
	public final LoggerFacade	logger;

	@LocaleResource(value="filteritem.tags",tooltip="filteritem.tags.tt")
	@Format("30*10s")
	public ItemAndSelection<String>[]	tags;
	
	
	public FilterItem(final LoggerFacade logger, final ItemAndSelection<String>[] tags) {
		this.logger = logger;
		this.tags = tags;
	}

	@Override
	public String toString() {
		return "FilterItem [tags=" + Arrays.toString(tags) + "]";
	}

	@Override
	public RefreshMode onField(final FilterItem inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return logger;
	}

	@Override
	public FilterItem clone() {
		try{return (FilterItem)super.clone();
		} catch (CloneNotSupportedException e) {
			return new FilterItem(logger, tags);
		}
	}
	
	public void join(final ItemAndSelection<String>[] newTags) {
		if (newTags == null) {
			throw new NullPointerException("Tags to join can't be null"); 
		}
		else if (newTags.length > 0) {
			final Set<String>						oldNames = new HashSet<>(), newNames = new HashSet<>();  
			final List<ItemAndSelection<String>>	totalList = new ArrayList<>(); 
			
			for (ItemAndSelection<String> item : tags) {
				oldNames.add(item.getItem());
			}
			for (ItemAndSelection<String> item : newTags) {
				newNames.add(item.getItem());
			}
			for (ItemAndSelection<String> item : tags) {
				if (newNames.contains(item.getItem())) {
					totalList.add(item);
				}
			}
			for (ItemAndSelection<String> item : newTags) {
				if (!oldNames.contains(item.getItem())) {
					totalList.add(item);
				}
			}
			tags = totalList.toArray(new ItemAndSelection[totalList.size()]);
			Arrays.sort(tags, (o1,o2)->o1.getItem().compareTo(o2.getItem()));
		}
	}
}
