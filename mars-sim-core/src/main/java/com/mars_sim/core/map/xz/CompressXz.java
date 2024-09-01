/**
 * Mars Simulation Project
 * CompressXz.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */

package com.mars_sim.core.map.xz;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.tukaani.xz.*;

import com.mars_sim.core.map.common.FileLocator;

public class CompressXz {

	// CompressXz
	public static void main(String[] args) throws Exception {
//	    String from = CompressXz.class.getClassLoader().getResource("/elevation/megt90n000eb.img").toExternalForm();
	    InputStream resourceStream = FileLocator.class.getResourceAsStream("/elevation/megt90n000eb.img");
	    String from = resourceStream.toString();
	    System.out.println("from: " + from);
	    String to = CompressXz.class.getClassLoader().getResource("megt90n000eb.img.xz").toExternalForm();
	    
	    try (FileOutputStream fileStream = new FileOutputStream(to);
	
//	    	XZInputStream xzStream = new XZInputStream(resourceStream, BasicArrayCache.getInstance())
	         XZOutputStream xzStream = new XZOutputStream(
	                 fileStream, new LZMA2Options(LZMA2Options.PRESET_MAX), BasicArrayCache.getInstance())) {

	        Files.copy(Paths.get(from), xzStream);
	    }
	}
}
