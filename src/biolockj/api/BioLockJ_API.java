package biolockj.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Processor;
import biolockj.Properties;
import biolockj.exception.BioLockJException;
import biolockj.exception.ConfigException;
import biolockj.exception.ConfigPathException;
import biolockj.module.BioModule;
import biolockj.pipelines.PipelineUtil;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;
import biolockj.util.ModuleUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class BioLockJ_API {

	private static String query = null;
	// bash script
	private static final String BASH_ENTRY = "biolockj-api";
	
	// query 
	private static final String LAST_PIPELINE = "last-pipeline";
	private static final String LIST_MODULES = "listModules";
	private static final String LIST_API_MODULES = "listApiModules";
	private static final String LIST_PROPS = "listProps";
	private static final String LIST_ALL_PROPS = "listAllProps";
	private static final String VALIDATE_PROP = "isValidProp";
	private static final String PROP_TYPE = "propType";
	private static final String PROP_VALUE = "propValue";
	private static final String PROP_DESC = "describeProp";
	private static final String PROP_INFO = "propInfo";
	private static final String MODULE_INFO = "moduleInfo";
	private static final String LIST_MOUNTS = "listMounts";
	private static final String MOUNT_KEY = "mount";
	private static final String LIST_UPLOADS = "listUploads";
	private static final String HELP = "help";

	//options
	private static final String EXT_MODS_ARG = "external-modules";
	private static final String MODULE_ARG = "module";
	private static final String PROP_ARG = "property";
	private static final String VALUE_ARG = "value";
	private static final String CONFIG_ARG = "config";
	private static final String DEBUG_ARG = "verbose";
	
	private static boolean verbose = false;

	private BioLockJ_API() {}

	
	private static HashMap<String, String> parseOptions( String[] args ) throws API_Exception {
		HashMap<String, String> options = new HashMap<>();
		final String QUERY_KEY = "query";
		String argname = QUERY_KEY;
		for (String arg : args) {
			if (argname == null ) { //expecting option name
				if ( arg.startsWith( "--" ) ) {
					argname = arg.substring( 2 );
				}else{
					throw new API_Exception("Looking for option name, found unnamed value [ " + arg + " ].");
				}
			}else if( argname.equals( QUERY_KEY ) ){
				if (arg.startsWith( "--" )) throw new API_Exception("The query term should be given before any options. Option names start with \"--\", the query term does not.");
				query=arg;
				argname = null;
			}else { // expecting arg value
				options.put(argname, arg);
				argname = null;
			}
		}
		return options;
	}
	
	public static void main( String[] args ) throws Exception {
		try {
			if (args.length==0) {
				System.out.println( getHelp() );
				System.exit( 0 );
			}
			HashMap<String, String> options = parseOptions(args);
			
			if ( query == null ) throw new API_Exception("Cannot determine query.");
			if ( options.get(DEBUG_ARG) != null ) setVerbose( true );
			
			if ( options.get(EXT_MODS_ARG) != null) {
				System.exit( runSubProcess(options) );
			}
			
			String reply = "";
			switch( query ) {
				case LAST_PIPELINE:
					File BLJ_PROJ_DIR = new File( Processor.getBashVar( "BLJ_PROJ" ));
					reply = PipelineUtil.getMostRecentPipeline( BLJ_PROJ_DIR ).getAbsolutePath();
					break;
				case LIST_MODULES:
					unsupportedOption(query, options, new String[0]);
					reply = listToString( listModules() );
					break;
				case LIST_API_MODULES:
					unsupportedOption(query, options, new String[0]);
					reply = listToString( listApiModules( ) );
					break;
				case LIST_PROPS:
					unsupportedOption(query, options, new String[] {MODULE_ARG} );
					if ( options.get( MODULE_ARG ) == null ) {
						reply = listToString( listProps( ) );
					}else {
						reply = listToString( listModuleProps( options.get( MODULE_ARG ) ) );
					}
					break;
				case LIST_ALL_PROPS:
					unsupportedOption(query, options, new String[0]);
					Set<String> allProps = new HashSet<>();
					allProps.addAll(listProps( ));
					for (String mod : listApiModules()) {
						ApiModule tmp = (ApiModule) ModuleUtil.createModuleInstance( mod );
						allProps.addAll( tmp.listProps() );
					}
					ArrayList<String> allPropsOrderd = new ArrayList<>(allProps);
					Collections.sort(allPropsOrderd);
					reply = listToString( allPropsOrderd );
					break;
				case VALIDATE_PROP:
					unsupportedOption(query, options, new String[] {MODULE_ARG, PROP_ARG, VALUE_ARG} );
					requiredOption(query, options, new String[] {PROP_ARG, VALUE_ARG} );
					List<String> modules = options.get( MODULE_ARG ) == null ? new ArrayList<>() : Arrays.asList( options.get( MODULE_ARG ).split( "," ) );
					Boolean isGood = isValidProp(options.get(PROP_ARG), options.get(VALUE_ARG), modules );
					if (isGood == null) reply = "null";
					else reply = isGood.toString();
					break;
				case PROP_TYPE:
					unsupportedOption(query, options, new String[] {MODULE_ARG, PROP_ARG} );
					requiredOption(query, options, new String[] {PROP_ARG} );
					if ( options.get( MODULE_ARG ) == null ) {
						reply = Properties.getPropertyType( options.get( PROP_ARG ) );
					}else {
						reply = propTypeModule( options.get( PROP_ARG ), options.get( MODULE_ARG ) );
					}
					break;
				case PROP_DESC:
					unsupportedOption(query, options, new String[] {MODULE_ARG, PROP_ARG} );
					requiredOption(query, options, new String[] {PROP_ARG} );
					if ( options.get( MODULE_ARG ) == null ) {
						reply = Properties.getDescription( options.get( PROP_ARG ) );
					}else {
						reply = propDescModule( options.get( PROP_ARG ), options.get( MODULE_ARG ) );
					}
					break;
				case PROP_VALUE:
					unsupportedOption(query, options, new String[] {PROP_ARG, CONFIG_ARG, MODULE_ARG} );
					requiredOption(query, options, new String[] {PROP_ARG} );
					BioModule mod = null;
					if ( options.get( MODULE_ARG ) != null ) {
						mod = ModuleUtil.createModuleInstance( options.get( MODULE_ARG ) );
					}
					reply = propValue( options.get( PROP_ARG ), options.get( CONFIG_ARG ), mod);
					break;
				case PROP_INFO:
					unsupportedOption(query, options, new String[0]);
					reply = propInfo().toString(2);
					break;
				case MODULE_INFO:
					unsupportedOption(query, options, new String[0]);
					reply = moduleInfo().toString(2);
					break;
				case LIST_MOUNTS:
					unsupportedOption(query, options, new String[] {CONFIG_ARG} );
					requiredOption(query, options, new String[] {CONFIG_ARG} );
					reply = listToString( listMounts(options.get( CONFIG_ARG )) );
					break;
				case LIST_UPLOADS:
					unsupportedOption(query, options, new String[] {CONFIG_ARG} );
					requiredOption(query, options, new String[] {CONFIG_ARG} );
					reply = listToString( listUploads(options.get( CONFIG_ARG )) );
					break;
				case HELP:
					reply = getHelp();
					break;
				default:
					throw new API_Exception( "\"" + query + "\" is not a recognized query term." );
			}
			System.out.println( reply );
		} catch( API_Exception | BioLockJException ex ) {
			System.err.println( ex.getMessage() );
			System.err.println( "See help menu using: " + BASH_ENTRY + " " + HELP);
		} catch( Exception e ) {
			System.err.println( "An unexpected error occurred; your query could not be processed." );
			throw e;
		}
	}
	
	/**
	 * If the user passed in options that this querry does not support, throw an informative error.
	 * @param query
	 * @param options
	 * @param supportedOpts
	 * @throws API_Exception
	 */
	private static void unsupportedOption(String query, HashMap<String, String> options, String[] supportedOpts) throws API_Exception{
		List<String> supported = new ArrayList<>();
		supported.addAll( Arrays.asList( supportedOpts ) );
		if ( !supported.contains( DEBUG_ARG ) ) supported.add( DEBUG_ARG );
		for (String opt : options.keySet()) {
			if ( !supported.contains( opt ) && options.get( opt ) != null) {
				throw new API_Exception("The query [" + query + "] does not support the [--" + opt + "] option."
					+ System.lineSeparator() + "Found [ " + opt + " = " + options.get( opt ) + " ].");
			}
		}
	}
	
	/**
	 * If the user failed to pass in the options that this query requires, throw an informative error.
	 * @param query
	 * @param options
	 * @param supportedOpts
	 * @throws API_Exception
	 */
	private static void requiredOption(String query, HashMap<String, String> options, String[] requiredOpts) throws API_Exception{
		List<String> reqrd = new ArrayList<>();
		reqrd.addAll( Arrays.asList( requiredOpts ) );
		for (String opt : reqrd) {
			if ( options.get( opt ) == null) {
				throw new API_Exception("The query [" + query + "] requires the [--" + opt + "] option.");
			}
		}
	}
	
	public static void setVerbose(boolean writeLots) {
		verbose = writeLots;
	}
	
	private static String listToString( Iterable<? extends Object> elements ) {
		ArrayList<String> output = new ArrayList<>();
		for (Object elem : elements ) {
			if (elem instanceof Class) {
				output.add( ( (Class<?>) elem).getName() );
			}else if (elem instanceof CharSequence) {
				output.add( ( (CharSequence) elem).toString() );
			}else {
				output.add( elem.toString() );
			}
		}
		return( String.join( System.lineSeparator(), output ) );
	}
	
	/**
	 * The {@value #EXT_MODS_ARG} option allows the use to expect the class path. 
	 * To use this, a new java instance much be called with the extended path.
	 * @param options
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ConfigPathException
	 */
	private static int runSubProcess(HashMap<String, String> options) throws IOException, InterruptedException, ConfigPathException {
		File BLJ_JAR = new File( (new File(BioLockJUtil.getBljDir(), "dist")), "BioLockJ.jar");
		StringBuilder cmd = new StringBuilder();
		cmd.append( "java -cp " + BLJ_JAR.getAbsolutePath() + ":" + options.get( EXT_MODS_ARG ) + "/*" + " biolockj.api.BioLockJ_API " + query ) ;
		options.remove( EXT_MODS_ARG );
		for (String key : options.keySet()) {
			cmd.append( " --" + key + " " + options.get( key ) );
		}
		Process p = Runtime.getRuntime().exec( cmd.toString() );
		final BufferedReader brOut = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		final BufferedReader brErr = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );
		String out = null;
		while( ( out = brOut.readLine() ) != null ) System.out.println(out);
		String err = null;
		while( ( err = brErr.readLine() ) != null ) System.err.println(err);
		p.waitFor();
		p.destroy();
		return(p.exitValue());
	}
	
	
	/**
	 * Return a list of all the modules on the class path.
	 * @param prefix - Single argument passed to the constructor of the Reflections class.
	 * @return
	 * @throws Exception 
	 */
	public static List<String> listModules(String prefix) throws Exception {
		List<String> allBioModules = new ArrayList<>();

		Reflections reflections = supressStdOut_createReflections(prefix);
		
		Set<Class<? extends BioModule>> subTypes = reflections.getSubTypesOf( BioModule.class );
		
		for (Class<? extends BioModule> st : subTypes ) {
			try {
				BioModule tmp = (BioModule) Class.forName( st.getName() ).newInstance();
				allBioModules.add( tmp.getClass().getName() );
			}catch(InstantiationException | ExceptionInInitializerError ex) {
				//System.err.println("The class [" + st.getName() + "] is in scope, but cannot be instantiated.");
			}	
		}
		
		Collections.sort(allBioModules);
		return allBioModules;
	}
	public static List<String> listModules() throws Exception {
		return listModules("");
	}
	
	private static Reflections supressStdOut_createReflections( String prefix ) {
		PrintStream classicOut = System.out;
		PrintStream classicErr = System.err;

		if( verbose ) {
			System.setOut( classicErr );
		} else {
			System.setOut( new PrintStream( new OutputStream() {
				public void write( int b ) {}
			} ) );
			System.setErr( new PrintStream( new OutputStream() {
				public void write( int b ) {}
			} ) );
		}

		Reflections reflections = null;

		try {
			if( prefix.trim().length() == 0 ) {

				// example code modified from
				// https://www.codota.com/code/java/classes/org.reflections.util.ConfigurationBuilder
				Set<URL> classpath = new HashSet<>();
				classpath.addAll( ClasspathHelper.forClassLoader() );
				classpath.addAll( ClasspathHelper.forJavaClassPath() );
				classpath.addAll( ClasspathHelper.forManifest() );
				classpath.addAll( ClasspathHelper.forPackage( "" ) );

				reflections = new Reflections( new ConfigurationBuilder().setUrls( classpath ).useParallelExecutor()
					.filterInputsBy( FilterBuilder.parsePackages( "-java, -javax, -sun, -com.sun" ) )
					.setScanners( new SubTypesScanner(), new TypeAnnotationsScanner() ) );

			} else {
				reflections = new Reflections( prefix );
			}
		} catch( Exception ex ) {

		}

		System.setOut( classicOut );
		System.setErr( classicErr );

		return reflections;
	}
	
	/**
	 * Like listModules, but only include modules that implement the ApiModule interface.
	 * @param args
	 * @return
	 * @throws Exception 
	 */
	public static List<String> listApiModules() throws Exception {
		List<String> allBioModules = listModules();
		List<String> apiModules = new ArrayList<>();
		for (String mod : allBioModules ) {
			BioModule tmp = ModuleUtil.createModuleInstance( mod );
			if (tmp instanceof ApiModule) {
				apiModules.add( tmp.getClass().getName() );
			}
		}
		return apiModules;
	}
	
	
	public static List<String> listProps() throws API_Exception{
		List<String> allProps = new ArrayList<>();
		allProps.addAll( Properties.getPropTypeMap().keySet() );
		Collections.sort(allProps);
		return allProps;
	}
	public static List<String> listModuleProps(String mod) throws Exception {
		List<String> allProps = new ArrayList<>();
		BioModule tmp = ModuleUtil.createModuleInstance( mod );
		if (tmp instanceof ApiModule) {
			allProps.addAll( ( (ApiModule) tmp ).listProps() );
		}
		return allProps;
	}
	
	/**
	 * Returns true if the 
	 * @param prop - a config property
	 * @param val - a proposed value for prop
	 * @param modules - if not empty, the property/val pair is validated by one or more modules, in addition to the biolockj backbone.
	 * @return
	 * @throws Exception 
	 */
	public static Boolean isValidProp(String prop, String val, List<String> modules) throws Exception {
		initConfig();
		Config.setConfigProperty( prop, val );
		
		Boolean isValid = Properties.isValidProp(prop);
		
		if (modules.size() > 0) {
			for (String mod : modules) {
				BioModule tmp = ModuleUtil.createModuleInstance( mod );
				if (tmp instanceof ApiModule) {
					Boolean modVote;
					try {
						modVote = ( (ApiModule) tmp).isValidProp( prop );
					}catch(ConfigException e) {
						modVote = false;
					}
					if (isValid == null) isValid = modVote;
					else if (modVote != null) isValid = modVote && isValid;
				}
			}
		}
		return isValid;
	}
	
	/**
	 * Returns a string describing the format type that required for the value of the property given.
	 * The first String in args is taken as the property of interest.
	 * The second string is a module 
	 * the property type is queried for that module rather than the BioLockJ backbone.
	 * @param args - 
	 * @return
	 * @throws API_Exception 
	 */
	public static String propTypeModule(String prop, String module) throws API_Exception {
		String type = "";
		try {
			BioModule tmp = ModuleUtil.createModuleInstance( module );
			if (tmp instanceof ApiModule) {
				type = ( (ApiModule) tmp ).getPropType( prop );
			}
		}catch(ClassNotFoundException ex ) {
			throw new API_Exception("Module class [" + module + "] could not be found.");
		} catch( Exception e ) {
			throw new API_Exception("There was ap problem getting the type for ["+ prop + "] from class [" + module + "].");
		}
		return type; 
	}
	
	public static String propDescModule(String prop, String module) throws API_Exception {
		String desc = "";
		try {
			BioModule tmp = ModuleUtil.createModuleInstance( module );
			if (tmp instanceof ApiModule) {
				desc = ( (ApiModule) tmp ).getDescription( prop );
			}
		}catch(ClassNotFoundException ex ) {
			throw new API_Exception("Module class [" + module + "] could not be found.");
		} catch( Exception e ) {
			throw new API_Exception("There was ap problem getting the description for ["+ prop + "] from class [" + module + "].");
		}
		return desc; 
	}
	
	/**
	 * Returns the value for that property (first String)
	 * If a second argument is given, it is assumed to be the primary config file.
	 * @param property - the property to get the value of
	 * @param config - (can be null) the config file to read in to establish property values
	 * @param module - (can be null) a module to pass in a context when getting the value
	 * @return
	 * @throws Exception 
	 */
	public static String propValue(String property, String config, BioModule module) throws Exception {
		if (config != null) initConfig(config);
		else initConfig();
		String value = Config.getString( null, property );
		if ( value == null && module != null && module instanceof ApiModule) {
			value = ((ApiModule) module).getPropDefault( property ); //may still be null
		}
		return value;
	}
	public static String propValue(String property, BioModule module) throws Exception {
		return propValue(property, null, module);
	}
	public static String propValue(String property) throws Exception {
		return propValue(property, null, null);
	}
	
	private static void initConfig(String path) throws Exception {
		File config = new File(path);
		if (!config.exists()) throw new API_Exception( "Cannot find configuration file: " + path );
		initConfig(config);
	}
	private static void initConfig(File config) throws Exception {
		Config.partiallyInitialize(config);
	}
	private static void initConfig() throws Exception {
		File tempConfig = File.createTempFile( "tempconfig", "properties" );
		initConfig(tempConfig);
		tempConfig.delete();
	}
	
	/**
	 * For each of the properties registered to the Properties class, 
	 * report the property name, type, description and default value.
	 * @return
	 * @throws Exception
	 */
	public static JSONArray propInfo() throws Exception {
		HashMap<String, String> propDescMap = Properties.getPropDescMap();
		List<String> props = new ArrayList<>(propDescMap.keySet());
		Collections.sort(props);
		initConfig();
		JSONArray jarray = new JSONArray();
		for (String prop : props) {
			HashMap<String, String> propAtts = new HashMap<>();
			propAtts.put("property", prop);
			propAtts.put("type", Properties.getPropertyType( prop ));
			propAtts.put("description", Properties.getDescription( prop ));
			propAtts.put("default", propValue(prop));
			JSONObject jobj = new JSONObject( propAtts );
			jarray.put( jobj );
		}
		return jarray;
	}
	
	/**
	 * Returns a json formatted list of all modules and for each module that 
	 * implements the gui interface, it lists the props used by the module,
	 * and for each prop the info.
	 * @param args
	 * @return
	 * @throws Exception 
	 */
	public static JSONArray moduleInfo() throws Exception {
		initConfig();
		List<String> modules = listApiModules();
		JSONArray modsArray = new JSONArray();
		for (String mod : modules) {
			BioModule tmp = biolockj.util.ModuleUtil.createModuleInstance( mod );
			HashMap<String, Object> modAtts = new HashMap<>();
			
			modAtts.put("usage", "#BioModule " + mod );		
			try {
				modAtts.put("pre-req modules", tmp.getPreRequisiteModules() );
				modAtts.put("post-req modules", tmp.getPostRequisiteModules() );
			}catch(Exception e) {}
			
			if (tmp instanceof ApiModule) {
				ApiModule atmp = (ApiModule) tmp;
				modAtts.put("description", atmp.getDescription() );
				modAtts.put("details", atmp.getDetails() );
				modAtts.put("citation", atmp.getCitationString() );
				modAtts.put("title", atmp.getTitle() );
				modAtts.put("menuPlacement", atmp.getMenuPlacement() );
				
				List<String> props = listModuleProps( mod );
				JSONArray modProps = new JSONArray();
				for (String prop : props) {
					HashMap<String, String> propAtts = new HashMap<>();
					propAtts.put("property", prop);
					propAtts.put("type", atmp.getPropType( prop ));
					propAtts.put("description", atmp.getDescription( prop ));
					propAtts.put("default", atmp.getPropDefault( prop ));
					modProps.put( propAtts );
				}
				modAtts.put( "properties", modProps );
			}

			JSONObject jModAtts = new JSONObject( modAtts );
			modsArray.put( jModAtts );
		}
		return modsArray; //return json; convert to string in switch case.
	}
	
	private static List<String> listFilesFromConfig(final String config, final String mountOrUpload) throws Exception {		
		File configFile = Config.getExistingFileObjectFromPath( config, false, true );
		Set<String> mounts = new HashSet<>();
		Set<String> uploads = new HashSet<>();
		
		if ( configFile.exists() ) {
			mounts.add( DockerUtil.deContainerizePath(configFile).getParentFile().getAbsolutePath() );
			uploads.add( DockerUtil.deContainerizePath(configFile).getAbsolutePath() );
			initConfig(configFile.getAbsolutePath());
			Map<String, String> props = Config.getProperties();
			Set<String> keys = props.keySet();
			Set<String> vals = new HashSet<>();
			for (String key : keys) {
				vals.addAll( Config.getList( null, key ) ); //this converts any $VAR refs
			}
			//System.err.println("Reviewing " + vals.size() + " property values to see if any are files on the local system.");
			for (String val : vals ) {
				//System.err.println("Consider the value: " + val );
				File testFile = Config.getFileObjectFromPath( val, true, true );
				if (testFile.getAbsolutePath().equals( Config.replaceEnvVar( Constants.STANDARD_CONFIG_PATH) ) 
								|| testFile.getAbsolutePath().equals( Config.replaceEnvVar( Constants.DOCKER_CONFIG_PATH )) ) continue;
				boolean fileExists = testFile.exists();
				if (!fileExists && DockerUtil.inDockerEnv()) fileExists = existsOnHost(testFile);
				if (fileExists) {
					//System.err.println("This exists: " + val);
					File hostForm = DockerUtil.deContainerizePath( testFile );
					if (testFile.isDirectory()) {
						mounts.add( hostForm.getAbsolutePath() );
						uploads.add( hostForm.getAbsolutePath() );
					}else {
						mounts.add( hostForm.getParentFile().getAbsolutePath() );
						uploads.add( hostForm.getAbsolutePath() );
					}
				}
			}
		}else if (DockerUtil.inDockerEnv() && existsOnHost(configFile) ) {
			System.err.println("The file " + configFile.getName() + " exists on the host, but not in the container.");
			String container = DockerUtil.getMainDockerImage();
			String query = mountOrUpload.equals( MOUNT_KEY ) ? LIST_MOUNTS : LIST_UPLOADS;
			String target = "/mnt/tmpConfigDirForApi";
			if ( (new File(target)).exists() ) throw new API_Exception( "Avoid inf loop: don't launch a new container in the current one already has " + target );
			String cmd = "docker run --rm --mount type=bind,source=" + configFile.getParentFile().getAbsolutePath() 
							+ ",target=" + target + " " + container + " " + query + " --" + CONFIG_ARG + " " +  target + "/" + configFile.getName();
			System.err.println("subcommand: " + cmd);
			final Process p = Runtime.getRuntime().exec( cmd );
			final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			String s = null;
			while( ( s = br.readLine() ) != null )
				if( !s.trim().isEmpty() ) {
					mounts.add(s);
					uploads.add( s );
				}
			p.waitFor();
			p.destroy();
		}else {
			throw new ConfigPathException( configFile );
		}
		List<String> returnList = new ArrayList<>();
		if (mountOrUpload.equals( MOUNT_KEY ) ) returnList.addAll( mounts );
		else returnList.addAll( uploads );
		return removeSubdirs(returnList);
	}
	public static List<String> listMounts(String config) throws Exception {
		return listFilesFromConfig(config, MOUNT_KEY);
	}
	public static List<String> listUploads(String config) throws Exception {
		return listFilesFromConfig(config, "upload");
	}
	private static boolean existsOnHost(File testFile) {
		boolean answer = false;
		try {
			String YES = "yes";
			String cmd = "docker run --rm --mount type=bind,source=" + testFile.getParentFile().getAbsolutePath() 
							+ ",target=/tmpTest ubuntu [ -f /tmpTest/" + testFile.getName() + " ] || [ -d /tmpTest/" 
							+ testFile.getName() + " ] && echo " + YES + " || echo no ";
			//System.err.println("test command: " + cmd);
			final Process p = Runtime.getRuntime().exec( cmd );
			final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			String returnVal = null;
			String s = null;
			while( ( s = br.readLine() ) != null )
				if( !s.trim().isEmpty() ) {
					if( returnVal == null ) returnVal = s;
				}
			p.waitFor();
			p.destroy();
			answer = returnVal.equals( YES );
			//System.err.println("answer: " + answer);
		}catch(Exception ex){
			//System.err.println("Encountered an error while testing to see if file exists on host: " + testFile.getName());
			//System.err.println(ex.getMessage());
			ex.getSuppressed();
		}
		return answer;
	}
	
	private static List<String> removeSubdirs(final List<String> dirs){
		Collections.sort(dirs);
		List<String> newList = new ArrayList<>();
		newList.add( dirs.get( 0 ) );
		for (String d : dirs) {
			boolean alreadyHaveParentDir = false;
			for (String parent : newList) {
				if (d.equals( parent )) alreadyHaveParentDir = true;
				if ( d.startsWith( parent ) && 
					d.length() > parent.length() &&
					d.charAt( parent.length() )==File.separatorChar ) {
					alreadyHaveParentDir = true;
				}
			}
			if (!alreadyHaveParentDir) newList.add( d );
		}
		return newList;
	}
	
	/**
	 * Print the help menu explaining how to use this tool.
	 * @return
	 */
	private static String getHelp() {
		String EXT_MODS_OPTION = "--" + EXT_MODS_ARG + " <dir>";
		String MODULE_OPTION = "--" + MODULE_ARG + " <module_path>";
		String PROP_OPTION = "--" + PROP_ARG + " <property>";
		String VALUE_OPTION = "--" + VALUE_ARG + " <value>";
		String CONFIG_OPTION = "--" + CONFIG_ARG + " <file>";
		String DEBUG_OPTION = "--" + DEBUG_ARG + " true";
		
		StringBuffer sb = new StringBuffer();
		sb.append( "BioLockJ API " + BioLockJUtil.getVersion( ) + " - UNCC Fodor Lab" +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "Usage:" +System.lineSeparator() );
		sb.append( "(bash)" +System.lineSeparator() );
		sb.append( BASH_ENTRY + " <query> [options...]" +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "(java)" +System.lineSeparator() );
		sb.append( "java -cp /path/to/BioLockJ.jar biolockj.api.BioLockJ_API <query> [options...]" +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "For some uses, redirecting stderr is recommended:" +System.lineSeparator() );
		sb.append( BASH_ENTRY + " <query> [options...]  2> /dev/null" +System.lineSeparator() );
		sb.append( "Options shown in [ ] are optional for a given query." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "Use " + BASH_ENTRY + " without args to get help menu." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "Options:" + System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + EXT_MODS_OPTION +System.lineSeparator() );
		sb.append( "        path to a directory containing additional modules" +System.lineSeparator() );
		sb.append( "  " + MODULE_OPTION +System.lineSeparator() );
		sb.append( "        class path for a specific module" +System.lineSeparator() );
		sb.append( "  " + PROP_OPTION +System.lineSeparator() );
		sb.append( "        a specific property" +System.lineSeparator() );
		sb.append( "  " + VALUE_OPTION +System.lineSeparator() );
		sb.append( "        a vlue to use for a specific property" +System.lineSeparator() );
		sb.append( "  " + CONFIG_OPTION +System.lineSeparator() );
		sb.append( "        file path for a configuration file giving one or more property values" +System.lineSeparator() );
		sb.append( "  " + DEBUG_OPTION +System.lineSeparator() );
		sb.append( "        flag indicating that all messages should go to standard err, including some that are typically disabled." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "query:" + System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + LAST_PIPELINE +System.lineSeparator() );
		sb.append( "        Returns the path to the most recent pipeline." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + LIST_MODULES + " [ " + EXT_MODS_OPTION + " ]" +System.lineSeparator() );
		sb.append( "        Returns a list of classpaths to the classes that extend BioModule." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + LIST_API_MODULES + " [" + EXT_MODS_OPTION + " ]" +System.lineSeparator() );
		sb.append( "        Like listModules but limit list to modules that implement the ApiModule interface." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + LIST_PROPS + " [ " + MODULE_OPTION + " ]" +System.lineSeparator() );
		sb.append( "        Returns a list of properties." +System.lineSeparator() );
		sb.append( "        If no args, it returns the list of properties used by the BioLockJ backbone." +System.lineSeparator() );
		sb.append( "        If a modules is given, then it returns a list of all properties used by" +System.lineSeparator() );
		sb.append( "        that module." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + LIST_ALL_PROPS + " [ " + EXT_MODS_OPTION + " ]" +System.lineSeparator() );
		sb.append( "        Returns a list of all properties, include all backbone properties and all module properties." +System.lineSeparator());
		sb.append( "        Optionally supply the path to a directory containing additional modules to include their properties." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + PROP_TYPE + " " + PROP_OPTION + " [ " + MODULE_OPTION + " [ " + EXT_MODS_OPTION + " ] ]" +System.lineSeparator() );
		sb.append( "        Returns the type expected for the property: String, list, integer, positive number, etc." +System.lineSeparator() );
		sb.append( "        If a module is supplied, then the modules propType method is used." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + PROP_DESC + " " + PROP_OPTION + " [ " + MODULE_OPTION + " [ " + EXT_MODS_OPTION + " ] ]" +System.lineSeparator() );
		sb.append( "        Returns a description of the property." +System.lineSeparator() );
		sb.append( "        If a module is supplied, then the modules getDescription method is used." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + PROP_VALUE + " " + PROP_OPTION + " [ " + CONFIG_OPTION + " ] [ "+ MODULE_OPTION + " ]" +System.lineSeparator() );
		sb.append( "        Returns the value for that property given that config file (optional) or " +System.lineSeparator() );
		sb.append( "        no config file (ie the default value)" +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + VALIDATE_PROP + " " + PROP_OPTION + " " + VALUE_OPTION + " [ " + MODULE_OPTION + "  [" + EXT_MODS_OPTION + "] ]" +System.lineSeparator() );
		sb.append( "        T/F/NA. Returns true if the value (val) for the property (prop) is valid;" +System.lineSeparator() );
		sb.append( "        false if prop is a property but val is not a valid value," +System.lineSeparator() );
		sb.append( "        and NA if prop is not a recognized property." +System.lineSeparator() );
		sb.append( "        IF a module is supplied, then additionally call the validateProp(key, value)" +System.lineSeparator() );
		sb.append( "        for that module, or for EACH module if a comma-separated list is given." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + PROP_INFO + System.lineSeparator() );
		sb.append( "        Returns a json formatted list of the general properties (listProps)" +System.lineSeparator() );
		sb.append( "        with the type, descrption and default for each property" +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + MODULE_INFO + " [" + EXT_MODS_OPTION + "]" +System.lineSeparator() );
		sb.append( "        Returns a json formatted list of all modules and for each module that " +System.lineSeparator() );
		sb.append( "        implements the ApiModule interface, it lists the props used by the module," +System.lineSeparator() );
		sb.append( "        and for each prop the type, descrption and default." +System.lineSeparator() );
		sb.append( System.lineSeparator() );
		sb.append( "  " + LIST_MOUNTS + " " + CONFIG_OPTION + "" +System.lineSeparator() );
		sb.append( "        Returns a list of directories that would need to be mounted in order for " +System.lineSeparator() );
		sb.append( "        the files listed in the config file to be available to a pipeline running in docker." +System.lineSeparator());
		sb.append( System.lineSeparator() );
		sb.append( "  " + LIST_UPLOADS + " " + CONFIG_OPTION + "" +System.lineSeparator() );
		sb.append( "        Returns a list of file and directories that would need to be uploaded in order for " +System.lineSeparator() );
		sb.append( "        the files listed in the config file to be available to a pipeline running in the cloud." +System.lineSeparator());
		sb.append( System.lineSeparator() );
		sb.append( "  " + HELP + "  (or no args)" +System.lineSeparator() );
		sb.append( "        Print help menu." +System.lineSeparator() );
		sb.append( System.lineSeparator() );

		return sb.toString(); 
	}

}
