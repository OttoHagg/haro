package net.bsuojanen.swt.widgets.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioSample {

	private AudioInputStream audioInputStream;
	private int[][] samplesContainer;
	private int numberOfChannels; // mono versus stereo
	private int bitsPerSample; // 8 versus 16 (common) versus 24 (also common)

	// cached values
	protected int sampleMax = 0;
	protected int sampleMin = 0;
	protected double biggestSample;
	
	public AudioSample(final File file) throws Exception {
		try {
			this.audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
			this.bitsPerSample = audioInputStream.getFormat().getSampleSizeInBits();
			if(this.bitsPerSample != 16) {
				throw new UnsupportedAudioFileException("Only audio sampled at 16 bits is currently supported.");
			}
			this.numberOfChannels = this.audioInputStream.getFormat().getChannels();
			this.createSampleArrayCollection();
		} catch (UnsupportedAudioFileException | IOException e) {
			throw(e);
		}
	}

	public final int getNumberOfChannels() {
		return this.numberOfChannels;
	}
	
	public final int getBitsPerSample() {
		return this.bitsPerSample;
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

	/**
	 * The assumption is every two 8-bit bytes form a 16-bit sample, and that there is one sample for each of the channels.
	 * FIXME - Handle various sample rates, not just 16 bits per sample.
	 * 
	 * @param eightBitByteArray
	 * @return
	 */
	protected final int[][] getSampleArray(byte[] eightBitByteArray) {
		
		int[][] toReturn = new int[this.numberOfChannels][ eightBitByteArray.length / ( 2 * this.numberOfChannels ) ];
		
		int index = 0;
		int numChannels = this.getNumberOfChannels();

		// Loop through the byte array
		for (int t = 0; t < eightBitByteArray.length;) {
			// For each iteration, loop through the channels
			for (int a = 0; a < numChannels; a++) {
				
					int low = (int) eightBitByteArray[t];
					t++;
					int high = (int) eightBitByteArray[t];
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
