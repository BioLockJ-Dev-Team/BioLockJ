package biolockj.module.diy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.Config;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.DockerVolCreationException;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.BioLockJUtil;

public class Rmarkdown extends ScriptModuleImpl implements ApiModule {

	public Rmarkdown() {
		super();
		addNewProperty( RMD_FILE, Properties.FILE_PATH, RMD_FILE_DESC );
		addNewProperty( RESOURCES, Properties.FILE_PATH_LIST, RESOURCES_DESC );
	}
	

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp(RMD_FILE);
		isValidProp(RESOURCES);
	}

	@Override
	public Boolean isValidProp( String property ) throws Exception {
	    Boolean isValid = super.isValidProp( property );
	    switch(property) {
	        case RMD_FILE:
	        	Config.requireExistingFile( this, RMD_FILE );
	            isValid = true;
	            break;
	        case RESOURCES:
	        	Config.getExistingFileList( this, RESOURCES );
	            isValid = true;
	            break;
	    }
	    return isValid;
	}

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final ArrayList<String> lines = new ArrayList<>();
		transferResources();
		transferMarkdown();
		lines.add( FUNCTION_NAME );
		data.add( lines );
		Log.info( GenMod.class, "Core command: " + data );
		return data;
	}
	
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_NAME + "(){" );
		lines.add( get_key_cmd() );
		lines.add( "}" );
		return lines;
	}
	
	private String get_key_cmd() throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException{
		String rmd_name = Config.requireExistingFile( this, RMD_FILE ).getName();
		// should end up like: Rscript -e "rmarkdown::render('../resources/ReproduceFigure.Rmd', output_dir='../output')"
		String key_cmd = "Rscript -e \"rmarkdown::render('../" + RES_DIR + "/" + rmd_name + "', output_dir='../output')\"";
		return key_cmd;
	}
	

	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + System.lineSeparator() + "Key command: " + get_key_cmd();
	}
	
	protected String transferMarkdown() throws ConfigPathException, IOException, Exception {
		final File original = Config.requireExistingFile( this, RMD_FILE );
		FileUtils.copyFileToDirectory( original, getResourceDir() );
		final File copy = new File( getResourceDir() + File.separator + original.getName() );
		Log.debug( GenMod.class, "Users R Markdown document saved to: " + copy.getAbsolutePath() );
		return copy.getAbsolutePath();
	}
	
	protected void transferResources() throws ConfigPathException, IOException, Exception {
		if( Config.getString( this, RESOURCES ) != null ) {
			for( File file: Config.getExistingFileList( this, RESOURCES ) ) {
				FileUtils.copyFileToDirectory( file, getResourceDir() );
				Log.info( this.getClass(),
					"Copied resource " + file.getAbsolutePath() + " to module resource folder: " + getResourceDir() );
			}
		}
	}
	
	@Override
	public String getDescription() {
		return "Render a custom R markdown.";
	}

	@Override
	public String getDetails() throws API_Exception {
		// TODO Auto-generated method stub
		return super.getDetails();
	}

	@Override
	public String getDockerImageOwner() {
		return "rocker";
	}
	
	@Override
	public String getDockerImageName() {
		return "r-rmd";
	}

	@Override
	public String getDockerImageTag() {
		return "latest";
	}
	
	@Override
	public String getCitationString() {
		return "Module created by Ivory Blakley" + System.lineSeparator() + "BioLockJ " + BioLockJUtil.getVersion( );
	}
	
	/**
	 * {@link biolockj.Config} property: {@value #RMD_FILE}<br>
	 * {@value #RMD_FILE_DESC}
	 */
	protected static final String RMD_FILE = "rmarkdown.rmarkdown";
	private static final String RMD_FILE_DESC = "path to an R markdown file (.Rmd) to be rendered.";
	
	/**
	 * {@link biolockj.Config} property: {@value #RESOURCES}<br>
	 * {@value #RESOURCES_DESC}
	 */
	protected static final String RESOURCES = "rmarkdown.resources";
	private static final String RESOURCES_DESC = "path to one or more files to be copied to the module resource folder.";
	
	private static final String FUNCTION_NAME = "renderRmarkdown";

}
