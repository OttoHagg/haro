package net.bsuojanen.swt.widgets.audio;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class WaveformComposite extends Composite {
	
	protected static final int BACKGROUND_COLOR = SWT.COLOR_BLACK;
	private ArrayList<ChannelCanvas> channelList;
	private AudioSample sample;

	public WaveformComposite(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new FillLayout(SWT.VERTICAL));
		this.setBackground(this.getDisplay().getSystemColor(BACKGROUND_COLOR));
	}
	
	public void setSample(AudioSample sample) {
		if( (this.sample != null) && (this.channelList != null) ) {
			this.reset();
		}
		
		this.sample = sample;
		this.channelList = new ArrayList<ChannelCanvas>();
		int numChannels = this.sample.getNumberOfChannels();
		for (int t = 0; t < numChannels; t++) {
			ChannelCanvas channel = new ChannelCanvas(this, SWT.NONE, this.sample, t);
			this.channelList.add(channel);
			this.layout(true);
		}
	}
	
	public void reset() {
		Control[] controls = this.getChildren();
		for (int i=0; i<controls.length; i++) {
			controls[i].dispose();
		}
		this.sample = null;
		this.channelList = null;
	}
}
