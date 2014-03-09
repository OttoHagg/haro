/*******************************************************************************
 * Copyright (c) 2014 Brian T. Suojanen. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Brian T. Suojanen (brian dot suojanen at outlook dot com)
 *******************************************************************************/
package net.bsuojanen.swt.widgets.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioSample {

	private static final boolean DEBUG = true;
	
	private AudioInputStream audioInputStream;
	private int[][] samplesContainer;
	private int numberOfChannels; // mono versus stereo
	private int bitsPerSample; // 8 versus 16 (common) versus 24 (also common)
	private int frameSize;
	private long numberOfFrames; // i.e. frame length
	private float framerate;
	
	protected int sampleMax = 0;
	protected int sampleMin = 0;
	protected double biggestSample;
	
	public AudioSample(final File file) throws Exception {
		try {
			
			this.openFile(file);
			
			AudioFormat format = this.audioInputStream.getFormat();

			this.bitsPerSample = format.getSampleSizeInBits();
			out("Bits per sample: " + this.bitsPerSample);
			
			this.numberOfChannels = format.getChannels();
			out("Number of channels: " + this.numberOfChannels);
			
			this.framerate = format.getFrameRate();
			out("Framerate: " + this.framerate);

			this.frameSize = format.getFrameSize();
			out("Framesize: " + this.frameSize + " byte(s)");
			
			this.numberOfFrames = this.audioInputStream.getFrameLength();
			out("Number of frames: " + this.numberOfFrames + " frames");
			
			// Support only mono and stereo audio files.
			if(this.getNumberOfChannels() > 2) {
				out(null);
				throw new UnsupportedAudioFileException("Only mono or stereo audio is currently supported.");
			}
			
			// Support only audio sampled at 8 or 16 bits.
			if(this.getBitsPerSample() > 16) {
				out(null);
				throw new UnsupportedAudioFileException("Only audio sampled at 8 or 16 bits is currently supported.");
			}
			
			this.createSampleArrayCollection();
			
			out(null);
		} catch (UnsupportedAudioFileException e) {
			throw(e);
		} catch(IOException e) {
			throw(e);
		}
	}
	
	private final void openFile(final File file) throws UnsupportedAudioFileException, IOException {
		out("Opening: " +file.getAbsolutePath());
		out("Size: " + file.length());
		try {
			this.audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
		} catch (UnsupportedAudioFileException e) {
			throw(e);
		} catch(IOException e) {
			throw(e);
		}
		
	}

	public final int getNumberOfChannels() {
		return this.numberOfChannels;
	}
	
	public final int getBitsPerSample() {
		return this.bitsPerSample;
	}

	public int getFrameSize() {
		return this.frameSize;
	}

	public long getFrameLength() {
		return this.numberOfFrames;
	}

	private final void createSampleArrayCollection() {
		try {
			this.audioInputStream.mark(Integer.MAX_VALUE);
			this.audioInputStream.reset();
			
			int bufferSize = ( (int) this.numberOfFrames ) * this.frameSize;
			
			byte[] bytes = new byte[bufferSize];
			
			int n = 0;
			try {
				n = this.audioInputStream.read(bytes);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(this.getBitsPerSample() == 8) {
				this.samplesContainer = this.get8BitSampleArray(bytes);
			} else {
				this.samplesContainer = this.get16BitSampleArray(bytes);
			}
			
			// find biggest sample. used for interpolating the yScaleFactor
			if (this.sampleMax > this.sampleMin) {
				this.biggestSample = this.sampleMax;
			} else {
				this.biggestSample = Math.abs(((double) this.sampleMin));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * For an 8-bit sample, every two nibbles (4 bits) form a sample, and there is one sample for each channel. These
	 * are usually mono and, therefore, the frame size is 1 byte. So, to support 8 bit, we have to split each byte
	 * in half to get the low and high sample bits.
	 * 
	 * @param eightBitByteArray
	 * @return
	 */
	protected final int[][] get8BitSampleArray(byte[] eightBitByteArray) {

		int chan = this.getNumberOfChannels();

		int len = 0;
		
		if(chan == 1) {
			len = eightBitByteArray.length;
		} else {
			len = eightBitByteArray.length / 2;
		}

		int[][] toReturn = new int[chan][len];

		int index = 0;

		// Loop through the byte array
		for (int t = 0; t < eightBitByteArray.length;) {
			// For each iteration, loop through the channels
			for (int a = 0; a < this.numberOfChannels; a++) {

				int lowNibble = (int) eightBitByteArray[t] & 0x0f; // lowest 4 bits
				int highNibble = ((int) eightBitByteArray[t] >> 4) & 0x0f; // highest 4 bits
				t++;

				int sample = (highNibble << 4) + (lowNibble & 0x0f);

				if (sample < this.sampleMin) {
					this.sampleMin = sample;
				} else if (sample > this.sampleMax) {
					this.sampleMax = sample;
				}

				toReturn[a][index] = sample;
			}
			index++;
		}

		return toReturn;
	}
	
	/**
	 * The assumption is every two 8-bit bytes form a 16-bit sample, and there is one sample for each of the channels.
	 * A stereo sample, therefore, has a frame size of 4 (bytes).
	 * 
	 * @param eightBitByteArray
	 * @return
	 */
	protected final int[][] get16BitSampleArray(byte[] eightBitByteArray) {
		
		int chan = this.numberOfChannels;
		
		int len = eightBitByteArray.length / ( 2 * chan );
		
		int[][] toReturn = new int[chan][len];
		
		int index = 0;

		// Loop through the byte array
		for (int t = 0; t < eightBitByteArray.length;) {
			// For each iteration, loop through the channels
			for (int a = 0; a < this.numberOfChannels; a++) {
				
					int low = (int) eightBitByteArray[t];
					t++;
					
					int high = (int) eightBitByteArray[t];
					t++;
					
					int sample = (high << 8) + (low & 0x00ff);

					if (sample < this.sampleMin) {
						this.sampleMin = sample;
					} else if (sample > this.sampleMax) {
						this.sampleMax = sample;
					}
					
					toReturn[a][index] = sample;
			}
			index++;
		}

		return toReturn;
	}

	public final double getXScaleFactor(int panelWidth) {
		return (panelWidth / ((double) this.samplesContainer[0].length));
	}

	public final double getYScaleFactor(int panelHeight) {
		return (panelHeight / (this.biggestSample * 2 * 1.2));
	}

	public final int[] getAudio(int channel) {
		return this.samplesContainer[channel];
	}

	public final int getIncrement(double xScale) {
		try {
			int increment = (int) (this.samplesContainer[0].length / (this.samplesContainer[0].length * xScale));
			return increment;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static final void out(final String message) {
		if(DEBUG) {
			if(message == null) {
				out("----------------------------------------");
			} else {
				System.out.println("TRACE >> " + message);
			}
			
		}
	}
}
