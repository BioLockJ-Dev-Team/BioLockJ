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
		String tempDir = module.getTempDir().getAbsolutePath();
		Log.info( DockerUtil.class, "tempDir String: " + tempDir);
		final List<String> lines = new ArrayList<>();
		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add(  SCRIPT_ID_VAR + "=$(basename $1)");
		lines.add(  ID_VAR + "=$(" + Config.getExe( module, Constants.EXE_DOCKER ) + " run " + DOCKER_DETACHED_FLAG + " "+ rmFlag( module ) + WRAP_LINE );
		lines.addAll(  getDockerVolumes( module )); 
		lines.add( " " + getDockerImage( module ) + WRAP_LINE );
		lines.add( "/bin/bash -c \"$1\" )" );
		lines.add( "echo \"Launched docker image: " + getDockerImage( module ) + "\"" );
		lines.add( "echo \"To execute module: " + module.getClass().getSimpleName() + "\"" );
		lines.add( "echo \"Docker container id: $" + ID_VAR + "\"" );
		lines.add( "echo \"${" + SCRIPT_ID_VAR + "}:" + DOCKER_KEY + ":${" + ID_VAR + "}\" >> " + startedFlag );
		lines.add( "docker inspect ${" + ID_VAR + "}" );
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
			String access = needsWritePermission(module, key) ? ":delegated" : ":ro" ;
			dockerVolumes.add( " -v " + key + ":" + volumeMap.get( key ) + access + WRAP_LINE );
		}
		
		Log.debug( DockerUtil.class, "Passed along volumes: " + dockerVolumes );
		return dockerVolumes;
	}

	private static boolean needsWritePermission(BioModule module, String key) throws DockerVolCreationException, ConfigPathException {
		if (module instanceof WritesOutsidePipeline ) {
			WritesOutsidePipeline wopMod = (WritesOutsidePipeline) module;
			Set<String> wopDirs = wopMod.getWriteDirs();
			if (wopDirs.contains( volumeMap.get( key ) )) {
				Log.info(DockerUtil.class, "The module [" + ModuleUtil.displaySignature( module ) + "] is granted write access to the folder [" + key + "]");
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
		return getDockerUser( module ) + "/" + getImageName( module ) + ":" + getImageTag(module);
	}

	/**
	 * Return the Docker Hub user ID. If none configured, return biolockj.
	 * 
	 * @param module BioModule
	 * @return Docker Hub User ID
	 */
	private static String getDockerUser( final BioModule module ) {
		String user = module.getDockerImageOwner();
		if (Config.getString( module, DOCKER_HUB_USER ) != null) user = Config.getString( module, DOCKER_HUB_USER );
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
	 * This information should come from the module, but config properties can be used to override the info in the module.
	 * 
	 * @param module BioModule
	 * @return Docker Image Name in the form <owner>/<image>:<tag>
	 */
	private static String getImageName( final BioModule module ) {
		String name = module.getDockerImageName();
		if (Config.getString( module, DOCKER_IMG ) != null) name=Config.getString( module, DOCKER_IMG );
		return name;
	}
	
	private static String getImageTag(final BioModule module) {
		String tag = module.getDockerImageTag();
		if (Config.getString( module, DOCKER_IMG_VERSION ) != null) tag = Config.getString( module, DOCKER_IMG_VERSION );
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
			if (volumeMap.get( key ).equals( DOCKER_PIPELINE_DIR ) ) pipelineKey = key;
			if ( DockerUtil.inAwsEnv() && volumeMap.get( key ).equals( DOCKER_BLJ_MOUNT_DIR ) ) pipelineKey = key;
		}
		if (pipelineKey == null) throw new DockerVolCreationException("no pipeline dir !");
		if ( pipelineKey != null && path.startsWith( pipelineKey ) ) return innerPath.replaceFirst( pipelineKey, vmap.get( pipelineKey ) );
		
		String bestMatch = null;
		int bestMatchLen = 0;
		for (String s : vmap.keySet()) {
			if ( path.startsWith( s ) && s.length() > bestMatchLen) {
					bestMatch = String.valueOf( s );
					bestMatchLen = s.length();
			}
		}
		if (bestMatch != null) {
			innerPath = innerPath.replaceFirst( bestMatch, vmap.get( bestMatch ) );
		}
		return innerPath;
	}
	
	public static String deContainerizePath(String innerPath) throws DockerVolCreationException {
		String hostPath = innerPath;
		if( DockerUtil.inDockerEnv() ) {
			TreeMap<String, String> vmap;
			vmap = getVolumeMap();
			for( String s: vmap.keySet() ) {
				if( innerPath.startsWith( vmap.get( s ) ) ) {
					hostPath = hostPath.replaceFirst( vmap.get( s ), s );
					break;
				}
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
	 * Register properties with the Properties class for API access.
	 * @throws API_Exception 
	 */
	public static void registerProps() throws API_Exception {
		Properties.registerProp( DOCKER_HUB_USER, Properties.STRING_TYPE, DOCKER_HUB_USER_DESC );
		Properties.registerProp( DOCKER_IMG, Properties.STRING_TYPE, DOCKER_IMG_DESC );
		Properties.registerProp( DOCKER_IMG_VERSION, Properties.STRING_TYPE, DOCKER_IMG_VERSION_DESC );
		Properties.registerProp( SAVE_CONTAINER_ON_EXIT, Properties.BOOLEAN_TYPE, SAVE_CONTAINER_ON_EXIT_DESC );
	}
	/**
	 * Let modules see property names.
	 */
	public static ArrayList<String> listProps(){
		ArrayList<String> props = new ArrayList<>();
		props.add( DOCKER_HUB_USER );
		props.add( DOCKER_IMG );
		props.add( DOCKER_IMG_VERSION );
		props.add( SAVE_CONTAINER_ON_EXIT );
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
	 * {@link biolockj.Config} String property: {@value #DOCKER_IMG_VERSION}
	 * {@value #DOCKER_IMG_VERSION_DESC}
	 */
	static final String DOCKER_IMG_VERSION = "docker.imageTag";
	private static final String DOCKER_IMG_VERSION_DESC = "indicate specific version of Docker images";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #SAVE_CONTAINER_ON_EXIT}<br>
	 * {@value #SAVE_CONTAINER_ON_EXIT_DESC}
	 */
	static final String SAVE_CONTAINER_ON_EXIT = "docker.saveContainerOnExit";
	private static final String SAVE_CONTAINER_ON_EXIT_DESC = "if ture, docker run command will NOT include the --rm flag";

	/**
	 * Name of the bash script function used to generate a new Docker container: {@value #SPAWN_DOCKER_CONTAINER}
	 */
	static final String SPAWN_DOCKER_CONTAINER = "spawnDockerContainer";

	/**
	 * {@link biolockj.Config} String property: {@value #DOCKER_IMG}
	 * {@value #DOCKER_IMG_DESC}
	 */
	private static final String DOCKER_IMG = "docker.imageName";
	private static final String DOCKER_IMG_DESC = "The name of a docker image to override whatever a module says to use.";
	
	/**
	 * {@link biolockj.Config} String property: {@value #DOCKER_HUB_USER}<br>
	 * {@value #DOCKER_HUB_USER_DESC}<br>
	 * Docker Hub URL: <a href="https://hub.docker.com" target="_top">https://hub.docker.com</a><br>
	 * By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own
	 * versions of these modules and add new modules in their own Docker Hub account.
	 */
	protected static final String DOCKER_HUB_USER = "docker.imgOwner";
	private static final String DOCKER_HUB_USER_DESC = "name of the Docker Hub user that owns the docker containers";

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
}
