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
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import biolockj.*;
import biolockj.Properties;
import biolockj.api.API_Exception;
import biolockj.exception.*;
import biolockj.module.*;

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
		final List<String> lines = new ArrayList<>();
		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add( SCRIPT_ID_VAR + "=$(basename $1)" );
		lines.add( ID_VAR + "=$(" + Config.getExe( module, Constants.EXE_DOCKER ) + " run " + DOCKER_DETACHED_FLAG +
			" " + rmFlag( module ) + WRAP_LINE );
		lines.addAll( getDockerVolumes( module ) );
		lines.addAll( getEnvVars() );
		lines.add( " " + getDockerImage( module ) + WRAP_LINE );
		lines.add( USE_BASH + " \"$1\" )" );
		lines.add( "echo \"Launched docker image: " + getDockerImage( module ) + "\"" );
		lines.add( "echo \"To execute module: " + module.getClass().getSimpleName() + "\"" );
		lines.add( "echo \"Docker container id: $" + ID_VAR + "\"" );
		lines.add( "echo \"${" + SCRIPT_ID_VAR + "}:" + DOCKER_KEY + ":${" + ID_VAR + "}\" >> " + startedFlag );
		lines.add( "docker inspect ${" + ID_VAR + "}" );
		lines.add( "}" + Constants.RETURN );
		return lines;
	}

	private static Collection<? extends String> getEnvVars() throws ConfigNotFoundException  {
		final List<String> elines =  new ArrayList<>();
		Map<String, String> envVars = Config.getEnvVarMap();
		for (String key : envVars.keySet()) {
			Log.debug(DockerUtil.class, "Giving container environment variable: " + key + "=" + envVars.get( key ));
			elines.add( " -e " + key + "=" + envVars.get( key ) + " " + WRAP_LINE );
		}
		return elines;
	}

	public static boolean workerContainerStopped( final File mainStarted, final File workerScript ) {
		boolean hasStopped = false;
		String containerId = null;
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader( mainStarted ) );
			String s = null;
			String key = workerScript.getName() + ":" + DOCKER_KEY + ":";
			while( ( s = reader.readLine() ) != null ) {
				if( s.startsWith( workerScript.getName() ) ) containerId = s.replaceFirst( key, "" );
			}
			reader.close();
		} catch( IOException e ) {
			Log.warn( DockerUtil.class, "Failed to extract container id from [" + mainStarted.getName() + "]." );
			e.printStackTrace();
		}
		if( containerId == null ) {
			Log.warn( DockerUtil.class, "No container id for [" + workerScript.getName() + "]." );
		} else {
			try {
				hasStopped = !containerIsRunning( containerId );
			} catch( IOException e ) {
				Log.warn( DockerUtil.class, "Could not determine if container [" + containerId + "] is running." );
				e.printStackTrace();
			}
		}
		return ( hasStopped );
	}

	private static boolean containerIsRunning( final String containerId ) throws IOException {
		String cmd = "docker inspect -f '{{.State.Running}}' " + containerId + " 2>/dev/null";
		final Process p = Runtime.getRuntime().exec( cmd );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s = br.readLine();
		br.close();
		Log.debug( DockerUtil.class, "Docker inspect result: " + s );
		return s.equals( "'true'" );
	}
	
	private static List<String> getDockerVolumes( final BioModule module )
		throws ConfigPathException, ConfigNotFoundException, DockerVolCreationException, ConfigFormatException {
		Log.debug( DockerUtil.class, "Assign Docker volumes for module: " + module.getClass().getSimpleName() );
		final List<String> dockerVolumes = new ArrayList<>();
		if (Config.getBoolean( module, DOCKER_MOUNT_SOCK )) {
			dockerVolumes.add( " -v " + DOCKER_SOCKET + ":" + DOCKER_SOCKET + WRAP_LINE );
		}
		Log.debug( DockerUtil.class, "The docker socket [" + DOCKER_SOCKET + "] will " + (Config.getBoolean( module, DOCKER_MOUNT_SOCK ) ? "be " : "not be ") 
			+ "mounted for worker containers for module " + module.getClass().getSimpleName() );
		dockerVolumes.add( " -v " + deContainerizePath( BioLockJ.getPipelineDir().getParent() ) + ":" +
			BioLockJ.getPipelineDir().getParent() + ":delegated" + WRAP_LINE );
		for( String key: volumeMap.keySet() ) {
			if( key.equals( DOCKER_SOCKET ) ) continue;
			if( volumeMap.get( key ).equals( DOCKER_PIPELINE_DIR ) ) continue;
			String access = needsWritePermission( module, key ) ? ":delegated": ":ro";
			dockerVolumes.add( " -v " + key + ":" + volumeMap.get( key ) + access + WRAP_LINE );
		}

		Log.debug( DockerUtil.class, "Passed along volumes: " + dockerVolumes );
		return dockerVolumes;
	}

	private static boolean needsWritePermission( BioModule module, String key )
		throws DockerVolCreationException, ConfigPathException {
		if( module instanceof OutsidePipelineWriter ) {
			OutsidePipelineWriter wopMod = (OutsidePipelineWriter) module;
			Set<String> wopDirs = wopMod.getWriteDirs();
			if( wopDirs.contains( volumeMap.get( key ) ) ) {
				Log.info( DockerUtil.class, "The module [" + ModuleUtil.displaySignature( module ) +
					"] is granted write access to the folder [" + key + "]" );
				return true;
			}
		}
		return false;
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
	 * Return the name of the Docker image needed for the given module.
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 * @throws ConfigNotFoundException if Docker image version is undefined
	 */
	public static String getDockerImage( final BioModule module ) throws ConfigNotFoundException {
		if ( module == null ) {
			return Constants.MAIN_DOCKER_OWNER
							+ "/" + Constants.MAIN_DOCKER_IMAGE
							+ ":" + getDefaultImageTag();
		}
		return getDockerUser( module ) + "/" + getImageName( module ) + ":" + getImageTag( module );
	}

	/**
	 * Return the Docker Hub user ID. If none configured, return biolockj.
	 * 
	 * @param module BioModule
	 * @return Docker Hub User ID
	 */
	private static String getDockerUser( final BioModule module ) {
		String user = module.getDockerImageOwner();
		if( Config.getString( module, DOCKER_HUB_USER ) != null ) user = Config.getString( module, DOCKER_HUB_USER );
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
	 * Return the Docker Image name for the given module.<br>
	 * This information should come from the module, but config properties can be used to override the info in the
	 * module.
	 * 
	 * @param module BioModule
	 * @return Docker Image Name in the form <owner>/<image>:<tag>
	 */
	private static String getImageName( final BioModule module ) {
		String name = module.getDockerImageName();
		if( Config.getString( module, DOCKER_IMG ) != null ) name = Config.getString( module, DOCKER_IMG );
		return name;
	}

	private static String getImageTag( final BioModule module ) {
		String tag = module.getDockerImageTag();
		if( Config.getString( module, DOCKER_IMG_VERSION ) != null )
			tag = Config.getString( module, DOCKER_IMG_VERSION );
		return tag;
	}
	
	public static String getDefaultImageTag() {
		String tag;
		if( Config.getString( null, DOCKER_IMG_VERSION ) != null ) {
			tag = Config.getString( null, DOCKER_IMG_VERSION );
		}else if (BioLockJUtil.getVersion().contains( "-" )) {
			tag = BioLockJUtil.getVersion().substring( 0, BioLockJUtil.getVersion().indexOf( "-" ) );
		}else {
			tag = BioLockJUtil.getVersion();
		}
		return tag;
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
			throw new DockerVolCreationException( e );
		}
		String json = sb.toString();
		JSONArray fullArr = new JSONArray( json );
		JSONObject obj = fullArr.getJSONObject( 0 );
		if( !obj.has( "Mounts" ) ) throw new DockerVolCreationException();
		JSONArray arr = obj.getJSONArray( "Mounts" );
		volumeMap = new TreeMap<>();
		for( int i = 0; i < arr.length(); i++ ) {
			JSONObject mount = arr.getJSONObject( i );
			String source = convertMacPath(mount.get( "Source" ).toString());
			String destination = mount.get( "Destination" ).toString();
			volumeMap.put( source, destination );
			Log.info( DockerUtil.class, "Host directory: " + source );
			Log.info( DockerUtil.class, "is mapped to container directory: " + destination );
		}
		Log.info( DockerUtil.class, volumeMap.toString() );
	}

	private static void writeDockerInfo() throws IOException, InterruptedException, DockerVolCreationException {
		File infoFile = getInfoFile();
		Log.info( DockerUtil.class, "Creating " + infoFile.getName() + " file." );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( infoFile ) );
		final Process p = Runtime.getRuntime().exec( getDockerInforCmd() );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		StringBuilder sb = new StringBuilder();
		String s = null;
		while( ( s = br.readLine() ) != null ) {
			sb.append( s );
			writer.write( s + System.lineSeparator() );
		}
		p.waitFor();
		p.destroy();
		writer.close();
		Log.info( DockerUtil.class,
			"the info file " + ( infoFile.exists() ? "is here:" + infoFile.getAbsolutePath(): "is not here." ) );
	}

	private static String getDockerInforCmd() throws IOException, DockerVolCreationException {
		return "docker inspect " + getContainerId();
		// return "curl --unix-socket /var/run/docker.sock http:/v1.38/containers/" + getHostName() + "/json";
	}

	private static File getInfoFile() {
		File parentDir = BioLockJ.getPipelineDir();
		if( BioLockJUtil.isDirectMode() )
			parentDir = new File( ( new File( BioLockJ.getPipelineDir(), RuntimeParamUtil.getDirectModuleDir() ) ),
				BioModuleImpl.TEMP_DIR );
		if( parentDir != null && parentDir.exists() ) {
			Log.debug( DockerUtil.class,
				"path to info file: " + ( new File( parentDir, DOCKER_INFO_FILE ) ).getAbsolutePath() );
			return new File( parentDir, DOCKER_INFO_FILE );
		} else {
			return null;
		}
	}

	public static String containerizePath( final String path ) throws DockerVolCreationException {
		Log.debug( DockerUtil.class, "Containerizing path: " + path );
		if( !DockerUtil.inDockerEnv() ) return path;
		if( path == null || path.isEmpty() ) return null;
		
		String hostPath = path;
		if (isWindowsHostPath(path)) hostPath = convertWindowsPath(path);
		
		String pipelineKey = null;
		TreeMap<String, String> vmap = getVolumeMap();
		for( String key: volumeMap.keySet() ) {
			if( volumeMap.get( key ).equals( DOCKER_PIPELINE_DIR ) ) pipelineKey = key;
			if( DockerUtil.inAwsEnv() && volumeMap.get( key ).equals( DOCKER_BLJ_MOUNT_DIR ) ) pipelineKey = key;
			// if the config file is null; we must not be running a pipeline; this is the API calling, and all mounts are under the workspace getting set up.
			if( RuntimeParamUtil.getConfigFile(false) == null && volumeMap.get( key ).equals( PURE_DOCKER_PIPELINE_DIR ) ) pipelineKey = key;
		}
		if( pipelineKey == null && BioLockJ.getPipelineDir() != null ) {
			throw new DockerVolCreationException( "no pipeline dir !" );
		}
		if( pipelineKey != null && isParentDir(pipelineKey, hostPath) )
			return hostPath.replaceFirst( pipelineKey, vmap.get( pipelineKey ) );
		
		String innerPath = path;
		String bestMatch = null;
		int bestMatchLen = 0;
		for( String s: vmap.keySet() ) {
			if( isParentDir(s, hostPath) && s.length() > bestMatchLen ) {
				bestMatch = String.valueOf( s );
				bestMatchLen = s.length();
			}
		}
		if( bestMatch != null ) {
			innerPath = hostPath.replaceFirst( bestMatch, vmap.get( bestMatch ) );
		}
		Log.debug( DockerUtil.class, "Containerized path to: " + innerPath );
		return innerPath;
	}
	public static File containerizePath(final File file) throws DockerVolCreationException {
		return new File(containerizePath(file.getAbsolutePath()));
	}

	public static String deContainerizePath( final String innerPath ) throws DockerVolCreationException {
		String hostPath = innerPath;
		if( DockerUtil.inDockerEnv() ) {
			TreeMap<String, String> vmap;
			vmap = getVolumeMap();
			for( String s: vmap.keySet() ) {
				if (isParentDir(vmap.get( s ), innerPath)) {
					hostPath = hostPath.replaceFirst( vmap.get( s ), s );
					break;
				}
			}

		}
		return hostPath;
	}
	public static File deContainerizePath( final File innerFile ) throws DockerVolCreationException {
		return new File( deContainerizePath(innerFile.getAbsolutePath()) );
	}
	public static void main(String[] args) throws DockerVolCreationException {
		if (args.length > 1 && args[1].equals("target") ) System.out.println( containerizePath( args[0] ) );
		else if (args.length > 1 && args[1].equals("source") ) System.out.println( deContainerizePath( args[0] ) );
		else System.out.println( deContainerizePath( args[0] ) );
	}
	
	private static boolean isParentDir(String parent, String child){
		if( child.equals( parent ) ) {
			return true;
		}
		if( child.startsWith( parent ) 
						&& child.length() > parent.length() 
						&& child.charAt( parent.length() )==File.separatorChar) {
			return true;
		}
		return false;
	}
	
	private static boolean isWindowsHostPath(final String path) {
		return (path.contains( ":" ) 
						&& path.indexOf( ":" ) == path.lastIndexOf( ":" )
						&& FilenameUtils.separatorsToWindows(path).equals(path));
	}
	
	private static String convertMacPath(final String path) {
		if (path.startsWith( "/host_mnt/" )) return path.replaceFirst( "/host_mnt/", "/" );
		else return path ;
	}
	
	private static String convertWindowsPath(final String path) {
		String drive = path.substring( 0, path.indexOf( ":" ) ).toLowerCase();
		String dirPath = path.substring( path.indexOf( ":" ) + 1 );
		String mountPath = File.separator + drive + FilenameUtils.separatorsToSystem( dirPath );
		Log.debug( DockerUtil.class, "Converted Windows path [" + path + "] to docker mount form: " + mountPath );
		return mountPath ;
	}

	public static String getContainerId() throws IOException, DockerVolCreationException {
		String id = null;
		File cgroup = new File( "/proc/self/cgroup" );
		BufferedReader br = new BufferedReader( new FileReader( cgroup ) );
		String line = null;
		while( ( line = br.readLine() ) != null && id == null ) {
			if( line.contains( ":/docker/" ) ) {
				id = line.substring( line.indexOf( "docker/" ) + 7 );
			}
		}
		if (id==null) throw new DockerVolCreationException( "Failed to determine the id of the current container." );
		br.close();
		return id;
	}

	public static String getHostName() {
		return Config.replaceEnvVar( "${HOSTNAME}" );
	}

	public static TreeMap<String, String> getVolumeMap() throws DockerVolCreationException {
		if( volumeMap == null ) {
			makeVolMap();
		}
		return volumeMap;
	}

	/**
	 * Method for diagnosing exceptions; only used by DockerVolumeException
	 * 
	 * @return
	 */
	public static TreeMap<String, String> backdoorGetVolumeMap() {
		return volumeMap;
	}

	private static final String rmFlag( final BioModule module ) throws ConfigFormatException {
		return Config.getBoolean( module, SAVE_CONTAINER_ON_EXIT ) ? "": DOCK_RM_FLAG;
	}

	public static void checkDependencies( BioModule module )
		throws BioLockJException, InterruptedException, IOException {
		if( inDockerEnv() ) {
			if( !getInfoFile().exists() ) writeDockerInfo();
			String image = getDockerImage( module );
			Log.info( DockerUtil.class,
				"The " + module.getClass().getSimpleName() + " module will use this docker image: " + image );
			if( Config.getBoolean( module, VERIFY_IMAGE ) ) {
				verifyImage( module, image );
			}
			Config.getBoolean( module, DOCKER_MOUNT_SOCK );
		} else {
			Log.info( DockerUtil.class, "Not running in Docker.  No need to check Docker dependencies." );
		}
	}

	private static void verifyImage(BioModule module, String image) throws DockerVolCreationException, InterruptedException, DockerImageException, SpecialPropertiesException, ConfigFormatException, IOException {
		Log.info(DockerUtil.class, "Verifying docker image: " + image);
		final File script = new File( Config.replaceEnvVar( "${BLJ}/resources/docker/" + TEST_SCRIPT) );
		final File copy = new File(BioLockJ.getPipelineDir(), TEST_SCRIPT);
		if ( ! copy.exists() ) FileUtils.copyFileToDirectory( script, BioLockJ.getPipelineDir() );
		copy.setExecutable( true );
		final String cmd = Config.getExe( module, Constants.EXE_DOCKER ) + " run --rm -v " + deContainerizePath( BioLockJ.getPipelineDir() ) + ":/testScript " 
						+ image + " " + USE_BASH + " /testScript/" + TEST_SCRIPT ;
		String result = "no response";
		int exit = -1;
		String label="testing::" + image;
		Log.info( DockerUtil.class, "[ " + label + " ]: STARTING CMD --> " + cmd );
		final ArrayList<String> returnVal = new ArrayList<>();
		final ArrayList<String> returnErr = new ArrayList<>();
		try {
			final Process p = Runtime.getRuntime().exec( cmd );
			final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			final BufferedReader berr = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );
			
			String s = null;
			while( ( s = br.readLine() ) != null ) {
				if( !s.trim().isEmpty() ) {
					Log.info( DockerUtil.class, "[ " + label + " ]: " + s );
					returnVal.add( s );
				}
			}
			while( ( s = berr.readLine() ) != null ) {
				if( !s.trim().isEmpty() ) {
					Log.info( DockerUtil.class, "[ " + label + " ] (sterr) : " + s );
					returnErr.add( s );
				}
			}
			boolean finished = p.waitFor(60, TimeUnit.SECONDS);
			p.destroy();
			if (finished) Log.info( DockerUtil.class, "[ " + label + " ]: COMPLETE" );
			else {
				Log.info( DockerUtil.class, "[ " + label + " ]: STOPPED - time elapsed." );
				Log.info( DockerUtil.class, "If the image is large, it may be necissary to pull the image before starting the pipeline: "
					+ System.lineSeparator() + "command --> docker pull " + image);
			}
			exit=p.exitValue();
			Log.info(DockerUtil.class, "Test [ " + label + " ] returned " + returnVal.size() + " lines, and had exit status: " + exit);
			if( !returnVal.isEmpty() ) result=summarizeReturnVal(returnVal);
			Log.debug(DockerUtil.class, "Got result: " + result);
		}catch(IOException ex) {
			if ( ex.getMessage() != null ) result=ex.getMessage();
			Log.debug(DockerUtil.class, "After IOException, result is: " + result);
		}
		if( ! result.equals( ALL_GOOD ) | exit != 0) {
			StringBuilder msgBuff = new StringBuilder();
			if( !returnErr.isEmpty() ) {
				returnVal.addAll( returnErr );
				msgBuff.append( System.lineSeparator() + summarizeReturnVal(returnVal)) ;
			}else {
				msgBuff.append( System.lineSeparator() + result );
			}
			msgBuff.append( System.lineSeparator() + System.lineSeparator() + "You can change the image using properties: [" );
			msgBuff.append( Config.getModuleFormProp( module, DOCKER_HUB_USER ) + "], [" );
			msgBuff.append( Config.getModuleFormProp( module, DOCKER_IMG ) + "], and [" +
				Config.getModuleFormProp( module, DOCKER_IMG_VERSION ) + "]" );
			msgBuff.append( System.lineSeparator() + "Or disable this check using [" +
				Config.getModuleFormProp( module, VERIFY_IMAGE ) + "=N]" );
			throw new DockerImageException( module, image, msgBuff.toString() ); 
			}
	}

	private static String summarizeReturnVal( ArrayList<String> returnVal ) {
		if( returnVal.isEmpty() ) {
			return "";
		} else {
			final StringBuffer sb = new StringBuffer();
			int cap = 5; // if the result is long, only print the first and last 'cap' lines of the returned info
			int capTotal = 2 * cap;
			if( returnVal.size() == 1 ) {
				sb.append( returnVal.get( 0 ) );
			} else if( returnVal.size() > ( capTotal + 1 ) ) {
				for( int i = 0; i < cap; i++ ) {
					sb.append( returnVal.get( i ) + System.lineSeparator() );
				}
				sb.append(
					"   ... (ommitted " + ( returnVal.size() - capTotal ) + " lines) ..." + System.lineSeparator() );
				for( int i = returnVal.size() - cap; i < returnVal.size(); i++ ) {
					sb.append( returnVal.get( i ) + System.lineSeparator() );
				}
			} else {
				for( int i = 0; i < returnVal.size(); i++ ) {
					sb.append( returnVal.get( i ) + System.lineSeparator() );
				}
			}
			return sb.toString();
		}
	}

	/**
	 * Register properties with the Properties class for API access.
	 * 
	 * @throws API_Exception
	 */
	public static void registerProps() throws API_Exception {
		Properties.registerProp( DOCKER_HUB_USER, Properties.STRING_TYPE, DOCKER_HUB_USER_DESC );
		Properties.registerProp( DOCKER_IMG, Properties.STRING_TYPE, DOCKER_IMG_DESC );
		Properties.registerProp( DOCKER_IMG_VERSION, Properties.STRING_TYPE, DOCKER_IMG_VERSION_DESC );
		Properties.registerProp( SAVE_CONTAINER_ON_EXIT, Properties.BOOLEAN_TYPE, SAVE_CONTAINER_ON_EXIT_DESC );
		Properties.registerProp( VERIFY_IMAGE, Properties.BOOLEAN_TYPE, VERIFY_IMAGE_DESC );
		Properties.registerProp( DOCKER_MOUNT_SOCK, Properties.BOOLEAN_TYPE, DOCKER_MOUNT_SOCK_DESC );
	}

	/**
	 * Let modules see property names.
	 */
	public static ArrayList<String> listProps() {
		ArrayList<String> props = new ArrayList<>();
		props.add( SAVE_CONTAINER_ON_EXIT );
		props.add( VERIFY_IMAGE );
		// Don't include these, these properties are specifically added to the docs in the docker section.
		// props.add( DOCKER_HUB_USER );
		// props.add( DOCKER_IMG );
		// props.add( DOCKER_IMG_VERSION );
		return props;
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
	 * When running in pure docker to do the initial setup, use this pipelines dir: {@value PURE_DOCKER_PIPELINE_DIR}
	 */
	public static final String PURE_DOCKER_PIPELINE_DIR = "/workspace/pipelines";

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
	public static final String CONTAINER_BLJ_DIR = "/app/biolockj";

	/**
	 * {@link biolockj.Config} String property: {@value #DOCKER_IMG_VERSION} {@value #DOCKER_IMG_VERSION_DESC}
	 */
	public static final String DOCKER_IMG_VERSION = "docker.imageTag";
	private static final String DOCKER_IMG_VERSION_DESC = "indicate specific version of Docker images";
	
	/**
	 * {@link biolockj.Config} Boolean property: {@value #DOCKER_MOUNT_SOCK} {@value #DOCKER_MOUNT_SOCK_DESC}
	 */
	public static final String DOCKER_MOUNT_SOCK = "docker.mountSock";
	private static final String DOCKER_MOUNT_SOCK_DESC = "should /var/docker/docker.sock be mounted for modules.";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #SAVE_CONTAINER_ON_EXIT}<br>
	 * {@value #SAVE_CONTAINER_ON_EXIT_DESC}
	 */
	static final String SAVE_CONTAINER_ON_EXIT = "docker.saveContainerOnExit";
	private static final String SAVE_CONTAINER_ON_EXIT_DESC = "If Y, docker run command will NOT include the --rm flag";

	/**
	 * Name of the bash script function used to generate a new Docker container: {@value #SPAWN_DOCKER_CONTAINER}
	 */
	static final String SPAWN_DOCKER_CONTAINER = "spawnDockerContainer";

	/**
	 * {@link biolockj.Config} String property: {@value #DOCKER_IMG} {@value #DOCKER_IMG_DESC}
	 */
	public static final String DOCKER_IMG = "docker.imageName";
	private static final String DOCKER_IMG_DESC =
		"The name of a docker image to override whatever a module says to use.";

	/**
	 * {@link biolockj.Config} String property: {@value #DOCKER_HUB_USER}<br>
	 * {@value #DOCKER_HUB_USER_DESC}<br>
	 * Docker Hub URL: <a href="https://hub.docker.com" target="_top">https://hub.docker.com</a><br>
	 * By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own
	 * versions of these modules and add new modules in their own Docker Hub account.
	 */

	public static final String DOCKER_HUB_USER = "docker.imageOwner";
	private static final String DOCKER_HUB_USER_DESC = "name of the Docker Hub user that owns the docker containers";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #VERIFY_IMAGE} {@value #VERIFY_IMAGE_DESC}
	 */
	private static final String VERIFY_IMAGE = "docker.verifyImage";
	private static final String VERIFY_IMAGE_DESC = "In check dependencies, run a test to verify the docker image.";

	private static final String DOCK_RM_FLAG = "--rm";
	private static final File DOCKER_ENV_FLAG_FILE = new File( "/.dockerenv" );
	private static final String DOCKER_SOCKET = "/var/run/docker.sock";
	private static final Set<String[]> downloadDbCmdRegister = new HashSet<>();
	private static final String WRAP_LINE = " \\";
	private static final String DOCKER_DETACHED_FLAG = "--detach";
	private static final String ID_VAR = "containerId";
	private static final String SCRIPT_ID_VAR = "SCRIPT_ID";
	private static final String DOCKER_KEY = "docker";
	private static final String DOCKER_INFO_FILE = "dockerInfo.json";
	private static final String ALL_GOOD = "Everything is awesome!";
	private static final String USE_BASH = "/bin/bash -c";
	private static final String TEST_SCRIPT = "testDockerImage.sh";
}
