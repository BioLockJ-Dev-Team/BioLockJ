# BioLockJ API                                      
                                      
BioLockJ comes with an API.                                      
                                      
For the most up-to-date information about how to use the API, see the help menu:                                      
                   
                   
`biolockj-api help`                   
                   
```                   
BioLockJ API v1.3.2-dev - UNCC Fodor Lab                   
                   
Usage:                   
biolockj-api <query> [options...]                   
                   
For some uses, redirecting stderr is recommended:                   
biolockj-api <query> [options...]  2> /dev/null                   
Options shown in [ ] are optional for a given query.                   
                   
Use biolockj-api without args to get help menu.                   
                   
Options:                   
                   
  --external-modules <dir>                   
        path to a directory containing additional modules                   
  --module <module_path>                   
        class path for a specific module                   
  --property <property>                   
        a specific property                   
  --value <value>                   
        a vlue to use for a specific property                   
  --config <file>                   
        file path for a configuration file giving one or more property values                   
  --verbose                   
        flag indicating that all messages should go to standard err, including some that are typically disabled.                   
                   
query:                   
                   
  listModules [ --external-modules <dir> ]                   
        Returns a list of classpaths to the classes that extend BioModule.                   
                   
  listApiModules [--external-modules <dir> ]                   
        Like listModules but limit list to modules that implement the ApiModule interface.                   
                   
  listProps [ --module <module_path> ]                   
        Returns a list of properties.                   
        If no args, it returns the list of properties used by the BioLockJ backbone.                   
        If a modules is given, then it returns a list of all properties used by                   
        that module.                   
                   
  listAllProps [ --external-modules <dir> ]                   
        Returns a list of all properties, include all backbone properties and all module properties.                   
        Optionally supply the path to a directory containing additional modules to include their properties.                   
                   
  propType --property <property> [ --module <module_path> [ --external-modules <dir> ] ]                   
        Returns the type expected for the property: String, list, integer, positive number, etc.                   
        If a module is supplied, then the modules propType method is used.                   
                   
  describeProp --property <property> [ --module <module_path> [ --external-modules <dir> ] ]                   
        Returns a description of the property.                   
        If a module is supplied, then the modules getDescription method is used.                   
                   
  propValue --property <property> [ --config <file> ] [ --module <module_path> ]                   
        Returns the value for that property given that config file (optional) or                    
        no config file (ie the default value)                   
                   
  isValidProp --property <property> --value <value> [ --module <module_path>  [--external-modules <dir>] ]                   
        T/F/NA. Returns true if the value (val) for the property (prop) is valid;                   
        false if prop is a property but val is not a valid value,                   
        and NA if prop is not a recognized property.                   
        IF a module is supplied, then additionally call the validateProp(key, value)                   
        for that module, or for EACH module if a comma-separated list is given.                   
                   
  propInfo                   
        Returns a json formatted list of the general properties (listProps)                   
        with the type, descrption and default for each property                   
                   
  moduleInfo [--external-modules <dir>]                   
        Returns a json formatted list of all modules and for each module that                    
        implements the ApiModule interface, it lists the props used by the module,                   
        and for each prop the type, descrption and default.                   
                   
  help  (or no args)                   
        Print help menu.                   
                   
                   
```                   
                   
`listModules` and `listApiModules` are nearly identical.  The methods that allow the API to interface with modules are in the ApiModule interface, not all BioModules implement that interface.  Once all of the build-in modules have those methods, then these two functions will be identical; the BioModule interface will absorb the ApiModule interface, and listApiModules will be depricated.                   
                   
`listAllProps` is the union of all possible output from `listProps`.                   
                   
`propInfo` returns information equivelent to calling `biolockj_api listProps` and creating a for-loop to call `biolockj_api propType $PROP`, `biolockj_api describeProp $PROP` and `biolockj_api propValue $PROP` for each PROP in the list.                   
                   
`moduleInfo` returns information equivelent to calling `biolockj_api listModules` and creating a for-loop to call `biolockj_api listProps $MODULE` and for each of its properties.                   
