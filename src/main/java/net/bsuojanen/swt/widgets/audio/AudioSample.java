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
				throw new UnsupportedAudioFileException(
						"Only mono or stereo audio is currently supported.");
			}

			// We support 8-bit (PCM), 16-bit, and 24-bit audio.
			if (this.getBitsPerSample() > 24) {
				throw new UnsupportedAudioFileException(
						"Audio recorded higher than 24 bits per sample is not currently supported.");
			}
			
			if(this.getBitsPerSample() == 8) {
				if(this.getEncoding().equals("ALAW")) {
					throw new UnsupportedAudioFileException(
							"A-law enconding is not currently supported.");
				}
				
				if(this.getEncoding().equals("ULAW")) {
					throw new UnsupportedAudioFileException(
							"U-law enconding is not currently supported.");
				}
			}

			this.createSampleArrayCollection();

		} catch (UnsupportedAudioFileException e) {
			throw (e);
		} catch (IOException e) {
			throw (e);
		}
	}
	
	public final float getDuration() {
		return ( (float) this.getNumberOfFrames() / this.getFramerate() );
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
	 * Frames per second. For example, CD quality audio has a frame rate of 44100.0.
	 * 
	 * @return
	 */
	public final float getFramerate() {
		return format.getFrameRate();
	}
	
	/**
	 * CD quality audio has a sample rate of 44100.0.
	 * 
	 * @return
	 */
	public final float getSampleRate() {
		return this.format.getSampleRate();
	}
	
	/**
	 * @return
	 */
	public final boolean isBigEndian() {
		return this.format.isBigEndian();
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
	 * Java Sound supports ALAW, ULAW, PCM_FLOAT, PCM_SIGNED, and PCM_UNSIGNED.
	 *  
	 * @return
	 */
	public final String getEncoding() {
		return this.format.getEncoding().toString();
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
			} else if(this.getBitsPerSample() == 24) {
				this.samplesContainer = this.get24BitSampleArray(bytes);
			} else {
				this.samplesContainer = this.get16BitSampleArray(bytes);
			}
			
			// Find biggest sample. Useful for interpolating the yScaleFactor (ex. drawing a waveform).
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
	 * This is for linear (PCM_UNSIGNED) 8-bit audio. The result is "loud" so it
	 * probably needs some kind of normalization.
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
				
				byte eightBitSample = (byte) eightBitByteArray[t];
				
				//System.out.println(eightBitSample);
				
				// TODO: What the hell? the format is "PCM_UNSIGNED" but I'm seeing negatives. Come back to this.
				
				// Convert from signed (-128 to 127) to unsigned (0 to 255).
				// Most, if not all, 8-bit PCM samples are unsigned.
				if(this.getEncoding().equals("PCM_SIGNED")) {
					eightBitSample += 128;
				}
				
				int sample = this.byteToInt8(eightBitSample);
				
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

					byte low = eightBitByteArray[t];
					t++;
					
					byte high = eightBitByteArray[t];
					t++;
					
					int sample = this.bytesToInt16(high, low);

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
	 * @param eightBitByteArray
	 * @return
	 */
	private final int[][] get24BitSampleArray(byte[] eightBitByteArray) {
		
		int chan = this.getNumberOfChannels();
		
		int len = eightBitByteArray.length / ( 3 * chan );
		
		int[][] toReturn = new int[chan][len];
		
		int index = 0;

		// Loop through the byte array
		for (int t = 0; t < eightBitByteArray.length;) {
			// For each iteration, loop through the channels
			for (int a = 0; a < this.getNumberOfChannels(); a++) {

					byte low = eightBitByteArray[t];
					t++;
					
					byte mid = eightBitByteArray[t];
					t++;
					
					byte high = eightBitByteArray[t];
					t++;
					
					int sample = bytesToInt24(low, mid, high, this.isBigEndian());

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
	
	private final int bytesToInt24(byte low, byte mid, byte high, boolean bigEndian) {
		return bigEndian ? ((low << 16) | ((mid & 0xFF) << 8) | (high & 0xFF)) : ((high << 16) | ((mid & 0xFF) << 8) | (low & 0xFF));
	}

	private final int bytesToInt16(byte highByte, byte lowByte) {
		return (highByte << 8) | (lowByte & 0xFF);
	}
	
	private final int byteToInt8(byte theByte) {
		return theByte;
	}
}
