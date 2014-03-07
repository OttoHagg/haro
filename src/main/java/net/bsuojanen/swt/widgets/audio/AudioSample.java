package net.bsuojanen.swt.widgets.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioSample {
	private static final int NUM_BITS_PER_BYTE = 8;

	private AudioInputStream audioInputStream;
	private int[][] samplesContainer;

	// cached values
	protected int sampleMax = 0;
	protected int sampleMin = 0;
	protected double biggestSample;
	
	public AudioSample(File file) throws Exception {
		try {
			this.audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
			this.createSampleArrayCollection();
		} catch (UnsupportedAudioFileException | IOException e) {
			throw(e);
		}
	}

	public final int getNumberOfChannels() {
		int numBytesPerSample = audioInputStream.getFormat()
				.getSampleSizeInBits()
				/ NUM_BITS_PER_BYTE;
		return audioInputStream.getFormat().getFrameSize() / numBytesPerSample;
	}

	private final void createSampleArrayCollection() {
		try {
			audioInputStream.mark(Integer.MAX_VALUE);
			audioInputStream.reset();
			byte[] bytes = new byte[ (int) (audioInputStream.getFrameLength()) * ( (int) audioInputStream.getFormat().getFrameSize() ) ];
			
			try {
				audioInputStream.read(bytes);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// convert sample bytes to channel separated 16 bit samples
			samplesContainer = getSampleArray(bytes);

			// find biggest sample. used for interpolating the yScaleFactor
			if (sampleMax > sampleMin) {
				biggestSample = sampleMax;
			} else {
				biggestSample = Math.abs(((double) sampleMin));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected final int[][] getSampleArray(byte[] eightBitByteArray) {
		
		int[][] toReturn = new int[getNumberOfChannels()][eightBitByteArray.length / (2 * getNumberOfChannels())];
		
		int index = 0;
		int numChannels = this.getNumberOfChannels();

		// loop through the byte[]
		for (int t = 0; t < eightBitByteArray.length;) {
			// for each iteration, loop through the channels
			for (int a = 0; a < numChannels; a++) {
				
					int low = (int) eightBitByteArray[t];
					t++;
					/*
					 * The assumption is every two 8-bit bytes form a 16-bit sample, and that there is one sample for each of the channels.
					 */
					int high = (int) eightBitByteArray[t]; // FIXME - Anything (typically mono) sampled at 8 bit barfs here.
					// java.lang.ArrayIndexOutOfBoundsException
					t++;
					int sample = (high << 8) + (low & 0x00ff);

					if (sample < sampleMin) {
						sampleMin = sample;
					} else if (sample > sampleMax) {
						sampleMax = sample;
					}
					
					toReturn[a][index] = sample;
			}
			index++;
		}

		return toReturn;
	}

	public final double getXScaleFactor(int panelWidth) {
		return (panelWidth / ((double) samplesContainer[0].length));
	}

	public final double getYScaleFactor(int panelHeight) {
		return (panelHeight / (biggestSample * 2 * 1.2));
	}

	public final int[] getAudio(int channel) {
		return samplesContainer[channel];
	}

	public final int getIncrement(double xScale) {
		try {
			int increment = (int) (samplesContainer[0].length / (samplesContainer[0].length * xScale));
			return increment;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

}
