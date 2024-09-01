/*
 * Mars Simulation Project
 * FastRGB.java
 * @date 2024-08-10
 * @author Manny Kung
 */

package com.mars_sim.core.map;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class FastRGB {

    FastRGB() {
    }

    public static int getRGB(BufferedImage image, int x, int y) {
  
    	byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    	int width = image.getWidth();
    	int height = image.getHeight();
    	boolean hasAlphaChannel = image.getAlphaRaster() != null;
        int pixelLength = 3;
        if (hasAlphaChannel) {
            pixelLength = 4;
        }
        
        int pos = (y * pixelLength * width) + (x * pixelLength);

        int argb = -16777216; // 255 alpha
        if (hasAlphaChannel) {
            argb = (((int) pixels[pos++] & 0xff) << 24); // alpha
        }

        argb += ((int) pixels[pos++] & 0xff); // blue
        argb += (((int) pixels[pos++] & 0xff) << 8); // green
        argb += (((int) pixels[pos++] & 0xff) << 16); // red
        return argb;
    }
}
