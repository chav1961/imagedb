package chav1961.imagedb.dialogs;

import java.io.File;
import java.net.URI;

public class DemoWizard {
	public static enum Errors {
		
	}

	public File		driverLocation = new File("./");
	public URI		connURI = URI.create("jdbc:postgresql://localhost:5432/postgres");
	public String	superUser = "";
	public char[]	superPassword = null;
	public String	user = "";
	public char[]	password = null;

}
