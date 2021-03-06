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
package net.bsuojanen.swt.snippets;

import java.io.File;

import javax.sound.sampled.UnsupportedAudioFileException;

import net.bsuojanen.swt.widgets.audio.AudioSample;
import net.bsuojanen.swt.widgets.audio.WaveformComposite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class WaveformExample {
	
	private static final boolean VERBOSE = true;
	
	private WaveformComposite waveform;
	private Shell shell;
	private final static String title = "Waveform Example";

	public WaveformExample() {
		final Display display = new Display();
		this.shell = new Shell(display, SWT.DIALOG_TRIM);
		this.shell.setText(title);
		
		this.shell.setLayout(new FillLayout());
		this.createMenus();
		this.waveform = new WaveformComposite(shell, SWT.NONE);
		this.shell.setSize(800, 400);
		this.shell.open();
		
		while (! shell.isDisposed()) {
			if (! display.readAndDispatch()) display.sleep();
		}
		
		display.dispose();
	}
	
	private final void createMenus() {
		final Menu menuBar = new Menu(this.shell, SWT.BAR);
		this.shell.setMenuBar(menuBar);
		// File menu
		final MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
		fileItem.setText("File");
		final Menu fileMenu = new Menu(this.shell, SWT.DROP_DOWN);
		fileItem.setMenu(fileMenu);
		// File -> Open
		final MenuItem fileOpenItem = new MenuItem(fileMenu, SWT.PUSH);
		fileOpenItem.setText("&Open...\tCtrl+O");
		fileOpenItem.setAccelerator(SWT.CTRL + 'O');
		fileOpenItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(shell, SWT.OPEN);
				
				final String filename = dialog.open();
				
				if (filename != null) {
					File file = new File(filename);
					
					out(null);
					out("Opening file: " +file.getAbsolutePath());
					out("File size (bytes): " + file.length());
					
					shell.setText(title); // reset the title
					waveform.reset(); // reset the wave form
					try {
						AudioSample sample = new AudioSample(file);
						dumpSample(sample);
						waveform.setSample(sample);
						// TODO - Handle files that take a long time to process (busy indicator).
						// Or, better yet, spawn a background thread to process.
						shell.setText(title + " - " + file.getName()); // update the title
					} catch (UnsupportedAudioFileException e) {
						// Not all formats are supported yet (ex. 8-bit, a-law).
						System.out.println(e.getMessage()); 
						shell.setText(title + " - Error opening " + file.getName());
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}});
	}

	public static void main(String[] args) {
		new WaveformExample();
	}
	
	private static final void dumpSample(AudioSample sample) {
		out("Encoding: " + sample.getEncoding());
		out("Sample size in bits: " + sample.getBitsPerSample());
		out("Number of channels: " + sample.getNumberOfChannels());
		out("Sample rate: " + sample.getSampleRate());
		out("Framerate: " + sample.getFramerate());
		out("Framesize: " + sample.getFrameSize() + " byte(s)");
		out("Number of frames: " + sample.getNumberOfFrames() + " frames");
		out("Is big endian: " + sample.isBigEndian());
		out("Biggest Sample: " + sample.getBiggestSample());
		out("Duration: " + sample.getDuration() + " seconds");
	}
	
	private static final void out(final String message) {
		if(VERBOSE) {
			if(message == null) {
				out("----------------------------------------");
			} else {
				System.out.println("TRACE >> WaveformExample >> " + message);
			}
			
		}
	}

}
