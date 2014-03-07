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
					shell.setText(title + " - " + file.getName());
					try {
						AudioSample sample = new AudioSample(file);
						waveform.setSample(sample);						
					} catch (UnsupportedAudioFileException e) {
						// AudioSample currently supports audio sampled at 16 bits. Lofi stuff, at 8 bits,
						// shows up. But 24 is ubiquitous these days.
						System.out.println(e.getMessage()); 
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}});
	}

	public static void main(String[] args) {
		new WaveformExample();
	}

}
