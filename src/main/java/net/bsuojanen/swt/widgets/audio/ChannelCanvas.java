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

		int increment = sample.getIncrement(sample.getXScaleFactor( canvasWidth ));
		e.gc.setForeground(e.display.getSystemColor(WAVEFORM_COLOR));

		int t = 0;

		for (t = 0; t < increment; t += increment) {
			e.gc.drawLine(oldX, oldY, xIndex, oldY);
			xIndex++;
			oldX = xIndex;
		}

		for (; t < samples.length; t += increment) {
			double scaleFactor = sample.getYScaleFactor(canvasHeight);
			double scaledSample = samples[t] * scaleFactor;
			int y = (int) ((canvasHeight / 2) - (scaledSample));
			e.gc.drawLine(oldX, oldY, xIndex, y);

			xIndex++;
			oldX = xIndex;
			oldY = y;
		}
	}
}