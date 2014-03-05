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

import net.bsuojanen.swt.widgets.filesystem.FileExplorer;
import net.bsuojanen.swt.widgets.filesystem.FileExplorerFileSelectionListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Example usage of FileExplorer.
 * 
 */
public final class FileExplorerSnippet {
	
	private final Shell shell;
	private final FileExplorer explorer;

	public FileExplorerSnippet() {
		final Display display = new Display();
		this.shell = new Shell(display);
		this.shell.setText("FileExplorer Example");
		
		final GridLayout shellLayout = new GridLayout();
		shellLayout.numColumns = 1;
		shellLayout.marginHeight = 2;
		shellLayout.marginWidth = 2;
		shellLayout.marginBottom = 2;
		shellLayout.marginLeft = 2;
		shellLayout.marginRight = 2;
		shellLayout.marginTop = 2;
		shellLayout.makeColumnsEqualWidth = true;
		this.shell.setLayout(shellLayout);
		
		this.explorer = new FileExplorer(this.shell, SWT.HORIZONTAL);
		this.explorer.setLayoutData(new GridData(GridData.FILL_BOTH));
		explorer.setWeights(new int[] { 4, 9 });
		explorer.addFileSelectionListener(new FileExplorerFileSelectionListener() {
			public void fileSelected(File file) {
				System.out.println("File single-clicked: " + file.getAbsolutePath());
			}

			@Override
			public void fileActivated(File file) {
				System.out.println("File double-clicked: " + file.getAbsolutePath());
			}
		});
		
		this.explorer.expandRootDirectory();
		
		
		this.shell.pack();
		this.shell.setMaximized(true);
		this.shell.open();
		
		while (! this.shell.isDisposed()) {
			if (! display.readAndDispatch()) display.sleep();
		}
		
		/*
		 * IMPORTANT!!!
		 */
		this.explorer.freeResources();
		
		display.dispose();
	}

	public static void main(String[] args) {
		new FileExplorerSnippet();
	}

}
