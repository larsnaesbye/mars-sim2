/*
 * Mars Simulation Project
 * IntegerMapData.java
 * @date 2023-06-22
 * @author Scott Davis
 */
 package com.mars_sim.core.map;

import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static com.mars_sim.core.map.OpenCL.getGlobalSize;
import static com.mars_sim.core.map.OpenCL.getKernel;
import static com.mars_sim.core.map.OpenCL.getLocalSize;
import static com.mars_sim.core.map.OpenCL.getProgram;
import static com.mars_sim.core.map.OpenCL.getQueue;

import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;
import com.mars_sim.core.map.common.FileLocator;

 /**
  * A map that uses integer data stored in files to represent colors.
  */
 public class IntegerMapData implements MapData {

	// Static members.
 	private static Logger logger = Logger.getLogger(IntegerMapData.class.getName());

	public static final double HALF_MAP_ANGLE = 0.48587;
	
	public static final double QUARTER_HALF_MAP_ANGLE = HALF_MAP_ANGLE / 4;
	
 	private static final double TWO_PI = Math.PI * 2;
 	
 	private static final double HALF_PI = Math.PI / 2D;
 	
	private static boolean HARDWARE_ACCELERATION = true;
	// The max rho
 	public static double MAX_RHO;
 	// The min rho
 	public static double MIN_RHO;
	// The default rho at the start of the sim
 	public static double RHO_DEFAULT;
 	// The default magnification at the start of the sim
  	public static double MAG_DEFAULT;

 	// Data members.
  	/* # of pixels in the width of the map image. */
	private int pixelWidth;
	/* # of pixels in the height of the map image. */
	private int pixelHeight;
	/* height pixels divided by pi (equals to pixelHeight / Math.PI). */
	private double rho;
	/* The base map pixels double array. */
 	private int[][] baseMapPixels = new int[0][0];
 	
 	/* The meta data of the map. */
	private MapMetaData meta;
 	/* The OpenCL program instance. */
	private CLProgram program;
 	/* The OpenCL kernel instance. */
	private CLKernel kernel;
 	
 	/**
 	 * Constructor.
 	 * 
	 * @param name   the name/description of the data
 	 * @param filename   the map data file name.
 	 * @throws IOException Problem loading map data
 	 */
 	IntegerMapData(MapMetaData mapMetaData) throws IOException {
		this.meta = mapMetaData;
		
		// Load data files
		String metaFile = mapMetaData.getFile();
		
		try {
			if (metaFile == null || metaFile.equals("")) {
				logger.log(Level.SEVERE, "Map file not found.");
				return;
			}
			else {
				loadMapData(metaFile);
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Unable to load map. " + e.getMessage());
			return;
		}
		
		rho = pixelHeight / Math.PI;
		RHO_DEFAULT = rho;
		MAX_RHO = RHO_DEFAULT * 6;
		MIN_RHO = RHO_DEFAULT / 6;
		MAG_DEFAULT = rho / RHO_DEFAULT;
		
		logger.config("new IntegerMapData - rho : " + Math.round(rho *10.0)/10.0 + ". MAG_DEFAULT: " + Math.round(MAG_DEFAULT*10.0)/10.0 + ".");
		
		// Exclude mac from use openCL
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			HARDWARE_ACCELERATION = false;
		}
		else {
			setKernel();
		}
 	}
 	
 	/**
 	 * Sets up the JOCL kernel program.
 	 */
	private void setKernel() {
 
		try {
			program = getProgram("MapDataFast.cl");
			kernel = getKernel(program, "getMapImage")
					.setArg(11, (float) TWO_PI)
					.setArg(12, (float) getRho());
		} catch(Exception e) {
			HARDWARE_ACCELERATION = false;
			logger.log(Level.SEVERE, "Disabling hardware accel due to exception caused while compiling: " + e.getMessage());
		}
 	}

	/**
	 * Gets the meta data of the map.
	 * 
	 * @return
	 */
	@Override
	public MapMetaData getMetaData() {
		return meta;
	}

    /**
     * Gets the magnification of the Mars surface map.
     * 
     * @return
     */
    public double getMagnification() {
    	return rho / RHO_DEFAULT;
    }
    
	/**
	 * Gets the scale of the Mars surface map.
	 * 
	 * @return
	 */
	@Override
	public double getRho() {
		return rho;
	}
	
	/**
	 * Sets the rho of the Mars surface map.
	 * 
	 * @param value
	 */
	public void setRho(double value) {
		double newRho = value;
		if (newRho > MAX_RHO) {
			newRho = MAX_RHO;
		}
		else if (newRho < MIN_RHO) {
			newRho = MIN_RHO;
		}
		
		if (rho != newRho) {
			rho = newRho;
//	 		logger.info("rho: " + rho + "  newRho: " + newRho);
		}
	}	

	/**
     * Gets the half angle of the Mars surface map.
     * 
     * @return
     */
    public double getHalfAngle() {
    	double ha = Math.sqrt(HALF_MAP_ANGLE / getMagnification() / (0.25 + meta.getResolution()));
    	return Math.min(Math.PI, ha);
    }
	
	/**
     * Gets the number of pixels width.
     * 
     * @return
     */
	@Override
    public int getWidth() {
		return pixelWidth;
	}

	/**
     * Gets the number of pixels height.
     * 
     * @return
     */
	@Override
    public int getHeight() {
		return pixelHeight;
	}

 	/**
 	 * Loads the whole map data set into an 2-D integer array.
 	 * 
 	 * @param imageName
 	 * @return
 	 * @throws IOException
 	 */
 	private void loadMapData(String imageName) throws IOException {

 		BufferedImage cylindricalMapImage = null;
 		
		try {
			cylindricalMapImage = ImageIO.read(FileLocator.locateFile(MapDataFactory.MAPS_FOLDER + imageName));
			
			// Use getRaster() is fastest
		    // See https://stackoverflow.com/questions/10954389/which-amongst-pixelgrabber-vs-getrgb-is-faster/12062932#12062932
			// See https://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
			
			final byte[] pixels = ((DataBufferByte) cylindricalMapImage.getRaster().getDataBuffer()).getData();
//	 		May try: int[] srcPixels = ((DataBufferInt)cylindricalMapImage.getRaster().getDataBuffer()).getData()

	 		pixelWidth = cylindricalMapImage.getWidth();
	 		pixelHeight = cylindricalMapImage.getHeight();
//	 		logger.config("loadMapData - " +  imageName + " : " + pixelWidth + " x " + pixelHeight + ".");
	 		
	 		final boolean hasAlphaChannel = cylindricalMapImage.getAlphaRaster() != null;
		
	 		baseMapPixels = new int[pixelHeight][pixelWidth];
	 		
	 		if (!meta.isColourful()) {	
	 			
	 			// Note: May use the shade map to get height values
	 			
	 			boolean done = false;
	 			boolean alreadyWentToNextByte = false;
 			    int byteIndex = 0;
 			    int row = 0;
 			    int col = 0;
 			    int numBits = 0;
 			    byte currentByte = pixels[byteIndex];
 			    while (!done) {
 			        alreadyWentToNextByte = false;
	        
 			        // See https://stackoverflow.com/questions/32804784/weird-rgb-value-from-java-bufferedimage-getrgb/32824569#32824569
 			    	   
 			        // Using 0x10101 mask will turn it into monochromic 0 or 1
 			        
			        // Use 0xff mask because byte is a signed type in Java, and we want an unsigned value
 			        int grayValue = (currentByte & 0xff);        
// 			        int monoValue = (currentByte & 0x80) >> 7;
// 			        System.out.print(grayValue + " ");
 			        
	        		baseMapPixels[row][col] = grayValue;
	        
 			        currentByte = (byte) (((int) currentByte) << 1);
 			        numBits++;

 			        if ((row == pixelHeight - 1) && (col == pixelWidth - 1)) {
 			            done = true;
 			        }
 			        else {
 			            col++;

 			            if (numBits == 8) {
 			                currentByte = pixels[++byteIndex];
 			                numBits = 0;
 			                alreadyWentToNextByte = true;
 			            }

 			            if (col == pixelWidth) {
 			                row++;
 			                col = 0;

 			                if (!alreadyWentToNextByte) {
 			                    currentByte = pixels[++byteIndex];
 			                    numBits = 0;
 			                }
 			            }
 			        }
 			    } 			
	 			// https://stackoverflow.com/questions/30951726/reading-a-grayscale-image-in-java		
//	 			Raster raster = cylindricalMapImage.getData();
//	 			int h = raster.getHeight();
//	 			int w = raster.getWidth();
//	 		    for (int i = 0; i < w; i++) {
//	 		        for (int j = 0; j < h; j++) {
//	 		        	baseMapPixels[j][i] = raster.getSample(i, j, 0);
//	 		        }
//	 		    }
	 		}
	 		
	 		else if (hasAlphaChannel) {
	 			// Note: viking geologic is the only one that has alpha channel
	 			
	 			final int pixelLength = 4;
	 			
	 			// Note: int pos = (y * pixelLength * width) + (x * pixelLength);
	 			
	 			for (int pos = 0, row = 0, col = 0; pos + 3 < pixels.length; pos += pixelLength) {
	 				int argb = 0;
	 				
	 				// See https://stackoverflow.com/questions/11380062/what-does-value-0xff-do-in-java
	 				// When applying '& 0xff', it would end up with the value ff ff ff fe instead of 00 00 00 fe. 
	 				// A further subtlety is that '&' is defined to operate only on int values. As a result, 
	 				//
	 				// 1. value is promoted to an int (ff ff ff fe).
	 				// 2. 0xff is an int literal (00 00 00 ff).
	 				// 3. The '&' is applied to yield the desired value for result.

	 				argb += (((int) pixels[pos] & 0xff) << 24); // alpha
	 				argb += ((int) pixels[pos + 1] & 0xff); // blue
	 				argb += (((int) pixels[pos + 2] & 0xff) << 8); // green
	 				argb += (((int) pixels[pos + 3] & 0xff) << 16); // red
	 				
//	 				The Red and Blue channel comments are flipped. 
//	 				Red should be +1 and blue should be +3 (or +0 and +2 respectively in the No Alpha code).
	 				
//	 				You could also make a final int pixel_offset = hasAlpha?1:0; and 
//	 				do ((int) pixels[pixel + pixel_offset + 1] & 0xff); // green; 
//	 				and merge the two loops into one. – Tomáš Zato Mar 23 '15 at 23:02
	 						
	 				baseMapPixels[row][col] = argb;
	 				col++;
	 				if (col == pixelWidth) {
	 					col = 0;
	 					row++;
	 				}
	 			}
	 		}
	 		
	 		else {
	 			
	 		    final int pixelLength = 3;
	 			for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
	 				int argb = 0;
	 				
	 				argb += -16777216; // 255 alpha
	 				argb += ((int) pixels[pixel] & 0xff); // blue
	 				argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
	 				argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
	 				
	 				baseMapPixels[row][col] = argb;
	 				col++;
	 				if (col == pixelWidth) {
	 					col = 0;
	 					row++;
	 				}
	 			}
	 		}
	 		
		} catch (IOException e) {
			logger.severe("Can't read image file '" + imageName + "'.");
		}
 	}
 	
 	/**
 	 * Gets the map image based on the center phi and theta coordinates given.
 	 * 
 	 * @param centerPhi Center phi value on the image
 	 * @param centerTheta Center theta value on the image
	 * @param mapBoxWidth The Width of the requested image
	 * @param mapBoxHeight The Height of the requested image
	 * @param newRho The map rho
 	 */
 	@Override
 	public Image createMapImage(double centerPhi, double centerTheta, int mapBoxWidth, int mapBoxHeight, double newRho) {
	
		 boolean invalid = Double.isNaN(centerPhi) || Double.isNaN(centerTheta);
		 if (invalid) {
			 logger.log(Level.SEVERE, "centerPhi and/or centerTheta is invalid.");
			 return null;
		 }
	 
// 		if (mapImage != null 
// 				&& (centerPhiCache == centerPhi && centerThetaCache == centerTheta && newRho == rho)) {
// 			// No need to recreate the mapImage when the mouse has not moved
// 			return mapImage;
// 		}
 		
		// Set the new phi		
// 		centerPhiCache = centerPhi;
		// Set the new theta 		
// 		centerThetaCache = centerTheta;
		// Set the new map rho
		setRho(newRho);
 		
//		logger.log(Level.INFO, "centerPhiCache: " + centerPhiCache + "  centerThetaCache: " + centerThetaCache
//				+ "  scale: " + newRho);
		
 		// Create a new buffered image to draw the map on.
 		BufferedImage bImage = null;
 				
 		if (meta.isColourful()) {
 			 bImage = new BufferedImage(mapBoxWidth, mapBoxHeight, 
				BufferedImage. TYPE_INT_RGB); // TYPE_4BYTE_ABGR
 		}
 		else {
 			bImage = new BufferedImage(mapBoxWidth, mapBoxHeight, 
 				BufferedImage.TYPE_BYTE_GRAY); // TYPE_USHORT_GRAY
 		} 

// 		Graphics2D g2d = bImage.createGraphics();
// 		g2d.setColor(Color.BLACK);
// 		g2d.fillRect(0, 0, bImage.getWidth(), bImage.getHeight());
 		
 		// May experiment with BufferedImage.getSubimage(int x, int y, int w, int h);

// 		logger.config("transparency: " + result.getTransparency());
 		
 		// Create an array of int RGB color values to create the map image from.
 		int[] mapArray = new int[mapBoxWidth * mapBoxHeight];
 
		if (HARDWARE_ACCELERATION) {
			try {
				gpu(centerPhi, centerTheta, mapBoxWidth, mapBoxHeight, mapArray);
			} catch(Exception e) {
				HARDWARE_ACCELERATION = false;
				logger.log(Level.SEVERE, "Disabling GPU OpenCL accel. Exception caused by " + e.getMessage());
			}
		}
		else {
			cpu0(centerPhi, centerTheta, mapBoxWidth, mapBoxHeight, mapArray);
		}


	 	// Create new map image.
	 	setRGB(bImage, 0, 0, mapBoxWidth, mapBoxHeight, mapArray, 0, mapBoxHeight);
 		
 		// If alpha value is 255, it is fully opaque.
 		//  A value of 1 would mean it is (almost) fully transparent.
 		// May try setAlpha((byte)127, result);
	
 		return bImage;
 	}

    public void setRGB(BufferedImage bImage, int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
		int yoff  = offset;
		int off;
		Object pixel = null;
			
		for (int y = startY; y < startY + h; y++, yoff += scansize) {
			off = yoff;
			for (int x = startX; x < startX + w; x++) {
			    pixel = bImage.getColorModel().getDataElements(rgbArray[off++], pixel);
			    bImage.getRaster().setDataElements(x, y, pixel);
			}
		}
	}
    
 	private int toRGB(int grayValue) {
// 	    int part = Math.round(value * 255);
 	    return grayValue * 0x10101;
 	}
 	
 	
 	public void setAlpha(byte alpha, BufferedImage image) {       
 		// alpha is in 0-255 range
 		alpha %= 0xff; 
 	    for (int i = 0; i < image.getWidth(); i++) {          
 	        for (int j = 0; j < image.getHeight(); j++) {
 	        	int color = image.getRGB(i, j);

 	        	// According to Java API, the alpha value is at 24-31 bit.
 	            int mc = (alpha << 24) | 0x00ffffff; // shift blue to alpha
 	            int newcolor = color & mc;
 	            image.setRGB(i, j, newcolor);            
 	        }
 	    }
 	}
 	
	/**
 	 * Constructs a map array for display with CPU.
 	 * 
 	 * @param centerPhi
 	 * @param centerTheta
 	 * @param mapBoxWidth
 	 * @param mapBoxHeight
 	 * @param mapArray
 	 * @param scale
 	 */
	 private void cpu0(double centerPhi, double centerTheta, int mapBoxWidth, int mapBoxHeight, int[] mapArray) {
		 int halfWidth = mapBoxWidth / 2;
		 int halfHeight = mapBoxHeight / 2;

		 for(int y = 0; y < mapBoxHeight; y++) {
			 for(int x = 0; x < mapBoxWidth; x++) {
				 int index = x + (y * mapBoxWidth);

				 Point2D loc = convertRectToSpherical(x - halfWidth, y - halfHeight, centerPhi, centerTheta, getRho());
				 mapArray[index] = getRGBColorInt(loc.getX(), loc.getY());
			 }
		 }
	 }
	 
 	/**
 	 * Constructs a map array for display with CPU.
 	 * 
 	 * @param centerPhi
 	 * @param centerTheta
 	 * @param mapBoxWidth
 	 * @param mapBoxHeight
 	 * @param mapArray
 	 * @param scale
 	 */
	 private void cpu1(double centerPhi, double centerTheta, int mapBoxWidth, int mapBoxHeight, int[] mapArray) {
		 int halfWidth = mapBoxWidth / 2;
//		 int halfHeight = mapBoxHeight / 2;

		 // The map data is PI offset from the center theta.
		 double correctedTheta = centerTheta - Math.PI;
		 while (correctedTheta < 0D)
			 correctedTheta += TWO_PI;
		 while (correctedTheta > TWO_PI)
			 correctedTheta -= TWO_PI;
		
		 // Determine phi iteration angle.
		 double phiIterationPadding = 1.26D; // Derived from testing.
		 double phiIterationAngle = Math.PI / (mapBoxHeight * phiIterationPadding);

		 // Determine phi range.
		 double phiPadding = 1.46D; // Derived from testing.
		 double phiRange = Math.PI * phiPadding * pixelHeight / mapBoxHeight;

		 // Determine starting and ending phi values.
		 double startPhi = centerPhi - (phiRange / 2D);
		 if (startPhi < 0D)
			 startPhi = 0D;
		 double endPhi = centerPhi + (phiRange / 2D);
		 if (endPhi > Math.PI)
			 endPhi = Math.PI;

		 double ratio = TWO_PI * pixelWidth / mapBoxWidth;
		 // Note : Polar cap phi values must display 2 PI theta range. 
		 // (derived from testing)
		 double polarCapRange = Math.PI / 6.54D; 
		 // Determine theta iteration angle.
		 double thetaIterationPadding = 1.46D; // Derived from testing.
		 // Theta padding, derived from testing.
		 double minThetaPadding = 1.02D; 
		 // Determine theta range.
		 double minThetaDisplay = ratio * minThetaPadding;
			
		 for (double x = startPhi; x <= endPhi; x += phiIterationAngle) {
			 
			 double thetaIterationAngle = TWO_PI / (((double) mapBoxWidth * Math.sin(x) * thetaIterationPadding) + 1D);

			 double thetaRange = ((1D - Math.sin(x)) * TWO_PI) + minThetaDisplay;
			
			 if ((x < polarCapRange) || (x > (Math.PI - polarCapRange)))
				thetaRange = TWO_PI;
			 if (thetaRange > TWO_PI)
				thetaRange = TWO_PI;

			 // Determine the theta starting and ending values.
			 double startTheta = centerTheta - (thetaRange / 2D);
			 double endTheta = centerTheta + (thetaRange / 2D);
			
			 for (double y = startTheta; y <= endTheta; y += thetaIterationAngle) {
			 
				 // Correct y value to make sure it is within bounds. (0 to 2PI)
				 double yCorrected = y;
				 while (yCorrected < 0)
					 yCorrected += TWO_PI;
				 while (yCorrected > TWO_PI)
					 yCorrected -= TWO_PI;
				 
				 Point loc = findRectPosition(centerPhi, centerTheta, x, yCorrected, getRho(), halfWidth, halfWidth);
				  
				 // Determine the display x and y coordinates for the pixel in the image.
				 int xx = pixelWidth - (int)loc.getX();
				 int yy = pixelHeight - (int)loc.getY();
				
				 // Check that the x and y coordinates are within the display area.
				 boolean leftBounds = xx >= 0;
				 boolean rightBounds = xx < pixelWidth;
				 boolean topBounds = yy >= 0;
				 boolean bottomBounds = yy < pixelHeight;
				 
				 if (leftBounds && rightBounds && topBounds && bottomBounds) {
					// Determine array index for the display location.
					int index1 = (pixelWidth - xx) + ((pixelHeight - yy) * pixelWidth);			
					// Put color in array at index.
					if ((index1 >= 0) && (index1 < mapArray.length))
						mapArray[index1] = getRGBColorInt(x, yCorrected);
				 }
			 }
		 }
	 }
		 
	/**
	 * Converts spherical coordinates to rectangular coordinates. Returns integer x
	 * and y display coordinates for spherical location.
	 *
	 * @param newPhi   the new phi coordinate
	 * @param newTheta the new theta coordinate
	 * @param rho      diameter of planet (in km)
	 * @param half_map half the map's width (in pixels)
	 * @param low_edge lower edge of map (in pixels)
	 * @return pixel offset value for map
	 */
	public Point findRectPosition(double oldPhi, double oldTheta, double newPhi, double newTheta, double rho,
			int half_map, int low_edge) {
	
		final double col = newTheta + (- HALF_PI - oldTheta);
		final double xx = rho * Math.sin(newPhi);
		int x = ((int) Math.round(xx * Math.cos(col)) + half_map) - low_edge;
		int y = ((int) Math.round(((xx * (0D - Math.cos(oldPhi))) * Math.sin(col))
				+ (rho * Math.cos(newPhi) * (0D - Math.sin(oldPhi)))) + half_map) - low_edge;
		return new Point(x, y);
	}
		
	 /**
	  * Constructs a map array for display with GPU via JOCL.
	  * 
	  * @param centerPhi
	  * @param centerTheta
	  * @param mapBoxWidth
	  * @param mapBoxHeight
	  * @param mapArray
	  * @param scale
	  */
	 private synchronized void gpu(double centerPhi, double centerTheta, int mapBoxWidth, int mapBoxHeight, int[] mapArray) {
		 
		 // Set the rho this way to avoid global map artifact. Reason unknown.
		 kernel.setArg(12, (float) getRho());
		 
		 int size = mapArray.length;
		 int globalSize = getGlobalSize(size);

		 CLBuffer<IntBuffer> rowBuffer = OpenCL.getContext().createIntBuffer(size, WRITE_ONLY);
		 CLBuffer<IntBuffer> colBuffer = OpenCL.getContext().createIntBuffer(size, WRITE_ONLY);

		 kernel.rewind();
		 kernel.putArg((float)centerPhi)
				 .putArg((float)centerTheta)
				 .putArg(mapBoxWidth)
				 .putArg(mapBoxHeight)
				 .putArg(pixelWidth)
				 .putArg(pixelHeight)
				 .putArg(mapBoxWidth/2)
				 .putArg(mapBoxHeight/2)
				 .putArg(size)
				 .putArgs(colBuffer, rowBuffer)
//				 .putArg((float) getRho())
				 ;

		 getQueue().put1DRangeKernel(kernel, 0, globalSize, getLocalSize())
				 .putReadBuffer(rowBuffer, false)
				 .putReadBuffer(colBuffer, true);

		 int[] rows = new int[size];
		 rowBuffer.getBuffer().get(rows);
		 int[] cols = new int[size];
		 colBuffer.getBuffer().get(cols);

		 for(int i = 0; i < size; i++) {
			 mapArray[i] = baseMapPixels[rows[i]][cols[i]];
		 }

		 rowBuffer.release();
		 colBuffer.release();
	 }



	 /**
      * Converts linear rectangular XY position change to spherical coordinates with
      * rho value for map.
      *
      * @param x              change in x value (# of pixels or km)
      * @param y              change in y value (# of pixels or km)
      * @param phi			  center phi value (radians)
      * @param theta		  center theta value (radians)
      * @param rho            radius (in km) or map box height divided by pi (# of pixels)
      * @return a point2d of phi and theta
      */
	 public static Point2D convertRectToSpherical(double x, double y, double phi, double theta, double rho) {
		 double sinPhi = Math.sin(phi);
		 double sinTheta = Math.sin(theta);
		 double cosPhi = Math.cos(phi);
		 double cosTheta = Math.cos(theta);

		 double z = Math.sqrt((rho * rho) - (x * x) - (y * y));

		 double x2 = x;
		 double y2 = (y * cosPhi) + (z * sinPhi);
		 double z2 = (z * cosPhi) - (y * sinPhi);

		 double x3 = (x2 * cosTheta) + (y2 * sinTheta);
		 double y3 = (y2 * cosTheta) - (x2 * sinTheta);
		 double z3 = z2;

		 double phiNew = Math.acos(z3 / rho);
		 double thetaNew = Math.asin(x3 / (rho * Math.sin(phiNew)));

		 if (x3 >= 0) {
			 if (y3 < 0)
				 thetaNew = Math.PI - thetaNew;
		 } else {
			 if (y3 < 0)
				 thetaNew = Math.PI - thetaNew;
			 else
				 thetaNew = TWO_PI + thetaNew;
		 }

		 return new Point2D.Double(phiNew, thetaNew);
	 }
	 
	 
 	/**
 	 * Gets the RGB map color as an integer at a given location.
 	 * 
 	 * @param phi   the phi location.
 	 * @param theta the theta location.
 	 * @return the RGB map color as an integer.
 	 */
	@Override
 	public int getRGBColorInt(double phi, double theta) {
 		// Make sure phi is between 0 and PI.
 		while (phi > Math.PI)
 			phi -= Math.PI;
 		while (phi < 0)
 			phi += Math.PI;

 		// Adjust theta with PI for the map offset.
 		// Note: the center of the map is when theta = 0
 		// Make sure theta is between 0 and 2 PI.
 		while (theta > TWO_PI)
 			theta -= TWO_PI;
 		while (theta < 0)
 			theta += TWO_PI;

 		int row = (int) Math.round(phi * ((double) baseMapPixels.length / Math.PI));
 		if (row > baseMapPixels.length - 1)
 	 		row--;
 			
 		int column = (int) Math.round(theta * ((double) baseMapPixels[0].length / TWO_PI));
 		if (column > baseMapPixels[0].length - 1)
 			column--;
 		
// 		int pixel = baseMapPixels[row][column];
// 		int pixelWithAlpha = (pixel >> 24) & 0xFF; 
// 		return pixelWithAlpha;
 		
 		return baseMapPixels[row][column];
 	}

// 	/**
// 	 * Gets the RGB map color as an integer at a given location.
// 	 * 
// 	 * @param phi   the phi location.
// 	 * @param theta the theta location.
// 	 * @return the RGB map color.
// 	 * @Override
//   *	public Color getRGBColor(double phi, double theta) {
// 	 *	    return new Color(getRGBColorInt(phi, theta));
// 	 *  }
// 	 */

 	
 	public int[][] getPixels() {
 		return baseMapPixels;
 	}

	/**
	 * Prepares map panel for deletion.
	 */
	public void destroy() {
	 	baseMapPixels = null;
	 	meta = null;
		program = null;
		kernel = null;
	}
 	
 }
