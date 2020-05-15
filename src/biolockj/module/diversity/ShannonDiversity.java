package biolockj.module.diversity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import biolockj.Log;
import biolockj.module.JavaModuleImpl;
import biolockj.util.OtuWrapper;

public class ShannonDiversity extends JavaModuleImpl
{

	@Override
	public void runModule() throws Exception
	{
		Log.debug(this.getClass(),"In ShannonDiversity");
		
		List<File> inputFiles = getInputFiles();
		
		for(File f : inputFiles)
		{
			Log.debug( this.getClass(), "Opening " +  f.getAbsolutePath());
			OtuWrapper wrapper = new OtuWrapper(f);

			String newName = f.getName().replaceAll(".tsv", "") ;
			
			newName = newName.replace("_taxaCount","");
			
			File outFile = new File( getOutputDir() + File.separator+ 
								newName+  "_Shannon.tsv")	;
			
			Log.debug(this.getClass(), "Trying to write to " +  outFile.getAbsolutePath());
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			
			writer.write("sample\tShannonDiversity\n");
			
			List<String> sampleNames = wrapper.getSampleNames();
			
			for(String name : sampleNames)
			{
				writer.write(name + "\t" + wrapper.getShannonEntropy(name) + "\n");
			}
			
			writer.flush(); writer.close();
			
			Log.debug(this.getClass(), "Finished ShannonDiversity module");
			
		}
	}

}
