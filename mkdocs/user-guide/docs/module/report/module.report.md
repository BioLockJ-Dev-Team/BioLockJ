# Report Package

Modules in the biolockj.module.report package process [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) output, merge the OTU tables with the metadata, and can generate various reports and notifications.

This package contains the following sub-packages:

  * module.report.otu contains modules designed to produce or process otu tables.
  * module.report.taxa contains modules designed to produce or process taxa tables.
  * module.report.r contains modules that use R to generate statistics and/or visualizations.
  * module.report.humann2 contains modules designed to produce or process pathway tables.

---

#### Email
`#BioModule biolockj.module.report.Email`

**Description:**  Notify user pipeline is complete by emailing out the pipeline summary.

**Options:**

   - *mail.encryptedPassword* 
   - *mail.from*
   - *mail.smtp.auth*
   - *mail.smtp.host*
   - *mail.smtp.port*
   - *mail.smtp.starttls.enable*
   - *mail.to*

---

#### JsonReport
`#BioModule biolockj.module.report.JsonReport`

**Description:**  This module builds a JSON file from the [ParserModule](https://msioda.github.io/BioLockJ/docs/biolockj/module/implicit/parser/ParserModule.html) output.

**Options:** 

   - *report.logBase*
   - *report.taxonomyLevels*

---