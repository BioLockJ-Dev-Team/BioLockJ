package biolockj.exception;

import java.util.Map;
import java.util.Set;

public class RequiredRPackageException extends BioLockJException {

	/**
	 * Create exception based on which R package is missing.
	 * @param rpackage name of an R package that is required but cannot be loaded
	 */
	public RequiredRPackageException( String rpackage ) {
		super( missingPackageMessage(rpackage, "" ));
	}
	
	/**
	 * Create exception based on which R package is missing, and the URL to download it from.
	 * @param rpackage name of an R package that is required but cannot be loaded
	 * @param packageRepo url of repository that from which the package may be installed.
	 */
	public RequiredRPackageException( String rpackage, String packageRepo ) {
		super( missingPackageMessage(rpackage, packageRepo));
	}
	
	/**
	 * Create exception based on which R packages are missing, and the URLs of repos to download them from.
	 * @param names Set of Strings given one or more names of packages that are required but cannot be loaded
	 * @param repos Map of package names to url where the package could be installed from.
	 */
	public RequiredRPackageException( Set<String> names, Map<String, String> repos) {
		super( missingMultipleMessage( names, repos));
	}
	
	private static String missingPackageMessage(String rpackage, String repo) {
		String installCmd;
		if ( repo != null && ! repo.isEmpty() ) installCmd = "Rscript -e 'install.packages(\""+rpackage+"\", repos=\""+ repo + "\"); '";
		else installCmd = "Rscript -e 'install.packages(\""+rpackage+"\"); '";
		return "Missing R library [" + rpackage + "]. Please install this package.  Suggested install command: " + System.lineSeparator() 
		+ installCmd; 
	}
	
	private static String missingMultipleMessage(Set<String> names, Map<String, String> repos){
		StringBuffer sb = new StringBuffer();
		for (String name : names) {
			sb.append( missingPackageMessage( name, repos.get( name )) );
		}
		return sb.toString();
	}
	
	
	private static final long serialVersionUID = -8321547735003700677L;

}
