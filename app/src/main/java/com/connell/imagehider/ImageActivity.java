package com.connell.imagehider;

import java.io.FileInputStream;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

public class ImageActivity extends Activity {
	
	public static final String BUNDLE_FILENAME = "filename";
	public static final String BUNDLE_PASSWORD = "password";
	
	String mFilename;
	byte[] mPasswordBytes;
	AsyncTask<String, Integer, Bitmap> mLoader;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		ActionBar actionBar = getActionBar();
		if (actionBar == null)
			throw new NullPointerException("Action bar is null");
        
        mFilename = getIntent().getExtras().getString(BUNDLE_FILENAME);
        mPasswordBytes = getIntent().getExtras().getByteArray(BUNDLE_PASSWORD);
        
        View loadingView = new ImageErrorView(this, getString(R.string.loading), mFilename);
        
        setContentView(loadingView);
        
        mLoader = new LoadAESImage().executeOnExecutor(LoadAESImage.THREAD_POOL_EXECUTOR, mFilename);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_activity, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Decrypt the image
		if (item.getItemId() == R.id.remove) {
			decryptCurrentImage();
		}
		return false;
	}

	private void decryptCurrentImage() {        
        // Do all the decryption.
        // This will read from the encrypted file, and write a new decrypted file
        // in the same directory.
        final String newFilePath = mFilename.substring((mFilename.lastIndexOf(AESManager.encryptedFolderString())-1) +
        		AESManager.encryptedFolderString().length(), mFilename.length());
        final Handler uiHandler = new Handler();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getString(R.string.confirm_decrypt), mFilename));
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				mLoader.cancel(true);
				AESManager.decrypt(mPasswordBytes, mFilename, newFilePath, ImageActivity.this,
						new AESManager.EncryptionActionListener() {
							public void onActionComplete() {
								uiHandler.post(new Runnable() {
									public void run() {
										ImageActivity.this.finish();
									}
								});
							}
						});
			}
		});
		builder.setNegativeButton(android.R.string.no, null);
		builder.create().show();
	}
    
    private class LoadAESImage extends AsyncTask<String, Integer, Bitmap> {
		@Override
		protected Bitmap doInBackground(String...imageFile) {
			Bitmap b;
			
			try {
				AESInputStream is = new AESInputStream(new FileInputStream(imageFile[0]), mPasswordBytes);

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				
				b = BitmapFactory.decodeStream(is, null, options);

				is.close();
				
				return b;

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				return null;
			}
			
		}
		
		protected void onPostExecute(Bitmap b) {
			ImageView imageView = new ImageView(ImageActivity.this);
			imageView.setImageBitmap(b);
			ImageActivity.this.setContentView(imageView);			
		}
	}
}
