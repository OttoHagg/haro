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
import java.text.SimpleDateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * FileExplorer is a custom SWT widget that allows for easy implementation of
 * - you guessed it - a file explorer. Given it extends SashForm, it feels at
 * home anywhere a SashForm would. The user experience resembles Windows
 * Explorer, and is intended to be embedded in an application where a graphical
 * view of the filesystem is desired. Users can traverse directories and 
 * single-click or double-click files (via selection listeners).
 * 
 * The constructor (at the moment) accepts two important styles: SWT.HORIZONTAL
 * or SWT.VERTICAL depending on your needs. See FileExplorerSnippet.java for
 * this, and how to use the selection listeners.
 * 
 * This class is really the user interface. Most of the magic is found in class
 * SWTFileSystemView.java.
 * 
 * Important note: Before exiting your application, you must call
 * freeResources().
 */
public final class FileExplorer extends SashForm {

	private final SWTFileSystemView fileSystemView;
	private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
	private final Tree directoryTree;
	private final Table fileTable;
	private FileExplorerFileSelectionListener fefsl;
	private static final int NAME_COLUMN = 0;
	private static final int SIZE_COLUMN = 1;
	private static final int TYPE_COLUMN = 2;
	private static final int MODIFIED_COLUMN = 3;
	
	public FileExplorer(Composite parent, int style) {
		
		super(parent, style);
		this.fileSystemView = new SWTFileSystemView();
		this.fefsl = null;
		this.directoryTree = new Tree(this, SWT.SINGLE | SWT.BORDER);
		this.directoryTree.setLinesVisible(false);
		this.directoryTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.createDirectoryTree();
		
		this.directoryTree.addTreeListener(new TreeListener() {
				public void treeExpanded(TreeEvent event) {
				onDirectoryTreeExpanded((TreeItem) event.item);
			}
				
			public void treeCollapsed(TreeEvent event) {
				onDirectoryTreeCollapsed((TreeItem) event.item);
			}
		});
		
		this.directoryTree.addSelectionListener(new SelectionAdapter() {
			
			// A node in the tree is single-clicked...
			public void widgetSelected (SelectionEvent event) {
				try { // Wrapped this in a try/catch because of mysterious things on Mac OS X.
					TreeItem item = (TreeItem) event.item;
					createTableItems( (File) item.getData() ); 
				} catch (Exception e) {
					// Don't do anything. The user will just click again. Happens infrequently, thankfully.
				}
			}
			
			// A node in the tree is double-clicked
			public void widgetDefaultSelected(SelectionEvent event) {
				TreeItem item = (TreeItem) event.item;
				if (fileSystemView.isDirectory((File) item.getData())) {
						if (item.getExpanded()) {
							onDirectoryTreeCollapsed(item);
							item.setExpanded(false);
						} else {
							onDirectoryTreeExpanded(item);
							item.setExpanded(true);
						}
				}
			}

		});
		
		ViewForm vf = new ViewForm(this, SWT.NONE);
		/*
		 * TODO Implement a way to add a user-defined toolbar. For example...
		 * final ToolBar toolbar = new ToolBar(vf, SWT.HORIZONTAL | SWT.FLAT);
		 * ...
		 * vf.setTopLeft(toolbar);
		*/
		
		this.fileTable = new Table(vf, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		this.fileTable.setHeaderVisible(true);
		this.fileTable.setLinesVisible(false);

		TableColumn nameColumn = new TableColumn (this.fileTable, SWT.NONE);
		nameColumn.setText("Name");
		nameColumn.pack();
		nameColumn.setWidth(220);
		
		TableColumn sizeColumn = new TableColumn (this.fileTable, SWT.NONE);
		sizeColumn.setText("Size");
		sizeColumn.setAlignment(SWT.RIGHT);
		sizeColumn.pack();
		sizeColumn.setWidth(80);
		
		TableColumn typeColumn = new TableColumn (this.fileTable, SWT.NONE);
		typeColumn.setText("Type");
		typeColumn.setAlignment(SWT.LEFT);
		typeColumn.pack();
		typeColumn.setWidth(100);
		
		TableColumn modColumn = new TableColumn (this.fileTable, SWT.NONE);
		modColumn.setText("Date Modified");
		modColumn.setAlignment(SWT.LEFT);
		modColumn.pack();
		modColumn.setWidth(180);
		
		this.fileTable.setSize(this.fileTable.computeSize(SWT.DEFAULT, 600));
		
		this.fileTable.addSelectionListener (new SelectionAdapter () {
			
			// An item in the table is single-clicked...
			public void widgetSelected (SelectionEvent event) {
				TableItem item = (TableItem) event.item;
				File file = (File)item.getData();
				tableSingleSelected(file);
			}
			
			// An item in the table is double-clicked
			public void widgetDefaultSelected (SelectionEvent event) {
				TableItem item = (TableItem) event.item;
				File file = (File)item.getData();
				tableDoubleSelected(file);
			}
		});
		
		vf.setContent(this.fileTable);
	}
	
	/**
	 * Important!!!
	 * @see SWTFileSystemView.freeResources 
	 */
	public final void freeResources() {
		this.fileSystemView.freeResources();
	}
	
	public final void addFileSelectionListener(FileExplorerFileSelectionListener fsl) {
		this.fefsl = fsl;
	}
	
	public final void expandRootDirectory() {
		TreeItem item = this.directoryTree.getItem(0);
		if (item != null) {
			this.onDirectoryTreeExpanded(item);
			item.setExpanded(true);
		}
	}

	private final void tableSingleSelected(final File file) {
		
		if(this.fefsl != null) {
			/*
			 * TODO This is called even when the user double-clicks. Should there be
			 * some kind of internal delay/interrupt to ensure the user isn't
			 * double-clicking?
			 */
			this.fefsl.fileSelected(file);
		}
	}
	
	private final void tableDoubleSelected(final File file) {
		
		if(this.fefsl != null) {
			this.fefsl.fileActivated(file);
		}
	}
	
	private final void createDirectoryTree() {
		File[] roots = this.fileSystemView.getRoots();
		for (int i = 0; i < roots.length; i++) {
			this.addDirectoryTreeItem(null, roots[i]);
		}
	}
	
	private final void addDirectoryTreeItem(final TreeItem parentTreeItem, final File directory) {
		TreeItem item;
		if (parentTreeItem != null) {
			// It's got a parent TreeItem
			item = new TreeItem(parentTreeItem, SWT.NONE);
		} else {
			// It's a root TreeItem!
			item = new TreeItem(this.directoryTree, SWT.NONE);
		}
		
		item.setText(fileSystemView.getFileName(directory));
		item.setImage(fileSystemView.getIcon(this.getDisplay(), directory));
		item.setData(directory);
		/*
		 * Add some fake children. Windows Explorer behaves this way; I'm not sure it's good user experience
		 * and may do away with it.
		 */
		item.setItemCount(10);
	}
	
	private final void onDirectoryTreeExpanded(final TreeItem parentTreeItem) {
		// I forget why I'm checking for null here.
		if (parentTreeItem != null) {
			// Remove all child nodes
			parentTreeItem.removeAll();
			// Query for the array of children files
			File[] files = fileSystemView.getDirectories((File) parentTreeItem.getData());
			// Make sure there are children files
			if (files == null) return;
			// Add the children to the parent TreeItem
			for (int i = 0; i < files.length; i++) {
				if (files[i] != null) addDirectoryTreeItem(parentTreeItem, files[i]);
			}
		}
	}
	
	private final void onDirectoryTreeCollapsed(final TreeItem parentTreeItem) {
		if (parentTreeItem != null) {
			/*
			 * Not much to do here except show an image to represent "closed" state.
			 * Since we're relying on SWTFileSystemView to give us images,
			 * there's nothing to do (at least for now).
			 */
		}
	}

	private final void createTableItems(final File directory) {
		// Remove existing items
		this.fileTable.removeAll();
		// Query for the files listed for this directory
		File[] files = fileSystemView.getFiles(directory);
		// Add the items to the table (asserting that each child is a file).
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isDirectory()) {
				this.createTableItem(files[i]);
			}
		}
	}
	
	private final void createTableItem(final File file) {
		TableItem item = new TableItem(this.fileTable, SWT.NULL);
		item.setData(file);
		String name = fileSystemView.getFileName(file);
		item.setText(NAME_COLUMN, name);
		long bytesize = file.length();
		Long kbsize = new Long(bytesize/1024);
		if (kbsize < 1L) {
			kbsize = 1L;
		}
		
		item.setText(SIZE_COLUMN, kbsize.toString() + " KB");
		item.setText(TYPE_COLUMN, this.fileSystemView.getFileType(file));
		item.setText(MODIFIED_COLUMN, this.sdf.format(file.lastModified()));
		item.setImage(fileSystemView.getIcon(this.getDisplay(), file));
	}
}
