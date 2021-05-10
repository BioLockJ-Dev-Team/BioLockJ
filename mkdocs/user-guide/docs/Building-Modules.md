# Building New Modules

**Any Java class that implements the [BioModule](https://BioLockJ-Dev-Team.github.io/BioLockJ/javadocs/biolockj/module/BioModule.html) interface can be added to a BioLockJ pipeline.**

_In java, an interface is essentially a list of methods, often described as being similar to a contract.  When a class "implements an interface" that means has methods matching the method signatures specified in the interface.  The code in BioLockJ is structured to handle a module based on the methods in the BioModule interface._

Your module will almost certainly implement the BioModule interface extensions **[ApiModule interface](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/api/ApiModule.html)** and **[ScriptModule interface](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/ScriptModule.html)** as well.  Most of the methods required by these interfaces are provided by the BioModuleImple class. That parent class covers the common module mechanics, leaving you with a pretty short list of methods that you have to write.

The BioLockJ v1.0 implementation is currently focused on metagenomics analysis, but the generalized application framework is not limited to this domain.  Users can implement new BioModules to automate a wide variety of bioinformatics and report analytics.  The BioModule interface was designed so that users can develop new modules on their own.  

## Getting started

See the BioModule hello world tutorial for a basic example:   [https://github.com/BioLockJ-Dev-Team/BioLockJ/tree/main/src/biolockj/module/hello_world](https://github.com/BioLockJ-Dev-Team/BioLockJ/tree/main/src/biolockj/module/hello_world)
You can download the project and browse these source files in your IDE (recommended), or just look at them through your web browser for quick reference.
A series of class called Step1, Step2 etc, loosely line up with the steps below, with more details in the sections that follow.

#### Step 1
When you first start creating modules, you'll need to set up a development environment that allows your code to access the BioLockJ main code base. If you are already develop with java, this is very straight forward.  Beginners can expect to spend a little time in trail and error on day one.

#### Step 2
When you sit down to create a module, start with the basic documentation.  Create a new class implementing ApiModule and fill in the methods that will tell users the general aim of the module, who made it. and what properties it can take.

#### Step 3
Properties allow the user to specify behaviors for your module.  You'll need to document those too.  BioLockJ has a structured system for documenting properties.  It's best to follow an example.  Using this structure helps BioLockJ catch user input that might cause problems, and provide users helpful information about how to make corrections. It also makes it possible for you, or any other user who has your jar file, to use your module(s) with the shiny_biolockj GUI or to render standardized documentation.

#### Step 4
Explain what your module needs as input and what it makes as output.  At a minimum, this should be included in your documentation.  If this module works in conjunction with another specific module, then link to that module via pre/post requisite modules.

#### Step 5
Now we'll actually make the module do something.  Since we've already said what the properties are, and what the module is supposed to do, this is usually follows pretty swiftly.   Most modules will extend ScriptModuleImpl and their "main method" is buildScript().  In the buildScript() method, you assemble one or more bash commands that accomplish your module's task.  In a ScriptModule, the default executeTask() method (the "main method" of BioModules) is written to create and run scripts.  Many script modules also do something outside of their scripts, such as adding values to the metadata, these additional tasks are usually just added to the buildScript() method.


#### **Test**
Use a simple print statement in place of the main action so you can compile and test your module.

_Recommended: render the documentation (see details below) to see how the information is displayed for a user._

Try a test pipeline:

Save this text as: step3_test.config
```
#BioModule biolockj.module.hello_world.Step5
#BioModule biolockj.module.diy.GenMod

hello.myName = Jean-Luc
hello.exceitmentLevel = 3

genMod.codeLine = grep Hello ../../*.log
pipeline.inputTypes = none
```

Run:
```
EXT_MODS= <folder where your jar file is>
biolockj --external-modules ${EXT_MODS} step5.config
```

The test doesn't require specifying external modules, because it is the built-in example. It could run with `biolockj step3_test.config`.  But your own module is not built-in, so you will need to tell BioLockJ where to find your jar file.

The key points we want to test is the points we've covered so far:
Did the code compline?
Did BioLockJ run the pipeline?
Did the pipeline complete?




## Set up your development environment

Eclipse is the official IDE of choice for BioLockJ, but you are free to use any java IDE, such as Eclipse, Net Beans, Intellij IDEA, etc.
Your project will need to have the BioLockJ jar file as a dependency.  The simple approach is to download the current jar and add it to your dependencies list.
Alternatively, you can download the BioLockJ git repository and set that project as a dependency, which allows you navigate the BioLockJ code base, and update to the latest master without waiting for a release.



## Document your module

The BioLockJ API allows outside resources to get information about the BioLockJ program and any available modules.  

To interface with the API, your module will need to implement the **[ApiModule interface](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/api/ApiModule.html)**.

### API-generated html documentation

The BioLockJ documentation is stored in markdown files and rendered into html using mkdocs.  The BioLockJ API is designed to generate a markdown document, which is ready to be rendered into an html file using mkdocs.  

### Built-in descriptions

Override the `getCitationString()` method.  This should include citation information for any tool that your module wraps and a credit to yourself for creating the wrapper.

Override the `getDescription()` method to return a short description of what your module does, this should be one to two sentences.  For a more extensive description, including details about properties, expected inputs, assumptions, etc; override the `getDetails()` method (optional).  If your module has any pre-requisit modules or post-requisit modules, the modules Details should include the names of these modules and information about when and why these modules are added.

### Documenting Properties

If your module introduces any NEW configuration properties, those properties should registered to the module so the API can retrieve them.  Register properties using the `addNewProperty()` method in the modules constructor.  For example, the GenMod module defines three properties:
```java
public GenMod() {
	super();
	addNewProperty( PARAM, Properties.STRING_TYPE, "parameters to pass to the user's script" );
	addNewProperty( SCRIPT, Properties.FILE_PATH, "path to user script" );
	addNewProperty( LAUNCHER, Properties.STRING_TYPE, LAUNCHER_DESC );
}

protected static final String PARAM = "genMod.param";
protected static final String SCRIPT = "genMod.scriptPath";

/**
 * {@link biolockj.Config} property: {@value #LAUNCHER}<br>
 * {@value #LAUNCHER_DESC}
 */
protected static final String LAUNCHER = "genMod.launcher";
private static final String LAUNCHER_DESC = "Define executable language command if it is not included in your $PATH";
```
In this example, the descriptions for `PARAM` and `SCRIPT` are written in the `addNewProperty()` method.  The description for `LAUNCHER` is stored as its own string (`LAUNCHER_DESC`), and that string is referenced in the `addNewProperty` method and in the javadoc description for `LAUNCHER`. This rather verbose option IS NOT necessary, but it allows the description to be viewed through the api AND through javadocs, and IDE's; this is appropriate if you expect other classes to use the properties defined in your module.  

The descriptions for properties should be brief.  Additional details such as interactions between properties or the effects of different values should be part of the `getDetails()` method.  It should always be clear to a user what will happen if the value is "null".

If there is a logical default for the property, that can passed as an additional argument to `addNewProperty()`.  This value will only be used if there is no value given for the property in the config file (including any defaultProps layers and standard.properties).

If your module uses any general properties (beyond any uses by the the super class), then you should register it in the module's constructor using the `addGeneralProperty()` method.
```java
public QiimeClosedRefClassifier() {
	super();
	addGeneralProperty( Constants.EXE_AWK );
}
```
The existing description and type for this property (defined in biolockj.Properties) will be returned if the module is queried about this property.  For a list of general properties, run:<br> 
`biolockj_api listProps `

Finally, to very polished, you should override the `isValidProp()` method.  Be sure to include the call to super.
```java
@Override
public Boolean isValidProp( String property ) throws Exception {
	Boolean isValid = super.isValidProp( property );
	switch(property) {
		case HN2_KEEP_UNINTEGRATED:
			try {Config.getBoolean( this, HN2_KEEP_UNINTEGRATED );}
			catch(Exception e) { isValid = false; }
			isValid = true;
			break;
		case HN2_KEEP_UNMAPPED:
			try {Config.getBoolean( this, HN2_KEEP_UNMAPPED );}
			catch(Exception e) { isValid = false; }
			isValid = true;
			break;
	}
	return isValid;
}
```
In the example above, the Humann2Parser module uses two properties that are not used by any super class. The call to `super.isValidProp( property )` tests the property if it is used by a super class.  This class only adds checks for its newly defined properties.  Any property that is not tested, but is registered in the modules constructor will return true. This method is called through the API, and should be used to test one property at a time as if that is the only property in the config file. Tests to make sure that multiple properties are compatiable with each other should go in the `checkDependencies()` method.

### Generate user guide pages
For modules in the main BioLockJ project, the user guide pages are generated using the ApiModule methods as part of the deploy process.
Third party developers can use the same utilities to create matching documentation.

Suppose you have created one or more modules in a package `com.joesCode` and saved the compiled code in a jar file, `/Users/joe/dev/JoesMods.jar`.  
Set up a [mkdocs](https://www.mkdocs.org/) project:
```bash 
# See https://www.mkdocs.org/#installation
pip install mkdocs
mkdocs --version
mkdocs new joes-modules
mkdir joes-modules/docs/GENERATED
```
This mkdocs project will render markdown (.md) files into an html site.  Mkdocs supports a lot of really nice features, including a very nice default template.

Generate the .md files from your modules:
```bash
java -cp $BLJ/dist/BioLockJ.jar:/Users/joe/dev/JoesMods.jar \
    biolockj.api.BuildDocs \
    joes-modules/docs/GENERATED \
    com.joesCode
```

Put a link to your list of modules in the main index page.
```bash
cd joes-modules
echo "[view module list](GENERATED/all-modules.md)" >> docs/index.md 
```
The BuildDocs utility creates the .md files, but it assumes that these are part of a larger project, and you will need to make appropriate links to the generated pages from your main page.

Preview your user guide:
```bash
mkdocs serve
```
Open up `http://127.0.0.1:8000/` in your browser, and you'll see the default home page being displayed, with a link at the bottom to `view module list`, which links to a page listing all of the modules in the `joes.modules` package. 

You can build this documentation locally using `mkdocs build` and then push to your preferred hosting site, or set up a service such as [ReadTheDocs](https://readthedocs.org/) to render and host your documentation from your `docs` folder.


Even if you choose not to build user guide pages for your module, you should still implement the ApiModule interface.  Anyone who uses your module can generate the user guide pages if they want them, and even incorporate them into a custom copy of the main BioLockJ user guide.  Any other support program, such as a GUI, could use the the ApiModule methods as well.


## Coding your module

All BioLockJ modules extend [BioModuleImpl](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/BioModuleImpl.html), but few extend it directly.                          
Nearly all modules extend its child class [ScriptModuleImpl](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/ScriptModuleImpl.html).                        

Additional child classes are designed for particular types of modules:

Modules that perform a very basic function that is coded in java and takes advantage of BioLockJ methods should extend [JavaModuleImpl](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/JavaModuleImpl.html).

Modules that use BioLockJ methods to find/create resources/parameters to pass to an R script should implement [R_Module](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/report/r/R_Module.html).

Modules that wrap a classifier (a program that applies taxonomic classification to biological sequences) should extend [ClassifierModuleImpl](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/classifier/ClassifierModuleImpl.html).  These have a wide variety of output formats.  The Classifier module just runs the classifier, and the task of parsing that output into a standard table format is handled by a separate parser module, which extends [ParserModuleImpl](https://biolockj-dev-team.github.io/BioLockJ/javadocs/biolockj/module/implicit/parser/ParserModuleImpl.html).  Typically the classifier and parser module reference each other as pre/post-requisite modules, so a config might only specify the "FooBarClassifier" module, and the "FooBarParser" module is added to pipeline immediately after it automatically.  This relationship is a convenient way to handle processes that are related to each other, but have different needs in terms of memory, time, parallization strategy, etc.

Many of the methods in ParserModuleImpl are designed to be coupled with an appropriate [OtuNode](https://msioda.github.io/BioLockJ/docs/biolockj/node/OtuNode.html) class, which is a classifier-specific implementation that holds OTU information for 1 sequence.  Following this model is not required, but many classifiers this general pattern will prove convenient.


## Using External Modules

To use a module that you have created yourself or acquired from a third party, you need to:

1. Save the compiled code in a folder on your machine, for example: `/Users/joe/biolockjModules/JoesMods.jar` 
1. Include your module in the module run order in your config file, for example:<br>
`#BioModule com.joesCode.biolockj.RunTool`
<br>Be sure to include any properties your module needs in the config file.
1. Use the ` --external-modules <dir>` option  when you call biolockj:<br>
`biolockj --external-modules /Users/joe/biolockjModules myPipeline.properties`

Any other modules you have made or aquired can also be in the `/Users/joe/biolockjModules` folder.

## Finding and Sharing Modules

The official repository for external BioLockJ modules is [blj_ext_modules](https://github.com/BioLockJ-Dev-Team/blj_ext_modules).  Each module has a folder at the top level of the repository and should include the java code as well a config file to test the module alone, a test file to run a multi-module pipeline that includes the module, and (where applicable) a dockerfile.  This is work in progress.

