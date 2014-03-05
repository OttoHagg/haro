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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileSystemView;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

/**
 * SWTFileSystemView is responsible for querying the filesystem, and
 * balances compatibility with user-experience on each OS. Towards
 * this goal, the most important dependency here is FileSystemView
 * from the javax.swing.filechooser package.
 * 
 * This class is also responsible for converting image formats and,
 * admittedly, the look-and-feel is most "native" on Windows as other
 * platforms are emulated by Swing.
 * 
 * Lastly, this was hastily written as I was focusing
 * energy on other parts of a larger application. While this works
 * well, functionally, there is plenty of room for improvement.
 *
 */
public final class SWTFileSystemView {

	private static final FileSystemView view = FileSystemView.getFileSystemView();
	private static final PaletteData PALETTE_DATA = new PaletteData(0xFF0000, 0xFF00, 0xFF);
	
	/**
	 * A map of file extensions to icons.
	 */
	private Hashtable<String, Image> fileIconCache;
	
	/**
	 * Windows has special/virtual directories with unique icons. For
	 * other platforms the icon is the same (1) for all directories (which
	 * makes this inefficient/dumb).
	 * 
	 * (1) The LnF for Mac OS X and *nix is emulated by Swing. Yes, there
	 * are special icons for some directories on these platforms but we
	 * don't have access to these via FileSystemView.
	 * 
	 * TODO - Improve this for non-Windows platforms. Or, just get rid of it.
	 */
	private Hashtable<File, Image> directoryIconCache;
	
	public SWTFileSystemView() {
		this.fileIconCache = new Hashtable<String, Image>();
		this.directoryIconCache = new Hashtable<File, Image>();
	}
	
	/**
	 * @return
	 */
	public final File[] getRoots() {
		return view.getRoots();
	}
	
	/**
	 * @param file
	 * @return
	 */
	public final File[] getDirectories(final File file) {
		File[] files = view.getFiles(file, true); // Set to true to view hidden directories.
		File[] dirs = new File[files.length];
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				dirs[i] = files[i];
			} else {
				dirs[i] = null;
			}
		}
		return dirs;
	}
	
	public final File[] getFiles(final File file) {
		return file.listFiles();
	}
	
	public final String getFileName(final File file) {
		return view.getSystemDisplayName(file);
	}
	
	public final Image getIcon(final Display display, final File file) {
		Image icon = null;
		if(view.isTraversable(file)) {
			icon = this.getDirectoryIcon(display, file);
		} else {
			icon = this.getFileIcon(display, file);
		}
		return icon;
	}
	
	private final Image getFileIcon(final Display display, final File file) {
		// TODO This won't work for executables with icons associated with them.
		String ext = this.getFileExtension(file.getAbsolutePath()).toLowerCase();
		Image icon = (Image) this.fileIconCache.get(ext);
		if(icon == null) {
			javax.swing.Icon swingIcon = view.getSystemIcon(file);
			java.awt.Image awtImage = iconToImage(swingIcon);
			icon = this.convert(display, (BufferedImage) awtImage);
			this.fileIconCache.put(ext, icon);
		}
		
		return icon;
	}
	
	private final Image getDirectoryIcon(final Display display, final File file) {
		Image icon = (Image) this.directoryIconCache.get(file);
		if(icon == null) {
			javax.swing.Icon swingIcon = view.getSystemIcon(file);
			java.awt.Image awtImage = iconToImage(swingIcon);
			icon = this.convert(display, (BufferedImage) awtImage);
			this.directoryIconCache.put(file, icon);
		}
		return icon;
	}
	
	public final String getFileExtension(String filename) {
		String extension = "";
		int index = filename.lastIndexOf('.');
		if (index != -1) {
			extension = filename.substring(index + 1);
		}
		return extension;
	}
	
	public final String getFileType(final File file) {
		// TODO Improve this. For example, on Mac and Linux, binaries do not have a file extension.
		return this.getFileExtension(file.getName());
	}
	
	/**
	 * It is very, very important this method is called before exiting. Images in SWT
	 * must be explicitly "disposed".
	 */
	public final void freeResources() {
		if (this.fileIconCache != null) {
			for (Enumeration<Image> it = this.fileIconCache.elements(); it.hasMoreElements();) {
				Image image = (Image) it.nextElement();
				image.dispose();
			}
		}
		
		if (this.directoryIconCache != null) {
			for (Enumeration<Image> it = this.directoryIconCache.elements(); it.hasMoreElements();) {
				Image image = (Image) it.nextElement();
				image.dispose();
			}
		}
	}
	
	public final boolean isDirectory(final File file) {
		return view.isTraversable(file);
	}
	
	/**
	 * Converts an javax.swing.Icon to an java.awt.Image.
	 * 
	 * @param icon
	 * @return
	 */
	private static final java.awt.Image iconToImage(final javax.swing.Icon icon) {
		if (icon instanceof javax.swing.ImageIcon) {
			// Windows falls into here.
			return ((javax.swing.ImageIcon) icon).getImage();
		} else {
			// Mac OS and Linux (and probably most other platforms) fall here because LnF is emulated.
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			icon.paintIcon(null, g, 0, 0);
			g.dispose();
			return image;
		}
	}
	
	/**
	 * Converts an java.awt.Image to an org.eclipse.swt.graphics.Image.
	 * 
	 * @param display
	 * @param bufferedImage
	 * @return
	 */
	private final Image convert(final Display display, final BufferedImage bufferedImage) {
		ImageData imageData = new ImageData(bufferedImage.getWidth(),bufferedImage.getHeight(), 24, PALETTE_DATA);
		int scansize = (((bufferedImage.getWidth() * 3) + 3) * 4) / 4; // 32 bit alignment
		WritableRaster alphaRaster = bufferedImage.getAlphaRaster();
		byte[] alphaBytes = new byte[bufferedImage.getWidth()];
		for (int y = 0; y < bufferedImage.getHeight(); y++) {
			int[] buff = bufferedImage.getRGB(0, y, bufferedImage.getWidth(), 1, null, 0, scansize);
			imageData.setPixels(0, y, bufferedImage.getWidth(), buff, 0);
			if (alphaRaster != null) {
				int[] alpha = alphaRaster.getPixels(0, y, bufferedImage.getWidth(), 1, (int[]) null);
				for (int i = 0; i < bufferedImage.getWidth(); i++) {
					alphaBytes[i] = (byte) alpha[i];
				}
				imageData.setAlphas(0, y, bufferedImage.getWidth(), alphaBytes, 0);
			}
		}
		return new Image(display, imageData);
	}
}
