package com.connell.imagehider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import com.google.common.collect.ImmutableList;

public class AESManager {
	
	public static final String ACTION_ENCRYPTED_FILES_CHANGED = "com.connell.imagehider.aesmanager.fileschanged";

	public static File encryptedFolder() {
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ImageHider/";
		File file = new File(path);
		return file;
	}
	
	public static String encryptedFolderString() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/ImageHider/";
	}
	
	public static boolean createEncryptedFolder() {
		File folder = encryptedFolder();
		if (!folder.exists() && !folder.isFile()) {
			if (!folder.mkdirs())
				return false;
		}
		File nomedia = new File(folder.getAbsolutePath() + "/.nomedia");
		try {
			if (!nomedia.exists() && !nomedia.createNewFile())
				return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static boolean createLeadingFolders(String path) {
		return createLeadingFoldersHelper("", path);
	}
	
	private static boolean createLeadingFoldersHelper(String gen, String path) {
		String folder = path.substring(0, path.indexOf(File.separator)+1);
		if (folder.isEmpty())
			return true;
		File file = new File(gen + folder);
		if (file.exists() && !file.isDirectory()) {
			return false;
		} else if (file.exists() && file.isDirectory()) {
			return createLeadingFoldersHelper(gen + folder, path.substring(path.indexOf(File.separator)+1, path.length()));
		} else if (file.mkdir()) {
			return createLeadingFoldersHelper(gen + folder, path.substring(path.indexOf(File.separator)+1, path.length()));
		} else {
			return false;
		}
	}
	
	public static ImmutableList<File> listFiles() {
		File root = encryptedFolder();
		return listFilesHelper(root);
	}
	
	private static ImmutableList<File> listFilesHelper(File root) {
		ArrayList<File> results = new ArrayList<File>();
		File[] contents = root.listFiles();
		for (File f : contents) {
			if (f.isDirectory()) {
				results.addAll(listFilesHelper(f));
			} else if (f.isFile() && !f.getName().equals(".nomedia")) {
				results.add(f);
			}
		}
		return ImmutableList.copyOf(results);
	}
	
	/**
	 * Asynchronously encrypt the chosen file.
	 * @param keyString Key to encrypt with
	 * @param filename Name of file to encrypt
	 * @param newFilename Name to save newly encrypted file
	 * @param dialog An optional progress dialog to update with progress
	 * @param adapter An optional adapter to notify when the encryption is finished
	 * @param context Must supply context
	 * @return
	 */
	public static void encrypt (final byte[] key,
			final String filename,
			final String newFilename,
			final Context context,
			final EncryptionActionListener listener) {
		
		final Handler uiHandler = new Handler();
		final ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setMessage(context.getString(R.string.encrypt_progress_message));
		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMax(100);
		progressDialog.setProgress(0);
		progressDialog.show();
		
		Thread task = new Thread("Encrypt thread") {
			public void run() {
				boolean deleteSuccess = true;
				boolean outOfMem = false;
				boolean exception = false;
				
				try {
					File file = new File(filename);
					int length = (int)file.length();
					
					byte fileBytes[] = new byte[1024*100];
					
					if (!createLeadingFolders(newFilename)) {
						throw new IOException("Could not create folders in path " + newFilename);
					}
					
					InputStream is = new FileInputStream(filename);
					OutputStream os = new FileOutputStream(newFilename);	
					AESOutputStream aos = new AESOutputStream(os, key, length);
					
					int totalRead = 0;
					int partialRead = 0;
					while (true) {
						partialRead = is.read(fileBytes);
						if (partialRead == -1) {
							break;
						} else {
							totalRead += partialRead;
							aos.write(fileBytes, 0, partialRead);
							final int percentage = (totalRead * 100) / length;
							uiHandler.post(new Runnable() {
								public void run() {
									progressDialog.setProgress(percentage);
								}
							});
						}
					}
					
					is.close();
					aos.close();
					os.close();
					
					deleteSuccess = file.delete();
					
					AESImageAdapter.delete(context, filename);
				    
				    if (listener != null)
				    	listener.onActionComplete();
				    
				    Intent i = new Intent(ACTION_ENCRYPTED_FILES_CHANGED);
				    context.sendBroadcast(i);
			
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
					outOfMem = true;
				} catch (Exception e) {
					e.printStackTrace();
					exception = true;
				} finally {
					progressDialog.dismiss();
					if (exception) {
						errorDialog(uiHandler, context.getString(R.string.encrypt_error), context);
					} else if (outOfMem) {
						errorDialog(uiHandler, context.getString(R.string.memory_error), context);
					} else if (!deleteSuccess) {
						errorDialog(uiHandler, context.getString(R.string.encrypt_delete_error), context);
					}
				}
			}
		};
		
		task.start();
	}
	

	/**
	 * Asynchronously decrypt the chosen file.
	 * @param keyString Key to decrypt with
	 * @param filename Name of file to decrypt
	 * @param newFilename Name to save newly decrypted file
	 * @param dialog An optional progress dialog to update with progress
	 * @param adapter An optional adapter to notify when the encryption is finished
	 * @param context Must supply context
	 * @return
	 */
	public static void decrypt (final byte[] key,
			final String filename,
			final String newFilename,
			final Context context,
			final EncryptionActionListener listener) {
		
		final Handler uiHandler = new Handler();
		final ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setMessage(context.getString(R.string.decrypt_progress_message));
		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMax(100);
		progressDialog.setProgress(0);
		progressDialog.show();
		
		Thread task = new Thread("Decrypt thread") {
			public void run() {
				boolean deleteSuccess = true;
				boolean outOfMem = false;
				boolean exception = false;
				
				try {
					File file = new File(filename);
					int length = (int)file.length();
					
					byte fileBytes[] = new byte[1024*100];
					
					if (!createLeadingFolders(newFilename)) {
						throw new IOException("Could not create folders in path " + newFilename);
					}
					
					InputStream is = new FileInputStream(filename);
					OutputStream os = new FileOutputStream(newFilename);	
					AESInputStream ais = new AESInputStream(is, key);
					
					int totalRead = 0;
					int partialRead = 0;
					while (true) {
						partialRead = ais.read(fileBytes);
						if (partialRead == -1) {
							break;
						} else {
							totalRead += partialRead;
							os.write(fileBytes, 0, partialRead);
							final int percentage = (totalRead * 100) / length;
							uiHandler.post(new Runnable() {
								public void run() {
									progressDialog.setProgress(percentage);
								}
							});
						}
					}
					
					ais.close();
					is.close();
					os.close();
					
					deleteSuccess = file.delete();
					
			        // Run the media scanner to discover the decrypted file exists
				    MediaScannerConnection.scanFile(context,
				    		new String[] { newFilename },
				    		null,
				    		null);
				    
				    if (listener != null)
				    	listener.onActionComplete();
				    
				    Intent i = new Intent(ACTION_ENCRYPTED_FILES_CHANGED);
				    context.sendBroadcast(i);
			
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
					outOfMem = true;
				} catch (Exception e) {
					e.printStackTrace();
					exception = true;
				} finally {
					progressDialog.dismiss();
					if (exception) {
						errorDialog(uiHandler, context.getString(R.string.decrypt_error), context);
					} else if (outOfMem) {
						errorDialog(uiHandler, context.getString(R.string.memory_error), context);
					} else if (!deleteSuccess) {
						errorDialog(uiHandler, context.getString(R.string.decrypt_delete_error), context);
					}
				}
			}
		};
		
		task.start();
	}
	
	static void errorDialog(Handler handler, final String message, final Context context) {
		handler.post(new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(message);
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
			}
		});
	}
	
	public interface EncryptionActionListener {
		public void onActionComplete();
	}
}
