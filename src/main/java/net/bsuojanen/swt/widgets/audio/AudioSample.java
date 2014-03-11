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

	private static final boolean VERBOSE = true;
	
	private AudioInputStream audioInputStream;
	private AudioFormat format;
	
	private int[][] samplesContainer;
	
	private int sampleMax = 0;
	private int sampleMin = 0;
	private double biggestSample;
	
	public AudioSample(final File file) throws Exception {
		try {

			this.audioInputStream = AudioSystem
					.getAudioInputStream(new BufferedInputStream(
							new FileInputStream(file)));
			this.format = this.audioInputStream.getFormat();

			// Support only mono and stereo audio files.
			if (this.getNumberOfChannels() > 2) {
				out(null);
				throw new UnsupportedAudioFileException(
						"Only mono or stereo audio is currently supported.");
			}

			// Support only audio sampled at 8 or 16 bits.
			if (this.getBitsPerSample() > 16) {
				out(null);
				throw new UnsupportedAudioFileException(
						"Only audio sampled at 8 or 16 bits is currently supported.");
			}

			this.createSampleArrayCollection();

		} catch (UnsupportedAudioFileException e) {
			throw (e);
		} catch (IOException e) {
			throw (e);
		}
	}
	
	/**
	 * i.e. "frame length". 
	 * 
	 * @return
	 */
	public final long getNumberOfFrames() {
		return this.audioInputStream.getFrameLength();
	}
	
	/**
	 * Frames per second. For example, CD quality audio has a framerate of 44100.0.
	 * 
	 * @return
	 */
	public final float getFramerate() {
		return format.getFrameRate();
	}
	
	/**
	 * The biggest sample. Useful for drawing.
	 * 
	 * @return
	 */
	public final double getBiggestSample() {
		return this.biggestSample;
	}

	/**
	 * Mono (1) or stereo (2).
	 * 
	 * @return
	 */
	public final int getNumberOfChannels() {
		return this.format.getChannels();
	}
	
	/**
	 * The number of bits representing a "sample".
	 * 
	 * Currently, only 16 bit audio is well supported but 8 and 24 should be supported.
	 * 
	 * @return
	 */
	public final int getBitsPerSample() {
		return this.format.getSampleSizeInBits();
	}

	/**
	 * The number of bytes in a frame.
	 * 
	 * For example, 8-bit mono audio has a framesize of 1 byte. Conversely, 16-bit 
	 * stereo audio has a framesize of 4 because there are two channels (right and left)
	 * and each channel is expressed by 2 bytes.
	 * 
	 * @return
	 */
	public int getFrameSize() {
		return this.format.getFrameSize();
	}
	
	
	/**
	 * Return the samples for a given channel. Because some audio is mono it is
	 * safe to always pass 0 (zero) as the argument.
	 * 
	 * @param channel
	 * @return
	 */
	public final int[] getAudio(int channel) {
		return this.samplesContainer[channel];
	}

	private final void createSampleArrayCollection() {
		try {
			this.audioInputStream.mark(Integer.MAX_VALUE);
			this.audioInputStream.reset();
			
			int bufferSize = ( (int) this.getNumberOfFrames() ) * this.getFrameSize();
			
			byte[] bytes = new byte[bufferSize];
			
			try {
				this.audioInputStream.read(bytes);
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
			
			out("sampleMin: " + this.sampleMin);
			out("sampleMax: " + this.sampleMax);
			out("this.biggestSample: " + this.biggestSample);
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
	private final int[][] get8BitSampleArray(byte[] eightBitByteArray) {

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
			for (int a = 0; a < this.getNumberOfChannels(); a++) {
				
				int tmp = (int) eightBitByteArray[t];
				int lowNibble = tmp & 0x0f; // lowest 4 bits
				int highNibble = (tmp >> 4) & 0x0f; // highest 4 bits
				
				/**
				 * Okay, I just figured out that a lot of 8-bit audio was/is encoded u-law (mu-law)
				 * or A-law, and is not linear like 16-bit audio. So the following is wrong.
				 * 
				 * See: http://www.hydrogenaudio.org/forums//lofiversion/index.php/t33608.html
				 */
				int sample = (highNibble << 4) + (lowNibble & 0x00ff);
				
				t++;

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
	 * The assumption is 16-bit audio is linear. Every two 8-bit bytes form a 16-bit sample, and
	 * there is one sample for each of the channels. A stereo sample, therefore, has a frame size
	 * of 4 (bytes).
	 * 
	 * @param eightBitByteArray
	 * @return
	 */
	private final int[][] get16BitSampleArray(byte[] eightBitByteArray) {
		
		int chan = this.getNumberOfChannels();
		
		int len = eightBitByteArray.length / ( 2 * chan );
		
		int[][] toReturn = new int[chan][len];
		
		int index = 0;

		// Loop through the byte array
		for (int t = 0; t < eightBitByteArray.length;) {
			// For each iteration, loop through the channels
			for (int a = 0; a < this.getNumberOfChannels(); a++) {

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
	
	private static final void out(final String message) {
		if(VERBOSE) {
			if(message == null) {
				out("----------------------------------------");
			} else {
				System.out.println("TRACE >> AudioSample >> " + message);
			}
			
		}
	}
}
