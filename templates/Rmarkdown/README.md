# Rmarkdown Demo

R markdown is an excellent tool for weaving together code and human-readable text.  The Rmarkdown module in BioLockJ facilitates using this tool in a BioLockJ pipeline.
See the documentation in the user guide, which is under BioLockJ/docs or online at https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/Rmarkdown/

### simpleMarkdown.properties

The pipeline outlined in 'simpleMarkdown.properties' shows the basics of using this module.  

To run this pipeline yourself, install biolockj and run:
```
biolockj simpleMarkdown.properties
```

To run this pipeline using docker, run:
```
biolockj --docker simpleMarkdown.properties
```

### simpleMarkdownPdf.properties

The pipeline outlined 'simpleMarkdownPdf.properties' is essentially identical to its non-Pdf counterpart.  The only difference is that the markdown is configured to produce a pdf rather than an html document.  Docker is recommended as rendering the pdf requires additional software.  Rendering the html requires software that most users have if they have installed RStudio.

###

See 'biolockj --help' for additional options.  In either case, this command will create a new instance of the pipeline in your '$BLJ_PROJ' folder.
The 'example_output' folder contains an example of the report document created with each pipeline.

The modules demonstrated here include:
[Rmarkdown](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/Rmarkdown/)
[GenMod](https://biolockj-dev-team.github.io/BioLockJ/GENERATED/biolockj.module.diy/GenMod/)
