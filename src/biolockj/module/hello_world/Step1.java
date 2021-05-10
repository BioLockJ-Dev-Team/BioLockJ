package biolockj.module.hello_world;

import biolockj.module.ScriptModuleImpl;

/**
 * First set up a mostly-empty class. Make sure that the BioLockJ jar file is listed as dependency for the project so
 * that "import biolockj.module.ScriptModuleImpl" does not give any errors; this is usually accomplished through settings
 * in an IDE (Integrated Development Environment) such as Eclipse, Net Beans, IntelliJ IDEA.
 * 
 * Notice that this class is "abstract", meaning it does not meet all the requirements to actually be a BioModule, but
 * it could be extended by another class. For the sake of the tutorial, we are splitting the methods of our module
 * across multiple classes that build on each other. As you build your own simple module, you will only make one class.
 * 
 * @author Ivory Blakley
 *
 */
public abstract class Step1 extends ScriptModuleImpl {



}
