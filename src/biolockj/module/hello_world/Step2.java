package biolockj.module.hello_world;

import biolockj.api.ApiModule;

/**
 * Start with the basic documentation.  What are you aiming to do?  
 * You have to do this sooner or later, and its better to do it right out the gate.
 * 
 * In your simple module, add "implements ApiModule" like you see in this class.
 * You can copy/paste these methods into your own module, or use your IDE to add overriding methods.
 * For example, in Eclipse, right click in the class body > source > Override / Implement methods ...  
 * will open a window to let you select from a list of methods to insert based on the parent class / interfaces.
 * 
 * All of the methods in this step return String.  Even if you have never used java before, you can see where you need to fill in your own text.
 * 
 * @author Ivory Blakley
 *
 */
public abstract class Step2 extends Step1 implements ApiModule {

	@Override
	public String getDescription() {
		// TODO Fill in a short (one line) description of what this module does, often phrased as a command.
		return "Print the classic phrase: hello world.";
	}

	@Override
	public String getDetails() {
		// TODO Optional. Fill in more details. Tell future users how this module behaves in different settings.
		return "Say hello.  By default, use the classic phrase \"hello, world.\".  Optionally supply a name such a John to print \"hello, John.\".";
	}

	@Override
	public String getCitationString() {
		// TODO who are you? and if you are wrapping a pre-existing tool, who made that?
		return "Module developed by Ivory Blakley.";
	}

}
