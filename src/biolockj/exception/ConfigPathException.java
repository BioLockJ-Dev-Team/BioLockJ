/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import java.io.File;
import biolockj.Config;
import biolockj.util.DockerUtil;

/**
 * ConfigPathException is thrown if property is invalid file paths are encountered when processing the BioLockJ
 * configuration file.
 */
public class ConfigPathException extends ConfigException {

	/**
	 * ConfigPathException is thrown if the filePath parameter does not exist on the file system.
	 *
	 * @param file File or directory path
	 */
	public ConfigPathException( final File file ) {
		super( "Invalid Path: " + showPath(file) + " does not exist on the file system" );
	}

	/**
	 * ConfigPathException is thrown if the filePath parameter does not exist on the file system.
	 *
	 * @param file File or directory path
	 * @param msg Error message
	 */
	public ConfigPathException( final File file, final String msg ) {
		super( "Invalid Path: " + showPath(file) + " does not exist on the file system: " + msg );
	}

	/**
	 * ConfigPathException is thrown if the path does not exist or is not the proper file type: {@value #FILE} or
	 * {@value #DIRECTORY}.
	 *
	 * @param property {@link biolockj.Config} property name
	 * @param fileType File type must be {@value #FILE} or {@value #DIRECTORY}
	 */
	public ConfigPathException( final String property, final String fileType ) {
		super( property, "Current value \"" + Config.getString( null, property ) + "\" is not a valid " + fileType );
	}
	
	private static String showPath(final File file) {
		String show = file.getAbsolutePath();
		try {
			show = DockerUtil.deContainerizePath( file.getAbsolutePath() );
		}catch( DockerVolCreationException dex) {}
		return show;
	}

	/**
	 * One of 2 BioLockJ file path types that can be passed to the constructor {@value #DIRECTORY}. The exception
	 * message will state the filePath given is not a {@value #DIRECTORY}
	 */
	public static final String DIRECTORY = "directory";

	/**
	 * One of 2 BioLockJ file path types that can be passed to the constructor {@value #FILE}. The exception message
	 * will state the filePath given is not a {@value #FILE}
	 */
	public static final String FILE = "file";

	private static final long serialVersionUID = 8070021678414952511L;
}
