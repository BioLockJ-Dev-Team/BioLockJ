/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date May 11, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import biolockj.Constants;
import biolockj.util.MetaUtil;

/**
 * MetadataException is thrown if errors occur processing Metadata files.
 */
public class MetadataException extends BioLockJException {

	/**
	 * Create standard error to throw for Metadata related errors.
	 *
	 * @param msg Exception message details
	 */
	public MetadataException( final String msg ) {
		super( ( MetaUtil.exists() ? "Error in metadata file [ " + MetaUtil.getPath() + " ] " + RETURN: "" ) + msg );
	}
	
	protected static final String howToLinkMetaWithFile = Constants.RETURN + "File to sample associations can be controlled using [" +
					Constants.INPUT_TRIM_PREFIX + "] and [" + Constants.INPUT_TRIM_SUFFIX +
					"] for sequence files and [" + MetaUtil.META_FILENAME_COLUMN + "] for any file type.";

	private static final long serialVersionUID = 2815907364437447934L;
}
