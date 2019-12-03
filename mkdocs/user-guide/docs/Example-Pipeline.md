In our example analysis, we investigate the differences between the microbiome of 20 rural and 20 recently urbanized subjects from the Chinese province of Hunan.  For more information on this dataset, please review the analysis Fodor Lab published in the Sep 2017 issue of the journal Microbiome: 
https://microbiomejournal.biomedcentral.com/articles/10.1186/s40168-017-0338-7

#### Step 1: Prepare BioLockJ Config File
The BioLockJ project Config chinaKrakenFullDB.properties lists 5 BioModules to run (lines 3-7) + 13 properties:  

      #BioModule biolockj.module.implicit.RegisterNumReads
      #BioModule biolockj.module.classifier.wgs.KrakenClassifier
      #BioModule biolockj.module.report.taxa.NormalizeTaxaTables
      #BioModule biolockj.module.report.r.R_PlotPvalHistograms
      #BioModule biolockj.module.report.r.R_PlotOtus

In addition to the 5 listed BioModules, 4  additional [implicit BioModules](../wiki/module.implicit) will also run: 

| Mod# | Module | Description |
| :-- | :-- | :-- |
| 1 | [ImportMetadata](../wiki/module.implicit#ImportMetadata) | Always run 1st (for all pipelines) |
| 2 | [KrakenParser](../wiki/module.implicit.parser.wgs#KrakenParser) | Always run after [KrakenClassifier](../wiki/module.classifier.wgs#KrakenClassifier) |
| 3 | [AddMetadataToOtuTables](../wiki/module.report.taxa#AddMetadataToOtuTables) | Always run just before the 1st R module |  
| 4 | [CalculateStats](../wiki/module.report.r#R_CalculateStats) | Always run as the 1st R module. |  

Key properties:<br>

| Line# | Property | Description |
| :-- | :-- | :-- |
| 08 | *cluster.jobHeader* | Each script will run on 1 node, 16 cores, and 128GB RAM for up to 30 minutes |
| 10 | *pipeline.defaultProps* | Default config file defines most properties – in this case copperhead.properties |
| 12 | *input.dirPaths* | Directory path containing 40 gzipped whole genome sequencing (WGS) fastq files |
| 18 | *metadata.filePath* | Metadata file path: [chinaMetadata.tsv]( https://github.com/msioda/BioLockJ/blob/master/resources/metadata/chinaMetadata.tsv?raw=true) |

BioLockJ must associate sequence files in *input.dirPaths* with the correct metadata row.  This is done by matching sequence file names to the 1st column in the metadata file.  If the Sample ID is not found in your file names, the file names must be updated.  Use the following properties to ignore a file prefix or suffix when matching the sample IDs.

  - *input.suffixFw*
  - *input.suffixRv*
  - *input.trimPrefix*
  - *input.trimSuffix*

Sample IDs from 1st column of the metadata file: 081A, 082A, 083A...etc.  
Sequence file names: 081A_R1.fq.gz, 082A_R1.fq.gz, 083A_R1.fq.gz...etc.  

The default Config file, copperhead.properties, has its own default Config file [standard.properties](https://github.com/msioda/BioLockJ/blob/master/resources/config/default/standard.properties?raw=true) which defines the property *input.suffixFw=_R1*.  As a result, all characters starting with (and including) “_R1” are ignored when matching the file name to the metadata sample ID. 

#### Step 2: Run BioLockJ Pipeline
      > biolockj ~/chinaKrakenFullDB.properties

  - Look in the BioLockJ pipeline output directory defined by $BLJ_PROJ for a new pipeline directory named after the property file + today’s date:  ~/projects/chinaKrakenFullDB_2018Apr09
  - The 5 configured modules have run in order, with the addition of 2 implicit modules (1st and last) which are added to all pipelines automatically.
  - The biolockjComplete file indicates the pipeline ran successfully.

#### Step 3: Review Pipeline Summary
  - Run the [blj_summary](https://github.com/msioda/BioLockJ/blob/master/script/blj_summary?raw=true)  command to review the pipeline execution summary.

        > blj_summary

  - [Pipeline Summary](https://github.com/msioda/BioLockJ/blob/master/docs/example/sampleSummary.txt?raw=true) 


#### Step 4: Download R Reports
  - Run the [blj_download](https://github.com/msioda/BioLockJ/blob/master/script/blj_download?raw=true) command to get the command needed to download the analysis. 

        > blj_download
        > rsync

#### Step 5: Analyze R Reports
  - Open *downloadDir* on your local filesystem to review the analysis.  This directory contains:

| Output | Description |
| :-- | :-- | 
| /temp  | Directory where R log files are saved if R script runs locally. |
| /tables | Directory containing the OTU tables.  |
| /local | Directory where R script output is saved if R script runs locally and *r.debug=Y*. |
| *.RData  | The saved R sessions for R modules run if *r.saveRData=Y*. |
| chinaKrakenFullDB.log | The pipeline Java log file. |
| MAIN_*.R | Each R script for each module that generated reports has been updated to run on your local filesystem.   |
| *.tsv files | Spreadsheets containing p-value and R^2 statistics for each OTU in the taxonomy level. |
| *.pdf files | P-value histograms, and bar-charts or scatterplots for each OTU in the taxonomy level. |

  - Each R module generates a report for each *report.taxonomyLevel* configured:


#### Open chinaKrakenFullDB_Log10_genus.pdf

  - The report begins with the unadjusted P-Value Distributions:
  - Since *r.numHistogramBreaks=20* so the 1st bar represents the p-values < 0.05.  The ruralUrban attribute appears significant, as indicated by the high number p-values < 0.05.
  - For each OTU, a bar-chart or scatterplot is output with adjusted parametric and non-parametric p-values formatted using in the plot header.  
  - The p-value format is defined by *r.pValFormat*.
  - The p-adjust method is defined by *rStats.pAdjustMethod*.
  - P-values that meet the *r.pvalCutoff* threshold are highlighted with *r.colorHighlight*.
