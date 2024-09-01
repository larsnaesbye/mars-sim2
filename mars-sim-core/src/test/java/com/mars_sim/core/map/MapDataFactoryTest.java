package com.mars_sim.core.map;

import com.mars_sim.core.map.megdr.MEGDRMapReader;
import com.mars_sim.core.tool.RandomUtil;

import junit.framework.TestCase;

public class MapDataFactoryTest extends TestCase {

	private static final String SEPARATOR = ",";
	
    public void testMEGDRReader() {
        // Use different space combinations
        MEGDRMapReader memoryReader = MapDataFactory.createReader(MapDataFactory.MEMORY_READER
                                    + SEPARATOR + MEGDRMapReader.DEFAULT_MEGDR_FILE);
        MEGDRMapReader directReader = MapDataFactory.createReader(MapDataFactory.DIRECT_READER
                                    + SEPARATOR + " " + MEGDRMapReader.DEFAULT_MEGDR_FILE);
        MEGDRMapReader arrayReader = MapDataFactory.createReader(MapDataFactory.ARRAY_READER
        						+ " " + SEPARATOR  + MEGDRMapReader.DEFAULT_MEGDR_FILE + " ");
        
        for (int i = 0; i < 1000; i++) {
            double phi = RandomUtil.getRandomDouble(Math.PI);
            double theta = RandomUtil.getRandomDouble(Math.PI * 2);
            short memoryElevation = memoryReader.getElevation(phi, theta);
            short directElevation = directReader.getElevation(phi, theta);
            short arrayElevation = arrayReader.getElevation(phi, theta);

            assertEquals("Array & Direct elevation", arrayElevation, directElevation);
            assertEquals("Array & Memory elevation", arrayElevation, memoryElevation);
        }
    }
}
