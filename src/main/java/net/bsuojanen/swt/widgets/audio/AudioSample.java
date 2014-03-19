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
				// Ensure the bytes are unsigned...
				this.ensure8bitUnsigned(bytes);
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
	 * Determines signedness of the bytes in the array. For reference:
	 * signed = -128 to 127
	 * unsigned = 0 to 255
	 * 
	 * I keep reading that 8-bit PCM audio is supposed to always be unsigned, but
	 * that's just not the case. Sometimes, Java Sound reports PCM_UNSIGNED when
	 * in fact the bytes are signed. Is it a bug in Java Sound? Were these samples
	 * reporting the encoding incorrectly? I need to investigate further. But, for
	 * now, I'm going to implement a check.
	 * 
	 * @param eightBitByteArray
	 * @return
	 */
	private final boolean is8BitUnsigned(byte[] eightBitByteArray) {
		
		// Loop through the array of bytes
		for (int t = 0; t < eightBitByteArray.length; t++) {
			
			// Get a byte.
			byte eightBitSample = (byte) eightBitByteArray[t];
			
			int sample = this.byteToInt8(eightBitSample);
			
			// If it's less than zero, this indicated we're dealing with signed bytes.
			// Return false now, and stop iterating.
			if(sample < 0) {
				return false;
			}
			
			// If it's greater than 127, this indicates we're dealing with unsigned bytes.
			// Return true now, and stop iterating.
			if(sample > 127) {
				return true;
			}
		}
		
		// Umm, assume true? The bytes are 0 to 127, so it must be?
		return true;
	}
	
	/**
	 * Ensures the bytes in this array are unsigned.
	 * 
	 * @param eightBitByteArray
	 * @return
	 */
	private final byte[] ensure8bitUnsigned(byte[] eightBitByteArray) {
		
		if(this.is8BitUnsigned(eightBitByteArray)) {
			// We're good!
			return eightBitByteArray;
		}
		
		// TODO Change the encoding to be PCM_SIGNED? Does it matter?
		
		for (int t = 0; t < eightBitByteArray.length; t++) {
			eightBitByteArray[t] += 128;
		}
		
		return eightBitByteArray;
	}

	/**
	 * Support for linear (PCM) 8-bit audio.
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
	 * Support for 16-bit PCM audio.
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
	 * Support for 24-bit PCM audio.
	 * 
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
