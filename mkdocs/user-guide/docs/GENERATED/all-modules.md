# All Modules
*This is an auto-generated list of all modules with links to auto-generated module documentation.*

[AddMetadataToPathwayTables](biolockj.module.report.humann2/AddMetadataToPathwayTables.md)                   
[AddMetadataToTaxaTables](biolockj.module.report.taxa/AddMetadataToTaxaTables.md)                   
[AddPseudoCount](biolockj.module.report.taxa/AddPseudoCount.md) - *Add a pseudocount (+1) to each value in each taxa table.*                   
[AwkFastaConverter](biolockj.module.seq/AwkFastaConverter.md) - *Convert fastq files into fasta format.*                   
[BuildQiimeMapping](biolockj.module.implicit.qiime/BuildQiimeMapping.md)                   
[BuildTaxaTables](biolockj.module.report.taxa/BuildTaxaTables.md)                   
[CompileOtuCounts](biolockj.module.report.otu/CompileOtuCounts.md)                   
[Demultiplexer](biolockj.module.implicit/Demultiplexer.md) - *Demultiplex samples into separate files for each sample.*                   
[Email](biolockj.module.report/Email.md) - *Send an email containing the pipeline summary when the pipeline either completes or fails.*                   
[GenMod](biolockj.module.diy/GenMod.md) - *Allows user to add their own scripts into the BioLockJ pipeline.*                   
[GenomeAssembly](biolockj.module.assembly/GenomeAssembly.md)                   
[Gunzipper](biolockj.module.seq/Gunzipper.md) - *Decompress gzipped files.*                   
[HUMAnN2](biolockj.module.classifier.wgs/Humann2Classifier.md) - *Profile the presence/absence and abundance of microbial pathways in a community from metagenomic or metatranscriptomic sequencing data.*                   
[Humann2Parser](biolockj.module.implicit.parser.wgs/Humann2Parser.md) - *Build OTU tables from HumanN2 classifier module output.*                   
[ImportMetadata](biolockj.module.implicit/ImportMetadata.md)                   
[JsonReport](biolockj.module.report/JsonReport.md)                   
[KneadData](biolockj.module.seq/KneadData.md) - *Run the Biobakery [KneadData](https://bitbucket.org/biobakery/kneaddata/wiki/Home) program to remove contaminated DNA.*                   
[Kraken2Classifier](biolockj.module.classifier.wgs/Kraken2Classifier.md) - *Classify WGS samples with [KRAKEN 2](https://ccb.jhu.edu/software/kraken2/).*                   
[Kraken2Parser](biolockj.module.implicit.parser.wgs/Kraken2Parser.md) - *Build OTU tables from [KRAKEN](http://ccb.jhu.edu/software/kraken/) mpa-format reports.*                   
[KrakenClassifier](biolockj.module.classifier.wgs/KrakenClassifier.md) - *Classify WGS samples with KRAKEN.*                   
[KrakenParser](biolockj.module.implicit.parser.wgs/KrakenParser.md) - *Build OTU tables from [KRAKEN](http://ccb.jhu.edu/software/kraken/) mpa-format reports.*                   
[LogTransformTaxaTables](biolockj.module.report.taxa/LogTransformTaxaTables.md)                   
[MergeQiimeOtuTables](biolockj.module.implicit.qiime/MergeQiimeOtuTables.md)                   
[Metaphlan2Classifier](biolockj.module.classifier.wgs/Metaphlan2Classifier.md) - *Classify WGS samples with [MetaPhlAn2](http://bitbucket.org/biobakery/metaphlan2).*                   
[Metaphlan2Parser](biolockj.module.implicit.parser.wgs/Metaphlan2Parser.md)                   
[Multiplexer](biolockj.module.seq/Multiplexer.md) - *Multiplex samples into a single file, or two files (one with forward reads, one with reverse reads) if multiplexing paired reads.*                   
[NormalizeByReadsPerMillion](biolockj.module.report.taxa/NormalizeByReadsPerMillion.md)                   
[NormalizeTaxaTables](biolockj.module.report.taxa/NormalizeTaxaTables.md)                   
[PearMergeReads](biolockj.module.seq/PearMergeReads.md) - *Run pear, the Paired-End reAd mergeR*                   
[QiimeClassifier](biolockj.module.implicit.qiime/QiimeClassifier.md)                   
[QiimeClosedRefClassifier](biolockj.module.classifier.r16s/QiimeClosedRefClassifier.md) - *Pick OTUs using a closed reference database and construct an OTU table via the QIIME script pick_closed_reference_otus.py*                   
[QiimeDeNovoClassifier](biolockj.module.classifier.r16s/QiimeDeNovoClassifier.md) - *Run the QIIME pick_de_novo_otus.py script on all fasta sequence files*                   
[QiimeOpenRefClassifier](biolockj.module.classifier.r16s/QiimeOpenRefClassifier.md) - *Run the QIIME pick_open_reference_otus.py script on all fasta sequence files*                   
[QiimeParser](biolockj.module.implicit.parser.r16s/QiimeParser.md)                   
[R_CalculateStats](biolockj.module.report.r/R_CalculateStats.md) - *Generate a summary statistics table with [adjusted and unadjusted] [parameteric and non-parametirc] p-values and r<sup>2</sup> values for each reportable metadata field and each *report.taxonomyLevel* configured.*                   
[R_PlotEffectSize](biolockj.module.report.r/R_PlotEffectSize.md) - *Generate horizontal barplot representing effect size (Cohen's d, r<sup>2</sup>, and/or fold change) for each reportable metadata field and each *report.taxonomyLevel* configured.*                   
[R_PlotMds](biolockj.module.report.r/R_PlotMds.md) - *Generate sets of multidimensional scaling plots showing 2 axes at a time (up to the <*r_PlotMds.numAxis*>th axis) with color coding based on each categorical metadata field (default) or by each field given in *r_PlotMds.reportFields**                   
[R_PlotOtus](biolockj.module.report.r/R_PlotOtus.md) - *Generate OTU-metadata box-plots and scatter-plots for each reportable metadata field and each *report.taxonomyLevel* configured*                   
[R_PlotPvalHistograms](biolockj.module.report.r/R_PlotPvalHistograms.md) - *Generate p-value histograms for each reportable metadata field and each *report.taxonomyLevel* configured*                   
[RarefyOtuCounts](biolockj.module.report.otu/RarefyOtuCounts.md) - *Applies a mean iterative post-OTU classification rarefication algorithm so that each output sample will have approximately the same number of OTUs.*                   
[RarefySeqs](biolockj.module.seq/RarefySeqs.md) - *Randomly sub-sample sequences to reduce all samples to the configured maximum.*                   
[RdpClassifier](biolockj.module.classifier.r16s/RdpClassifier.md) - *Classify 16s samples with [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp).*                   
[RdpParser](biolockj.module.implicit.parser.r16s/RdpParser.md) - *Build OTU tables from [RDP](http://rdp.cme.msu.edu/classifier/classifier.jsp) reports.*                   
[RegisterNumReads](biolockj.module.implicit/RegisterNumReads.md)                   
[RemoveLowOtuCounts](biolockj.module.report.otu/RemoveLowOtuCounts.md)                   
[RemoveLowPathwayCounts](biolockj.module.report.humann2/RemoveLowPathwayCounts.md)                   
[RemoveScarceOtuCounts](biolockj.module.report.otu/RemoveScarceOtuCounts.md)                   
[RemoveScarcePathwayCounts](biolockj.module.report.humann2/RemoveScarcePathwayCounts.md)                   
[SeqFileValidator](biolockj.module.seq/SeqFileValidator.md) - *This BioModule validates fasta/fastq file formats are valid and enforces min/max read lengths.*                   
[SraDownload](biolockj.module.getData/SraDownload.md) - *SraDownload downloads and compresses short read archive (SRA) files to fastq.gz*                   
[SraMetaDB](biolockj.module.getData.sra/SraMetaDB.md) - *Makes sure that the SRAmetadb exists, downloads if it does not already exist.*                   
[SraMetaData](biolockj.module.getData.sra/SraMetaData.md) - *Extract metadata via pysradb from local copy of SRAmetadb.sqlite.*                   
[TrimPrimers](biolockj.module.seq/TrimPrimers.md) - *Remove primers from reads, option to discard reads unless primers are attached to both forward and reverse reads.*                   
