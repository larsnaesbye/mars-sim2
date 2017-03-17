/**
 * Mars Simulation Project
 * OGGSoundClip.java
 * @version 3.1.0 2017-03-17
 * @author Lars Naesbye Christensen (complete rewrite for OGG)
 * Based on JOrbisPlayer example source
 */

package org.mars_sim.msp.ui.swing.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.mars_sim.msp.ui.javafx.MainScene;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

public class OGGSoundClip {

	private static Logger logger = Logger.getLogger(OGGSoundClip.class.getName());

	private final int BUFSIZE = 4096 * 2;
	private int convsize = BUFSIZE * 2;
	private byte[] convbuffer = new byte[convsize];
	private SyncState oy;
	private StreamState os;
	private Page og;
	private Packet op;
	private Info vi;
	private Comment vc;
	private DspState vd;
	private Block vb;
	private SourceDataLine outputLine;
	private int rate;
	private int channels;
	private BufferedInputStream bitStream = null;
	private byte[] buffer = null;
	private int bytes = 0;
	private Thread player = null;

	private float balance;
	private float gain = .8f;
	private float oldGain;
	private float volume = .8f;

	private boolean mute;
	private boolean paused;

	/**
	 * Create a new clip based on a reference into the class path
	 *
	 * @param ref
	 *            The reference into the class path which the OGG can be read
	 *            from
	 * @throws IOException
	 *             Indicated a failure to find the resource
	 */
	public OGGSoundClip(String ref) throws IOException {
		try {
			init(Thread.currentThread().getContextClassLoader().getResourceAsStream(ref));
		} catch (IOException e) {
			//throw new IOException("Couldn't find: " + ref);
			logger.log(Level.SEVERE, "Couldn't find: " + ref);
		}
	}

	/**
	 * Create a new clip based on a reference into the class path
	 *
	 * @param in
	 *            The stream from which the OGG can be read from
	 * @throws IOException
	 *             Indicated a failure to read from the stream
	 */
	public OGGSoundClip(InputStream in) throws IOException {
		init(in);
	}

	/**
	 * Set the default gain value (default volume)
	 */
	public void setDefaultGain() {
		setGain(.8f);
	}

	public float getGain() {
		return gain;
	}

	/**
	 * Attempt to set the global gain for the playback. If the control is not
	 * supported this method has no effect. 1.0 will set maximum gain, 0.0
	 * minimum gain
	 *
	 * @param gain
	 *            The gain value

	public void setGain(float gain) {
		if (gain != -1) {
			if ((gain < 0) || (gain > 1)) {
				throw new IllegalArgumentException("Volume must be between 0.0 and 1.0");
			}
		}

		this.gain = gain;

		if (outputLine == null) {
			return;
		}

		try {
			FloatControl control = (FloatControl) outputLine.getControl(FloatControl.Type.MASTER_GAIN);
			if (gain == -1) {
				control.setValue(0);
			} else {
				float max = control.getMaximum();
				float min = control.getMinimum(); // negative values all seem to
													// be zero?
				float range = max - min;

				control.setValue(min + (range * gain));
			}
		} catch (IllegalArgumentException e) {
			// gain not supported
			e.printStackTrace();
		}
	}
 */


	/**
	 * Compute the gain value for the playback--based on the new value of volume in the increment or decrement of 0.05f.
	 * @param volume the volume
	 */
	public void setGain(float volume) {
		//System.out.println("OGGSoundClip's setGain() is on " + Thread.currentThread().getName());

		if (volume > 1)
			volume = 1;
		else if (volume < 0)
			volume = 0;

		this.volume = volume;

		//System.out.println("volume : " + volume);

		if (outputLine == null) {
			return;
		}

		try {
			// Note: control is supposed to be in decibel (dB)
			//FloatControl control = (FloatControl) outputLine.getControl(FloatControl.Type.MASTER_GAIN);

			FloatControl control = null;
			//DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
			//SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
			//outputLine.open();
			// Adjust the volume on the output line.
			if(outputLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			    // If inside this if, the Master_Gain must be supported. Yes?
				control = (FloatControl) outputLine.getControl(FloatControl.Type.MASTER_GAIN);
			    // This line throws an exception. "Master_Gain not supported"
				//control.setValue( 100.0F );


				//  A positive gain amplifies (boosts) the signal's volume,
				//  A negative gain attenuates (cuts) it.
				// The gain setting defaults to a value of 0.0 dB, meaning the signal's loudness is unaffected.
				// Note that gain measures dB, not amplitude.
				float max = control.getMaximum();
				float min = control.getMinimum();

				float range = max - min;
				float step = range/20f;
				float num = volume/0.05f;
				float value = min + num * step;

				if (value < min)
					value = min;
				else if (value > max)
					value = max;

				control.setValue(value);

				//System.out.println("max : " + max);
				//System.out.println("min : " + min);
				//System.out.println("range : " + range);
				//System.out.println("step : " + step);
				//System.out.println("value : " + value);
			}
			else {
				MainScene.disableSound();
			}


		} catch (IllegalArgumentException e) {
			// TODO: how to resolve 'IllegalArgumentException: Master Gain not supported' in ubuntu ?
			e.printStackTrace();
			MainScene.disableSound();
		}

	}

	public float getVolume() {
		return volume;
	}

	/*
	public void setVolume() {
		try {
		// use FloatControl.Type.VOLUME
			FloatControl control = (FloatControl) outputLine.getControl(FloatControl.Type.VOLUME);
			float max = control.getMaximum();
			float min = control.getMinimum();
			float range = max - min;
			float value = min + (range * gain);
			if (value < min)
				value = min;
			else if (value > max)
				value = max;

			control.setValue(value);

			System.out.println("max : " + max);
			System.out.println("min : " + min);
			System.out.println("range : " + range);
			System.out.println("value : " + value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
*/

	/**
	 * Attempt to set the balance between the two speakers. -1.0 is full left
	 * speak, 1.0 if full right speaker. Anywhere in between moves between the
	 * two speakers. If the control is not supported this method has no effect
	 *
	 * @param balance
	 *            The balance value
	 */
	public void setBalance(float balance) {
		this.balance = balance;

		if (outputLine == null) {
			return;
		}

		try {
			FloatControl control = (FloatControl) outputLine.getControl(FloatControl.Type.BALANCE);
			control.setValue(balance);
		} catch (IllegalArgumentException e) {
			// balance not supported
		}
	}

	/**
	 * Check the state of the playback
	 *
	 * @return True if the playback has been stopped
	 */
	private boolean checkState() {
		while (paused && (player != null)) {
			synchronized (player) {
				if (player != null) {
					try {
						player.wait();
					} catch (InterruptedException e) {
						// ignored
					}
				}
			}
		}

		return stopped();
	}

	/**
	 * Pause the playback
	 */
	public void pause() {
		paused = true;
		oldGain = gain;
		setGain(0);
	}

	/**
	 * Check if the stream is paused
	 *
	 * @return True if the stream is paused
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Resume the playback
	 */
	public void resume() {
		if (!paused) {
			play();
			return;
		}

		paused = false;

		synchronized (player) {
			if (player != null) {
				player.notify();
			}
		}
		setGain(oldGain);
	}

	/**
	 * Check if the clip has been stopped
	 *
	 * @return True if the clip has been stopped
	 */
	public boolean stopped() {
		return ((player == null) || (!player.isAlive()));
	}

	/**
	 * Initialise the OGG clip
	 *
	 * @param in
	 *            The stream we're going to read from
	 * @throws IOException
	 *             Indicates a failure to read from the stream
	 */
	private void init(InputStream in) throws IOException {
		if (in == null) {
			//throw new IOException("Couldn't find input source");
			logger.log(Level.SEVERE, "Couldn't find input source");
		}
		bitStream = new BufferedInputStream(in);
		bitStream.mark(Integer.MAX_VALUE);
	}

	/**
	 * Play the clip once - for sound effects
	 */
	public void play() {
		//System.out.println("OGGSoundClip's play() is on " + Thread.currentThread().getName());
		stop();

		try {
			bitStream.reset();
		} catch (IOException e) {
			// ignore if no mark
			logger.log(Level.SEVERE, "IOException", e.getMessage());
		}

		player = new Thread() {
			public void run() {
				//try {
					playStream(Thread.currentThread());
				//} catch (InternalException e) {
					//e.printStackTrace();
				//	logger.log(Level.SEVERE, "InternalException", e.getMessage());
				//}

				try {
					bitStream.reset();
				} catch (IOException e) {
					//e.printStackTrace();
					logger.log(Level.SEVERE, "IOException", e.getMessage());
				}
			};
		};
		player.setDaemon(true);
		player.start();
	}

	/**
	 * Loop the clip - maybe for background music
	 */
	public void loop() {
		stop();

		try {
			bitStream.reset();
		} catch (IOException e) {
			// ignore if no mark
		}

		player = new Thread() {
			public void run() {
				while (player == Thread.currentThread()) {
					try {
						playStream(Thread.currentThread());
					} catch (Exception e) {
					//	logger.log(Level.SEVERE, "Troubleshooting audio : have you plugged in a speaker/headphone? "
					//			+ "Please check your audio source.", e.getMessage());
						//e.printStackTrace();
						player = null;
					}

					try {
						bitStream.reset();
					} catch (IOException e) {
					}
				}
			};
		};
		player.setDaemon(true);
		player.start();
	}

	/**
	 * Stop the clip playing
	 */
	public void stop() {
		if (stopped()) {
			return;
		}

		player = null;
		outputLine.drain();
	}

	/**
	 * Close the stream being played from
	 */
	public void close() {
		try {
			if (bitStream != null)
				bitStream.close();
		} catch (IOException e) {
		}
	}

	/*
	 * Taken from JOrbisPlayer
	 */
	private void initJavaSound(int channels, int rate) {
		try {
			AudioFormat audioFormat = new AudioFormat(rate, 16, channels,
					true, // PCM_Signed
					false // littleEndian
					);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
			if (!AudioSystem.isLineSupported(info)) {
				//throw new Exception("Line " + info + " not supported.");
				logger.log(Level.SEVERE, "Troubleshooting audio : have you plugged in a speaker/headphone? "
						+ "Please check your audio source.");

			}

			try {
				outputLine = (SourceDataLine) AudioSystem.getLine(info);
				// outputLine.addLineListener(this);
				outputLine.open(audioFormat);
			} catch (LineUnavailableException ex) {
				//throw new Exception("Unable to open the sourceDataLine: " + ex);
				logger.log(Level.SEVERE, "Unable to open the sourceDataLine: " + ex);
			} catch (IllegalArgumentException ex) {
				//throw new Exception("Illegal Argument: " + ex);
				logger.log(Level.SEVERE, "Illegal Argument: " + ex);
				logger.log(Level.SEVERE, "Will continue to run the sim without audio");
			}

			this.rate = rate;
			this.channels = channels;

			setBalance(balance);
			setGain(volume);
		} catch (Exception ee) {
			System.out.println(ee);
		}
	}

	/*
	 * Taken from JOrbisPlayer
	 */
	private SourceDataLine getOutputLine(int channels, int rate) {
		if (outputLine == null || this.rate != rate || this.channels != channels) {
			if (outputLine != null) {
				outputLine.drain();
				outputLine.stop();
				outputLine.close();
			}
			initJavaSound(channels, rate);
			outputLine.start();
		}
		return outputLine;
	}

	/*
	 * Taken from JOrbisPlayer
	 */
	private void initJOrbis() {
		oy = new SyncState();
		os = new StreamState();
		og = new Page();
		op = new Packet();

		vi = new Info();
		vc = new Comment();
		vd = new DspState();
		vb = new Block(vd);

		buffer = null;
		bytes = 0;

		oy.init();
	}

	/*
	 * Taken from the JOrbis Player
	 */
	private void playStream(Thread me) {//throws InternalException {
		boolean chained = false;

		initJOrbis();

		while (true) {
			if (checkState()) {
				return;
			}

			int eos = 0;

			int index = oy.buffer(BUFSIZE);
			buffer = oy.data;
			try {
				bytes = bitStream.read(buffer, index, BUFSIZE);
			} catch (Exception e) {
				//throw new InternalException(e);
				logger.log(Level.SEVERE, "Audio Troubleshooting : have a speaker/headphone been plugged in ? "
						+ "Please check your audio source.", e.getMessage());
			}

			oy.wrote(bytes);

			if (chained) {
				chained = false;
			} else {
				if (oy.pageout(og) != 1) {
					if (bytes < BUFSIZE)
						break;
					//throw new InternalException("Input does not appear to be an Ogg bitstream.");
					logger.log(Level.SEVERE, "Input does not appear to be an Ogg bitstream.");
				}
			}
			os.init(og.serialno());
			os.reset();

			vi.init();
			vc.init();

			if (os.pagein(og) < 0) {
				// error; stream version mismatch perhaps
				//throw new InternalException("Error reading first page of OGG bitstream data.");
				logger.log(Level.SEVERE, "Error reading first page of OGG bitstream data.");
			}

			if (os.packetout(op) != 1) {
				// no page? must not be vorbis
				//throw new InternalException("Error reading initial header packet.");
				logger.log(Level.SEVERE, "Error reading initial header packet.");
			}

			if (vi.synthesis_headerin(vc, op) < 0) {
				// error case; not a vorbis header
				//throw new InternalException("This Ogg bitstream does not contain Vorbis audio data.");
				logger.log(Level.SEVERE, "This Ogg bitstream does not contain Vorbis audio data.");
			}

			int i = 0;

			while (i < 2) {
				while (i < 2) {
					if (checkState()) {
						return;
					}

					int result = oy.pageout(og);
					if (result == 0)
						break; // Need more data
					if (result == 1) {
						os.pagein(og);
						while (i < 2) {
							result = os.packetout(op);
							if (result == 0)
								break;
							if (result == -1) {
								//throw new InternalException("Corrupt secondary header. Exiting.");
								logger.log(Level.SEVERE, "Corrupt secondary header. Exiting.");
							}
							vi.synthesis_headerin(vc, op);
							i++;
						}
					}
				}

				index = oy.buffer(BUFSIZE);
				buffer = oy.data;
				try {
					bytes = bitStream.read(buffer, index, BUFSIZE);
				} catch (Exception e) {
					//throw new InternalException(e);
					logger.log(Level.SEVERE, "Exception", e.getMessage());
				}
				if (bytes == 0 && i < 2) {
					//throw new InternalException("End of file before finding all Vorbis headers!");
					logger.log(Level.SEVERE, "End of file before finding all Vorbis headers!");
				}
				oy.wrote(bytes);
			}

			convsize = BUFSIZE / vi.channels;

			vd.synthesis_init(vi);
			vb.init(vd);

			float[][][] _pcmf = new float[1][][];
			int[] _index = new int[vi.channels];

			getOutputLine(vi.channels, vi.rate);

			while (eos == 0) {
				while (eos == 0) {
					if (player != me) {
						return;
					}

					int result = oy.pageout(og);
					if (result == 0)
						break; // need more data
					if (result == -1) { // missing or corrupt data at this page
						// position
						// System.err.println("Corrupt or missing data in
						// bitstream;
						// continuing...");
					} else {
						os.pagein(og);

						if (og.granulepos() == 0) { //
							chained = true; //
							eos = 1; //
							break; //
						} //

						while (true) {
							if (checkState()) {
								return;
							}

							result = os.packetout(op);
							if (result == 0)
								break; // need more data
							if (result == -1) { // missing or corrupt data at
								// this page position
								// no reason to complain; already complained
								// above

								// System.err.println("no reason to complain;
								// already complained above");
							} else {
								// we have a packet. Decode it
								int samples;
								if (vb.synthesis(op) == 0) { // test for
									// success!
									vd.synthesis_blockin(vb);
								}
								while ((samples = vd.synthesis_pcmout(_pcmf, _index)) > 0) {
									if (checkState()) {
										return;
									}

									float[][] pcmf = _pcmf[0];
									int bout = (samples < convsize ? samples : convsize);

									// convert doubles to 16 bit signed ints
									// (host order) and
									// interleave
									for (i = 0; i < vi.channels; i++) {
										int ptr = i * 2;
										// int ptr=i;
										int mono = _index[i];
										for (int j = 0; j < bout; j++) {
											int val = (int) (pcmf[i][mono + j] * 32767.);
											if (val > 32767) {
												val = 32767;
											}
											if (val < -32768) {
												val = -32768;
											}
											if (val < 0)
												val = val | 0x8000;
											convbuffer[ptr] = (byte) (val);
											convbuffer[ptr + 1] = (byte) (val >>> 8);
											ptr += 2 * (vi.channels);
										}
									}
									outputLine.write(convbuffer, 0, 2 * vi.channels * bout);
									vd.synthesis_read(bout);
								}
							}
						}
						if (og.eos() != 0)
							eos = 1;
					}
				}

				if (eos == 0) {
					index = oy.buffer(BUFSIZE);
					buffer = oy.data;
					try {
						bytes = bitStream.read(buffer, index, BUFSIZE);
					} catch (Exception e) {
						//throw new InternalException(e);
						logger.log(Level.SEVERE, "Exception", e.getMessage());
					}
					if (bytes == -1) {
						break;
					}
					oy.wrote(bytes);
					if (bytes == 0)
						eos = 1;
				}
			}

			os.clear();
			vb.clear();
			vd.clear();
			vi.clear();
		}

		oy.clear();
	}

	public boolean isMute() {
		return mute;
	}

	public void setMute(boolean mute) {
		// Set mute value.
		this.mute = mute;

		if (outputLine == null) {
			return;
		}
		else if (outputLine.isControlSupported(BooleanControl.Type.MUTE)) {
			BooleanControl muteControl = (BooleanControl) outputLine.getControl(BooleanControl.Type.MUTE);
			muteControl.setValue(mute);
		}

	}
/*
	private class InternalException extends Exception {

		private static final long serialVersionUID = 1L;

		public InternalException(Exception e) {
			super(e);
		}

		public InternalException(String msg) {
			super(msg);
		}
	}
*/
}
