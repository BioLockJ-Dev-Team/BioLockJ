package biolockj.module.diversity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import biolockj.module.JavaModuleImpl;
import biolockj.util.OtuWrapper;

public class ShannonDiversity extends JavaModuleImpl
{

	@Override
	public void runModule() throws Exception
	{
		System.out.println("Hello world from Shannon Diversity!!!!");
		
		List<File> inputFiles = getInputFiles();
		
		for(File f : inputFiles)
		{
			System.out.println("Hello "+ f.getAbsolutePath());
			OtuWrapper wrapper = new OtuWrapper(f);

			File outFile = new File( getOutputDir() + File.separator+ 
								f.getName().replaceAll(".tsv", "") + "_Shannon.tsv");
			
			System.out.println("Hello out file " + outFile.getAbsolutePath());
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			
			writer.write("sample\tShannonDiversity\n");
			
			List<String> sampleNames = wrapper.getSampleNames();
			
			for(String name : sampleNames)
			{
				writer.write(name + "\t" + wrapper.getShannonEntropy(name) + "\n");
			}
			
			writer.flush(); writer.close();
			
		}
		
		
	}

}
