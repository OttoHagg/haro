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
package net.bsuojanen.swt.widgets.filesystem;

import java.io.File;

/**
 * The selection listener for the file explorer. I needed only two
 * methods: single-click and double-click.
 *
 */
public abstract class FileExplorerFileSelectionListener {
	
	/**
	 * Handle single-click of file.
	 * @param file
	 */
	public abstract void fileSelected(final File file);
	
	/**
	 * Handle double-click of file.
	 * @param file
	 */
	public abstract void fileActivated(final File file);
	
}
