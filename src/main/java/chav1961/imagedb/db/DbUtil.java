package chav1961.imagedb.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DbUtil {
	public static String[] extractUniqueTags(final Connection conn) throws SQLException {
		final Set<String>	tags = new HashSet<>();
		
		try(final Statement	stmt = conn.createStatement();
			final ResultSet	rs = stmt.executeQuery("select contenttree.ct_tags from helen.contenttree union all select contentimage.ci_tags from helen.contentimage")) {
		
			while (rs.next()) {
				final String	currentTags = rs.getString(1);
				
				if (!rs.wasNull()) {
					for (String item : currentTags.split(",")) {
						tags.add(item.trim());
					}
				}
			}
		}
		final String[]	selected = tags.toArray(new String[tags.size()]);
		
		Arrays.sort(selected);
		return selected;
	}
}
