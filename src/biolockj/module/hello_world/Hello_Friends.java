package biolockj.module.hello_world;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Log;
import biolockj.Properties;
import biolockj.api.ApiModule;
import biolockj.exception.ConfigFormatException;
import biolockj.module.ScriptModuleImpl;

public class Hello_Friends extends ScriptModuleImpl implements ApiModule {

	public Hello_Friends() {
		super();
		addNewProperty( NAME_PROP, Properties.STRING_TYPE, NAME_PROP_DESC, NAME_PROP_DEFAULT );
		addNewProperty( EXCITE_PROP, Properties.INTEGER_TYPE, EXCITE_PROP_DESC );
	}

	/**
	 * {@value #NAME_PROP_DESC}; defaults to: {@value #NAME_PROP_DEFAULT}
	 */
	public static final String NAME_PROP = "helloFriends.friendsName";
	public static final String NAME_PROP_DESC = "A name to use instead of 'world'.";
	public static final String NAME_PROP_DEFAULT = "world";

	/**
	 * {@value #EXCITE_PROP_DESC}
	 */
	public static final String EXCITE_PROP = "helloFriends.excitmentLevel";
	public static final String EXCITE_PROP_DESC = "The number of ! to use with the phrase.";

	/**
	 * Be sure to touch any / all properties your module uses. Call your {@link #isValidProp(String)} method to check
	 * individual values. IF applicable, code additional tests here to make sure your properties are compatible.
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		isValidProp( NAME_PROP );
		isValidProp( EXCITE_PROP );
	}

	@Override
	public Boolean isValidProp( String property ) throws Exception {
		Boolean isValid = super.isValidProp( property );
		switch( property ) {
			case NAME_PROP:
				isValid = true;
				break;
			case EXCITE_PROP:
				try {
					Config.getPositiveInteger( this, EXCITE_PROP );
					isValid = true;
				} catch( Exception e ) {
					isValid = false;
				}
		}
		return isValid;
	}
	
	@Override
	public List<List<String>> buildScript( List<File> files ) throws Exception {
		List<List<String>> list = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		lines.add( "echo 'The message is: " + makeMessage() + "'" ); //captured by log file
		lines.add( "echo '" + makeMessage() + "' > ../output/hello.txt"); //captured by output file
		list.add( lines );
		return list;
	}
	
	/**
	 * Create a message based on the modules properties.
	 * @return
	 * @throws ConfigFormatException
	 */
	protected String makeMessage() throws ConfigFormatException {
		String name = Config.getString( this, NAME_PROP );
		String msg = "Hello, " + name + howExciting();
		Log.info( this.getClass(), msg );
		return msg;
	}
	
	/**
	 * Accesses the {@value biolockj.module.hello_world.Step3#EXCITE_PROP} and creates a string with that many !s, or
	 * just "." if the value is null.
	 * 
	 * Notice that this method access the value using the same method that was used in the
	 * {@link biolockj.module.BioModuleImpl#isValidProp(String)}
	 * 
	 * @return
	 * @throws ConfigFormatException
	 */
	protected String howExciting() throws ConfigFormatException {
		Integer val = Config.getPositiveInteger( this, EXCITE_PROP );
		if (val==null) return ".";
		return new String(new char[val]).replace("\0", "!");
	}
	
	@Override
	public String getDockerImageName() {
		return "ubuntu";
	}

	@Override
	public String getDescription() {
		return "Print the classic phrase: hello world. With some variation.";
	}

	@Override
	public String getDetails() {
		return "Say hello.  By default, use the classic phrase \"Hello, world.\".  Optionally supply a name such a John to print \"Hello, John.\".";
	}

	@Override
	public String getCitationString() {
		return "Module developed by Ivory Blakley.";
	}

}
