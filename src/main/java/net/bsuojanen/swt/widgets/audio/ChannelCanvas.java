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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ChannelCanvas extends Canvas {
	
	protected static final int BACKGROUND_COLOR = SWT.COLOR_BLACK;
	protected static final int REFERENCE_LINE_COLOR = SWT.COLOR_GRAY;
	protected static final int WAVEFORM_COLOR = SWT.COLOR_MAGENTA;

	private AudioSample sample;
	private int channelIndex;

	public ChannelCanvas(Composite parent, int style, AudioSample sample, int channelIndex) {
		super(parent, style);
		this.sample = sample;
		this.channelIndex = channelIndex;
		this.setBackground(this.getDisplay().getSystemColor(BACKGROUND_COLOR));
		this.addPaintListener(new ChannelPainter());
	}
	
	private final class ChannelPainter implements PaintListener {
		public void paintControl(PaintEvent e) {
			Canvas canvas = (Canvas) e.widget;
			int lineHeight = canvas.getSize().y / 2;
			e.gc.setForeground(e.display.getSystemColor(REFERENCE_LINE_COLOR));
			e.gc.drawLine(0, lineHeight, (int) canvas.getSize().x, lineHeight);
			drawWaveform(e, sample.getAudio(channelIndex));
		}
	}
	
	protected final void drawWaveform(PaintEvent e, int[] samples) {
		
		if (samples == null) {
			return;
		}

		Canvas canvas = (Canvas) e.widget;
		int canvasHeight = canvas.getSize().y;
		int canvasWidth = canvas.getSize().x;
		
		int oldX = 0;
		int oldY = (int) (canvasHeight / 2);
		int xIndex = 0;

		// TODO - Understand why increment is low for 8-bit audio.
		int increment = this.getIncrement(this.getXScaleFactor( canvasWidth ));
		e.gc.setForeground(e.display.getSystemColor(WAVEFORM_COLOR));

		int t = 0;

		for (t = 0; t < increment; t += increment) {
			e.gc.drawLine(oldX, oldY, xIndex, oldY);
			xIndex++;
			oldX = xIndex;
		}

		for (; t < samples.length; t += increment) {
			// TODO - Understand why scaleFactor is so large for 8-bit samples
			// TODO - We can probably cache scaleFactor for performance improvement.
			double scaleFactor = this.getYScaleFactor(canvasHeight);
			//out("scaleFactor: " + scaleFactor);
			double scaledSample = samples[t] * scaleFactor;
			int y = (int) ((canvasHeight / 2) - (scaledSample));
			e.gc.drawLine(oldX, oldY, xIndex, y);

			xIndex++;
			oldX = xIndex;
			oldY = y;
		}
	}
	
	public final double getXScaleFactor(int panelWidth) {
		return (panelWidth / ((double) this.sample.getAudio(0).length));
	}
	
	public final double getYScaleFactor(int panelHeight) {
		
		return (panelHeight / (this.sample.getBiggestSample() * 2 * 1.2));
	}
	
	public final int getIncrement(double xScale) {
		try {
			int increment = (int) (this.sample.getAudio(0).length / (this.sample.getAudio(0).length * xScale));
			return increment;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
}
