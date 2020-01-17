/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Aug 14, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import biolockj.*;
import biolockj.exception.*;
import biolockj.module.*;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.module.diy.GenMod;
import biolockj.module.implicit.qiime.*;
import biolockj.module.report.r.R_Module;
import biolockj.module.seq.*;

/**
 * DockerUtil for Docker integration.
 */
public class DockerUtil {

	/**
	 * Build the {@value #SPAWN_DOCKER_CONTAINER} method, which takes container name, in/out port, and optionally script
	 * path parameters.
	 * 
	 * @param module BioModule
	 * @return Bash function to run docker
	 * @throws ConfigNotFoundException If required {@link biolockj.Config} properties are undefined
	 * @throws ConfigViolationException If {@value biolockj.Constants#EXE_DOCKER} property name does not start with
	 * prefix "exe."
	 * @throws ConfigFormatException If {@value #SAVE_CONTAINER_ON_EXIT} property value is not set as a boolean
	 * {@value biolockj.Constants#TRUE} or {@value biolockj.Constants#FALSE}
	 * @throws ConfigPathException If mounted Docker volumes are not found on host or container file-system
	 * @throws DockerVolCreationException 
	 * @throws SpecialPropertiesException 
	 */
	public static List<String> buildSpawnDockerContainerFunction( final BioModule module, final String startedFlag )
		throws ConfigException, DockerVolCreationException {
		String tempDir = module.getTempDir().getAbsolutePath();
		Log.info( DockerUtil.class, "tempDir String: " + tempDir);
		final List<String> lines = new ArrayList<>();
		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add(  SCRIPT_ID_VAR + "=$(basename $1)");
		lines.add( LOG_VAR + "=" + tempDir + "/${" + SCRIPT_ID_VAR + "}.log" );
		lines.add(  ID_VAR + "=$(" + Config.getExe( module, Constants.EXE_DOCKER ) + " run " + DOCKER_DETACHED_FLAG + " "+ rmFlag( module ) + WRAP_LINE );
		lines.addAll(  getDockerVolumes( module )); 
		lines.add(  getDockerImage( module ) + WRAP_LINE );
		lines.add( "/bin/bash -c \"$1 &> $" + LOG_VAR + "\" )" );
		lines.add( "echo \"Launched docker image: " + getDockerImage( module ) + "\"" );
		lines.add( "echo \"To execute module: " + module.getClass().getSimpleName() + "\"" );
		lines.add( "echo \"Docker container id: $" + ID_VAR + "\"" );
		lines.add( "echo \"${" + SCRIPT_ID_VAR + "}:" + DOCKER_KEY + ":${" + ID_VAR + "}\" >> " + startedFlag );
		lines.add( "}" + Constants.RETURN );
		return lines;
	}
	
	public static boolean workerContainerStopped (final File mainStarted, final File workerScript) {
		boolean hasStopped = false;
		String containerId = null;
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader( mainStarted ));
			String s = null;
			String key = workerScript.getName() + ":" + DOCKER_KEY + ":";
			while( ( s = reader.readLine() ) != null )
			{
				if (s.startsWith( workerScript.getName() )) containerId=s.replaceFirst( key, "" );
			}
			reader.close();
		} catch( IOException e ) {
			Log.warn(DockerUtil.class, "Failed to extract container id from [" + mainStarted.getName() + "].");
			e.printStackTrace();
		}
		if ( containerId == null ) {
			Log.warn(DockerUtil.class, "No container id for [" + workerScript.getName() + "].");
		}else {
			try {
				hasStopped = ! containerIsRunning(containerId);
			} catch( IOException e ) {
				Log.warn(DockerUtil.class, "Could not determine if container [" + containerId + "] is running.");
				e.printStackTrace();
			}
		}
		return(hasStopped);
	}
	private static boolean containerIsRunning (final String containerId) throws IOException {
		String cmd = "docker inspect -f '{{.State.Running}}' " + containerId + " 2>/dev/null";
		final Process p = Runtime.getRuntime().exec( cmd ); 
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s = br.readLine();
		br.close();
		Log.debug(DockerUtil.class, "Docker inspect result: " + s);
		return s.equals( "'true'" );
	}
	
	private static List<String> getDockerVolumes( final BioModule module )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		Log.debug( DockerUtil.class, "Assign Docker volumes for module: " + module.getClass().getSimpleName() );

		final List<String> dockerVolumes = new ArrayList<>();
		dockerVolumes.add( " -v " + DOCKER_SOCKET + ":" + DOCKER_SOCKET  + WRAP_LINE);
		dockerVolumes.add( " -v " + deContainerizePath( Config.getPipelineDir().getParent() ) + ":" + Config.getPipelineDir().getParent() + ":delegated" + WRAP_LINE );
		for ( String key : volumeMap.keySet() ) {
			if ( key.equals( DOCKER_SOCKET ) ) continue;
			if ( volumeMap.get( key ).equals( DOCKER_PIPELINE_DIR ) ) continue;
			dockerVolumes.add( " -v " + key + ":" + volumeMap.get( key ) + ":ro" + WRAP_LINE );
		}
		
		Log.debug( DockerUtil.class, "Passed along volumes: " + dockerVolumes );
		return dockerVolumes;
	}

	/**
	 * Download a database for a Docker container
	 * 
	 * @param args Terminal command + args
	 * @param label Log file identifier for subprocess
	 * @return Thread ID
	 */
	public static Long downloadDB( final String[] args, final String label ) {
		if( downloadDbCmdRegister.contains( args ) ) {
			Log.warn( DockerUtil.class,
				"Ignoring duplicate download request - already downloading Docker DB: " + label );
			return null;
		}

		downloadDbCmdRegister.add( args );
		return Processor.runSubprocess( args, label ).getId();
	}

	/**
	 * Get the Docker container database found under the DockerDB directory or one of it's sub-directories.
	 * 
	 * @param module DatabaseModule
	 * @param dbPath Database file or sub-directory under the main Docker $BLJ_DB directory.
	 * @return Container database directory
	 * @throws ConfigPathException if DB property not found
	 * @throws ConfigNotFoundException if path is defined but is not an existing directory
	 * @throws DockerVolCreationException 
	 */
	public static File getDockerDB( final DatabaseModule module, final String dbPath )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		if( hasCustomDockerDB( module ) ) {
			if( dbPath == null ) return new File( DOCKER_DB_DIR );
			if( inAwsEnv() ) return new File( dbPath );
			return new File( dbPath.replace( getDbDirPath( module ), DockerUtil.DOCKER_DB_DIR ) );
		}

		if( dbPath == null || module.getDB() == null ) return new File( DOCKER_DEFAULT_DB_DIR );
		if( inAwsEnv() ) return new File( dbPath );
		return new File( dbPath.replace( getDbDirPath( module ), DockerUtil.DOCKER_DEFAULT_DB_DIR ) );
	}

	/**
	 * Return the name of the Docker image needed for the given module.
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 * @throws ConfigNotFoundException if Docker image version is undefined
	 */
	public static String getDockerImage( final BioModule module ) throws ConfigNotFoundException {
		return " " + getDockerUser( module ) + "/" + getImageName( module ) + ":" +
			Config.requireString( module, DockerUtil.DOCKER_IMG_VERSION );
	}

	/**
	 * Return the Docker Hub user ID. If none configured, return biolockj.
	 * 
	 * @param module BioModule
	 * @return Docker Hub User ID
	 */
	public static String getDockerUser( final BioModule module ) {
		final String user = Config.getString( module, DOCKER_HUB_USER );
		if( user == null ) return DEFAULT_DOCKER_HUB_USER;
		return user;
	}

	/**
	 * Get Docker file path through mapped volume
	 * 
	 * @param path {@link biolockj.Config} file or directory path
	 * @param containerPath Local container path
	 * @return Docker file path
	 */
	public static File getDockerVolumePath( final String path, final String containerPath ) {
		if( path == null || path.isEmpty() ) return null;
		return new File( containerPath + path.substring( path.lastIndexOf( File.separator ) ) );
	}

	/**
	 * Return the Docker Image name for the given class name.<br>
	 * Return blj_bash for simple bash script modules that don't rely on special software<br>
	 * Class names contain no spaces, words are separated via CamelCaseConvension.<br>
	 * Docker image names cannot contain upper case letters, so this method substitutes "_" before the lower-case
	 * version of each capital letter.<br>
	 * <br>
	 * Example: JavaModule becomes java_module
	 * 
	 * @param module BioModule
	 * @return Docker Image Name
	 * @throws ConfigNotFoundException if Docker container is undefiend for GenMod
	 */
	public static String getImageName( final BioModule module ) throws ConfigNotFoundException {
		final String className = module.getClass().getName();
		final String simpleName = getDockerClassName( module );
		String imageName = simpleName.substring( 0, 1 ).toLowerCase();
		if( useBasicBashImg( module ) ) imageName = BLJ_BASH;
		else if (module instanceof JavaModule ) imageName = "biolockj_controller"; //TODO: this whole process should be replaced with Config and Module methods.
		else if( module instanceof GenMod ) imageName = Config.requireString( module, Constants.DOCKER_CONTAINER_NAME );
		else {
			for( int i = 2; i < simpleName.length() + 1; i++ ) {
				final int len = imageName.toString().length();
				final String prevChar = imageName.toString().substring( len - 1, len );
				final String val = simpleName.substring( i - 1, i );
				if( !prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) &&
					val.equals( val.toUpperCase() ) && !NumberUtils.isNumber( val ) )
					imageName += IMAGE_NAME_DELIM + val.toLowerCase();
				else if( !prevChar.equals( IMAGE_NAME_DELIM ) ||
					prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) ) imageName += val.toLowerCase();
			}

			if( hasCustomDockerDB( module ) && ( className.toLowerCase().contains( "knead_data" ) ||
				className.toLowerCase().contains( "kraken" ) ) ) imageName += DB_FREE;
		}
		
		Log.info( DockerUtil.class, "Map: Class [" + className + "] <--> Docker Image [ " + imageName + " ]" );
		return imageName;
	}

	/**
	 * TODO: see if this should be updated for new docker volume system.
	 * Function used to determine if an alternate database has been defined (other than /mnt/db).
	 * 
	 * @param module BioModule
	 * @return TRUE if module has a custom DB defined runtime env
	 */
	public static boolean hasCustomDockerDB( final BioModule module ) {
		try {
			Log.info( DockerUtil.class, Constants.LOG_SPACER );
			Log.info( DockerUtil.class, "Check for Custom Docker DB" );
			Log.info( DockerUtil.class, Constants.LOG_SPACER );
			if( inDockerEnv() ) 
				Log.info( DockerUtil.class, "Verified BLJ is running INSIDE the Docker biolockj_controller Container" );
			else {
				Log.info( DockerUtil.class, "LOOKS LIKE BLJ is <<< NOT >>> running INSIDE the Docker biolockj_controller Container - run extra tests!" );
				final File testFile = new File( "/.dockerenv" );
				if( testFile.isFile() )
					Log.info( DockerUtil.class, "testFile.isFile() == TRUE! --> WHY FAIL ON INIT ATTEMPT?  BLJ is running INSIDE the Docker biolockj_controller Container" );
				else if( testFile.exists() )
					Log.info( DockerUtil.class, "testFile.exists() == TRUE! --> WHY FAIL ON INIT ATTEMPT?  BLJ is running INSIDE the Docker biolockj_controller Container" );
			}
			
			if( module instanceof DatabaseModule )
				Log.info( DockerUtil.class, module.getClass().getSimpleName() + " is a DB Module!" );
			else
				Log.info( DockerUtil.class, module.getClass().getSimpleName() + " is NOT DB Module!" );
			
			Log.info( DockerUtil.class, Constants.LOG_SPACER );
				
			if( inDockerEnv() && module instanceof DatabaseModule ) {
				final File db = ( (DatabaseModule) module ).getDB();
				if( db == null ) Log.info( DockerUtil.class, module.getClass().getSimpleName() + " db ==> NULL " );
				if( db != null ) Log.info( DockerUtil.class, module.getClass().getSimpleName() + " db ==> " + db.getAbsolutePath() );
				if( db != null ) return !db.getAbsolutePath().startsWith( DOCKER_DEFAULT_DB_DIR );
			}
		} catch( ConfigPathException | ConfigNotFoundException | DockerVolCreationException ex ) {
			Log.error( DockerUtil.class,
				"Error occurred checking database path of module: " + module.getClass().getName(), ex );
		} 
		return false;
	}

	/**
	 * Return TRUE if running in AWS (based on Config props).
	 * 
	 * @return TRUE if pipeline.env=aws
	 */
	public static boolean inAwsEnv() {
		return RuntimeParamUtil.isAwsMode();
	}

	/**
	 * Check runtime env for /.dockerenv
	 * 
	 * @return TRUE if Java running in Docker container
	 */
	public static boolean inDockerEnv() {
		return DOCKER_ENV_FLAG_FILE.isFile();
	}

	private static String getDbDirPath( final DatabaseModule module )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException {
		if( module.getDB() == null ) return null;
		if( module instanceof RdpClassifier ) return module.getDB().getParentFile().getAbsolutePath();
		return module.getDB().getAbsolutePath();
	}

	private static String getDockerClassName( final BioModule module ) {
		final String className = module.getClass().getSimpleName();
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeQiimeOtuTables ||
			module instanceof QiimeClassifier;

		return isQiime ? QiimeClassifier.class.getSimpleName(): module instanceof R_Module ?
			R_Module.class.getSimpleName(): module instanceof JavaModule ? JavaModule.class.getSimpleName(): className;
	}

	private static TreeMap<String, String> volumeMap;	
	
	private static void makeVolMap() throws DockerVolCreationException {
		StringBuilder sb = new StringBuilder();
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec( getDockerInforCmd() );
			final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			while( ( s = br.readLine() ) != null ) {
				sb.append( s );
			}
			p.waitFor();
			p.destroy();
		} catch( IOException | InterruptedException e ) {
			e.printStackTrace();
			throw new DockerVolCreationException(e);
		} 
		String json = sb.toString();
		JSONArray fullArr = new JSONArray( json );
		JSONObject obj = fullArr.getJSONObject( 0 );
		if ( !obj.has("Mounts") ) throw new DockerVolCreationException();
		JSONArray arr = obj.getJSONArray("Mounts");
		volumeMap = new TreeMap<>();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject mount = arr.getJSONObject( i ) ;
			volumeMap.put( mount.get( "Source" ).toString(), mount.get( "Destination" ).toString());
			Log.info(DockerUtil.class, "Host directory: " + mount.get( "Source" ).toString());
			Log.info(DockerUtil.class, "is mapped to container directory: " + mount.get( "Destination" ).toString());
		}
		Log.info( DockerUtil.class, volumeMap.toString() );
	}
	
	private static void writeDockerInfo() throws IOException, InterruptedException {
		File infoFile = getInfoFile();
		Log.info(DockerUtil.class, "Creating " + infoFile.getName() + " file.");
		final BufferedWriter writer = new BufferedWriter( new FileWriter( infoFile ) );
		//TODO: it must be possible to do this with a simple redirect
		//final Process p = Runtime.getRuntime().exec( getDockerInforCmd() + " > " + infoFile.getAbsolutePath() );
		final Process p = Runtime.getRuntime().exec( getDockerInforCmd() );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		StringBuilder sb = new StringBuilder();
		String s = null;
		while( ( s = br.readLine() ) != null ) {
			sb.append( s ); 
			writer.write( s + System.lineSeparator());
		}
		p.waitFor();
		p.destroy();
		writer.close();
		Log.info(DockerUtil.class, "the info file " + (infoFile.exists() ? "is here:" + infoFile.getAbsolutePath() : "is not here.") );
	}
	
	private static String getDockerInforCmd() throws IOException{
		return "docker inspect " + getContainerId();
		//return "curl --unix-socket /var/run/docker.sock http:/v1.38/containers/" + getHostName() + "/json";
	}
	
	private static File getInfoFile() {
		File parentDir = Config.getPipelineDir();
		if( BioLockJUtil.isDirectMode() )
			parentDir = new File((new File(Config.getPipelineDir(), RuntimeParamUtil.getDirectModuleDir())), BioModuleImpl.TEMP_DIR);
		if( parentDir != null && parentDir.exists() ) {
			Log.debug(DockerUtil.class, "path to info file: " + (new File( parentDir, DOCKER_INFO_FILE )).getAbsolutePath());
			return new File( parentDir, DOCKER_INFO_FILE );
		} else {
			return null;
		}
	}
	
	public static String containerizePath(String path) throws DockerVolCreationException  {
		Log.debug(DockerUtil.class, "Containerizing path: " + path);
		if ( !DockerUtil.inDockerEnv() ) return path;
		if (path == null || path.isEmpty()) return null;
		String innerPath = path;
		TreeMap<String, String> vmap = getVolumeMap();
		String pipelineKey = null;
		for (String key : volumeMap.keySet()) {
			if (volumeMap.get( key ).startsWith( DOCKER_PIPELINE_DIR ) ) pipelineKey = key;
			if ( DockerUtil.inAwsEnv() && volumeMap.get( key ).startsWith( DOCKER_BLJ_MOUNT_DIR ) ) pipelineKey = key;
		}
		if (pipelineKey == null) throw new DockerVolCreationException("no pipeline dir !");
		if ( pipelineKey != null && path.startsWith( pipelineKey ) ) return innerPath.replaceFirst( pipelineKey, vmap.get( pipelineKey ) );
		for (String s : vmap.keySet()) {
			if ( path.startsWith( s ) ) {
				innerPath = innerPath.replaceFirst( s, vmap.get( s ) );
				break;
			}
		}
		return innerPath;
	}
	
	public static String deContainerizePath(String innerPath) throws DockerVolCreationException {
		TreeMap<String, String> vmap;
		vmap = getVolumeMap();
		String hostPath = innerPath;
		for (String s : vmap.keySet()) {
			if ( innerPath.startsWith( vmap.get( s ) ) ) {
				hostPath = hostPath.replaceFirst( vmap.get( s ), s );
				break;
			}
		}
		return hostPath;
	}
	
	public static String getContainerId() throws IOException {
		String id = null;
		File cgroup = new File("/proc/self/cgroup");
		BufferedReader br = new BufferedReader(new FileReader( cgroup ) );
		String line = null; 
		while ( (line = br.readLine()) != null) {
			if (line.contains( "name=" )) {
				id = line.substring( line.indexOf( "docker/" ) + 7 );
			}
		}
		br.close();
		return id;
	}
	
	public static String getHostName() {
		return Config.replaceEnvVar( "${HOSTNAME}" );
	}
	
	public static TreeMap<String, String> getVolumeMap() throws DockerVolCreationException {
		if ( volumeMap == null ) {
			makeVolMap();
		}
		return volumeMap;
	}

	/**
	 * Method for diagnosing exceptions; only used by DockerVolumeException
	 * @return
	 */
	public static TreeMap<String, String> backdoorGetVolumeMap() {
		return volumeMap;
	}

	private static final String rmFlag( final BioModule module ) throws ConfigFormatException {
		return Config.getBoolean( module, SAVE_CONTAINER_ON_EXIT ) ? "": DOCK_RM_FLAG;
	}

	private static boolean useBasicBashImg( final BioModule module ) {
		return module instanceof PearMergeReads || module instanceof AwkFastaConverter || module instanceof Gunzipper;
	}
	
	public static void checkDependencies( BioModule module ) throws ConfigNotFoundException, IOException, InterruptedException {
		if ( inDockerEnv() ) {
			if ( ! getInfoFile().exists() ) writeDockerInfo();
			String image = getDockerImage( module );
			Log.info(DockerUtil.class, "The " + module.getClass().getSimpleName() + " module will use this docker image: " + image );
			//if (Config.getBoolean( module, "docker.verifyImage" )) verifyImage(image);
			//    TODO: some quick test to make sure the image exists 
		}else {
			Log.info(DockerUtil.class, "Not running in Docker.  No need to check Docker dependencies.");
		}
	}

	/**
	 * Docker container dir to map HOST $HOME to save logs + find Config values using $HOME: {@value #AWS_EC2_HOME} Need
	 * to name this dir = "/home/ec2-user" so Nextflow config is same inside + outside of container
	 */
	public static final String AWS_EC2_HOME = "/home/ec2-user";

	/**
	 * Docker container root user EFS directory: /mnt/efs
	 */
	public static final String DOCKER_BLJ_MOUNT_DIR = "/mnt/efs";

	/**
	 * Docker container root user DB directory: /mnt/efs/db
	 */
	public static final String DOCKER_DB_DIR = DOCKER_BLJ_MOUNT_DIR + "/db";

	/**
	 * Docker container root user DB directory: /mnt/efs/db
	 */
	public static final String DOCKER_DEFAULT_DB_DIR = "/mnt/db";

	/**
	 * All containers mount {@value biolockj.Constants#INTERNAL_PIPELINE_DIR} to the container volume: /mnt/efs/output
	 */
	public static final String DOCKER_PIPELINE_DIR = DOCKER_BLJ_MOUNT_DIR + "/pipelines";

	/**
	 * Docker container default $USER: {@value #DOCKER_USER}
	 */
	public static final String DOCKER_USER = "root";

	/**
	 * Docker container root user $HOME directory: /root
	 */
	public static final String ROOT_HOME = File.separator + DOCKER_USER;

	/**
	 * Docker container blj dir: {@value #CONTAINER_BLJ_DIR}
	 */
	static final String CONTAINER_BLJ_DIR = "/app/biolockj";

	/**
	 * {@link biolockj.Config} String property used to run specific version of Docker images:
	 * {@value #DOCKER_IMG_VERSION}
	 */
	static final String DOCKER_IMG_VERSION = "docker.imgVersion";

	/**
	 * {@link biolockj.Config} Boolean property - enable to avoid docker run --rm flag: {@value #SAVE_CONTAINER_ON_EXIT}
	 */
	static final String SAVE_CONTAINER_ON_EXIT = "docker.saveContainerOnExit";

	/**
	 * Name of the bash script function used to generate a new Docker container: {@value #SPAWN_DOCKER_CONTAINER}
	 */
	static final String SPAWN_DOCKER_CONTAINER = "spawnDockerContainer";

	/**
	 * {@link biolockj.Config} name of the Docker Hub user with the BioLockJ containers: {@value #DOCKER_HUB_USER}<br>
	 * Docker Hub URL: <a href="https://hub.docker.com" target="_top">https://hub.docker.com</a><br>
	 * By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own
	 * versions of these modules and add new modules in their own Docker Hub account.
	 */
	protected static final String DOCKER_HUB_USER = "docker.user";

	private static final String BLJ_BASH = "blj_bash";
	private static final String DB_FREE = "_dbfree";
	private static final String DEFAULT_DOCKER_HUB_USER = "biolockj";
	private static final String DOCK_RM_FLAG = "--rm";
	private static final File DOCKER_ENV_FLAG_FILE = new File( "/.dockerenv" );
	private static final String DOCKER_SOCKET = "/var/run/docker.sock";
	private static final Set<String[]> downloadDbCmdRegister = new HashSet<>();
	private static final String IMAGE_NAME_DELIM = "_";
	private static final String WRAP_LINE = " \\";
	private static final String DOCKER_DETACHED_FLAG = "--detach";
	private static final String ID_VAR = "containerId";
	private static final String SCRIPT_ID_VAR = "SCRIPT_ID";
	private static final String LOG_VAR = "LOG_FILE";
	private static final String DOCKER_KEY = "docker";
	private static final String DOCKER_INFO_FILE = "dockerInfo.json";
}
