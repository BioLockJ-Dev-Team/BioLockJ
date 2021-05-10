package biolockj.module.hello_world;

import biolockj.Config;
import biolockj.Properties;

/**
 * Even a very simple module probably uses at least one property. Properties are key/value pairs given in the config
 * file that control what a module does. Its really important to document those!
 * 
 * You could just write it all out in your getDetails() method. However is much better to take the time to describe your
 * properties through these ApiModule methods. These methods allow the BioLockJ automated documentation process to
 * generate uniformly formated documentation for all modules, and allows users and GUIs to interact with your module via
 * the command line.
 * 
 * There are many properties that your module uses that you have nothing to do with. They are are used in methods that
 * are inherited from the parent classes that handle most of the baseline module mechanics. You cover those when you
 * reference super().
 * 
 * @author Ivory Blakley
 *
 */
public abstract class Step3 extends Step2 {

	// Property names always start with a lower case letter.
	// Typically, the property name has two parts. The first part is the lower-case name of the module or utility class
	// that uses it, and the last part is descriptive of the property.
	/**
	 * {@value #NAME_PROP_DESC}; defaults to: {@value #NAME_PROP_DEFAULT}
	 */
	public static final String NAME_PROP = "hello.myName";
	public static final String NAME_PROP_DESC = "A name to use instead of 'world'.";
	public static final String NAME_PROP_DEFAULT = "world";

	/**
	 * {@value #EXCITE_PROP_DESC}
	 */
	public static final String EXCITE_PROP = "hello.excitmentLevel";
	public static final String EXCITE_PROP_DESC = "The number of ! to use with the phrase.";

	// The constructor.
	// A method that has the same name as the class and returns nothing (not even void) is a constructor.
	// In a BioModule, this is where we formally declare the properties that this module can use.
	public Step3() {
		super();
		addNewProperty( NAME_PROP, Properties.STRING_TYPE, NAME_PROP_DESC, NAME_PROP_DEFAULT );
		addNewProperty( EXCITE_PROP, Properties.INTEGER_TYPE, EXCITE_PROP_DESC );
	}

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
				// there's no value that would make this module fail.
				isValid = true;
				break;
			case EXCITE_PROP:
				try {
					// The Config.get* methods have built-in checks for type.
					// As long as its a positive integer its good.
					// If the value is not a positive integer, ( if its "apple" or "-1") then the getPositiveInteger
					// method will throw an exception.
					Config.getPositiveInteger( this, EXCITE_PROP );
					isValid = true;
				} catch( Exception e ) {
					isValid = false;
				}
		}
		return isValid;
	}

}
