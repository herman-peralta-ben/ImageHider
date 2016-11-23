package com.connell.imagehider;

import android.app.ActionBar;
import android.app.Activity;	
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

public class MainActivity extends Activity {

	static final int ACTIVITY_SELECT_IMAGE = 0;
	
	static final String TAG = "ImageHider";
	
	SharedPreferences mPreferences;
	
	static final String PASSWORD_HASH_PREF = "passwordhash";
	
	private AESImageAdapter mImageAdapter;
	private GridView mAdapterView;
	
	private final int PASSWORD_LENGTH = 16;
	
	static int IMAGE_PREVIEW_HEIGHT_DP = 300;
	static int IMAGE_PREVIEW_WIDTH_DP = 300;
	
	int mImagePreviewHeightPixels;
	int mImagePreviewWidthPixels;
	
	private byte[] mPasswordBytes = {0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0}; // the default password is sixteen 0 bytes
	private String mStoredPasswordHash;
	
	private Handler mHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		ActionBar actionBar = getActionBar();
		if (actionBar == null)
			throw new NullPointerException("Action bar is null");
		
		mPreferences = getSharedPreferences("ImageHider", MODE_PRIVATE);
		
		mImagePreviewHeightPixels = (int) (IMAGE_PREVIEW_HEIGHT_DP * getResources().getDisplayMetrics().density + 0.5f);
		mImagePreviewWidthPixels = (int) (IMAGE_PREVIEW_WIDTH_DP * getResources().getDisplayMetrics().density + 0.5f);
		
		mAdapterView = new GridView(this);
		mAdapterView.setColumnWidth(mImagePreviewWidthPixels);
		mAdapterView.setNumColumns(-1);
		mAdapterView.setOnItemClickListener(new ImagePreviewClickListener());
		mAdapterView.setVerticalSpacing(10);
		mAdapterView.setHorizontalSpacing(10);
		mAdapterView.setGravity(Gravity.FILL);
		
		mHandler = new Handler();

		mImageAdapter = new AESImageAdapter(this, mPasswordBytes, mHandler,
				mImagePreviewHeightPixels, mImagePreviewWidthPixels);		
		mAdapterView.setAdapter(mImageAdapter);

		setContentView(mAdapterView);
		
		if (!mPreferences.contains(PASSWORD_HASH_PREF)) {
			initialPasswordDialog();
		} else {
			mStoredPasswordHash = mPreferences.getString(PASSWORD_HASH_PREF, null);
			passwordDialog();
		}

		Toast.makeText(this, "Dar permisos en opciones", Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	private void passwordDialog() {
		PasswordDialogFragment passwordDialog = new PasswordDialogFragment(getString(R.string.password_instruction), false, new PasswordDialogFragment.PasswordDialogListener() {
			public void onDialogPositiveClick(PasswordDialogFragment dialog) {
				dialog.dismiss();
				boolean passwordCorrect = false;
				try {
					passwordCorrect = Password.check(dialog.password(), mStoredPasswordHash);
				} catch (Exception e) {}
				if (passwordCorrect) {
					mImageAdapter = new AESImageAdapter(MainActivity.this, mPasswordBytes, new Handler(),
							mImagePreviewHeightPixels, mImagePreviewWidthPixels);
					mAdapterView.setAdapter(mImageAdapter);
				} else {
					retryPasswordDialog();
				}
				mPasswordBytes = formatPassword(dialog.password());
				mImageAdapter = new AESImageAdapter(MainActivity.this, mPasswordBytes, new Handler(),
						mImagePreviewHeightPixels, mImagePreviewWidthPixels);
				mAdapterView.setAdapter(mImageAdapter);
			}
		});
		passwordDialog.show(getFragmentManager(), "PasswordDialogFragment");
	}
	
	private void retryPasswordDialog() {
		PasswordDialogFragment passwordDialog = new PasswordDialogFragment(getString(R.string.retry_password_instruction), false, new PasswordDialogFragment.PasswordDialogListener() {
			public void onDialogPositiveClick(PasswordDialogFragment dialog) {
				dialog.dismiss();
				boolean passwordCorrect = false;
				try {
					passwordCorrect = Password.check(dialog.password(), mStoredPasswordHash);
				} catch (Exception e) {}
				if (passwordCorrect) {
					mImageAdapter = new AESImageAdapter(MainActivity.this, mPasswordBytes, new Handler(),
							mImagePreviewHeightPixels, mImagePreviewWidthPixels);
					mAdapterView.setAdapter(mImageAdapter);
				} else {
					retryPasswordDialog();
				}
				mPasswordBytes = formatPassword(dialog.password());
				mImageAdapter = new AESImageAdapter(MainActivity.this, mPasswordBytes, new Handler(),
						mImagePreviewHeightPixels, mImagePreviewWidthPixels);
				mAdapterView.setAdapter(mImageAdapter);
			}
		});
		passwordDialog.show(getFragmentManager(), "PasswordDialogFragment");
	}
	
	private void initialPasswordDialog() {
		InitialPasswordDialogFragment passwordDialog = new InitialPasswordDialogFragment(getString(R.string.initial_password_instruction), false, new InitialPasswordDialogFragment.PasswordDialogListener() {
			public void onDialogPositiveClick(InitialPasswordDialogFragment dialog) {
				dialog.dismiss();
				String saltedHash;
				try {
					saltedHash = Password.getSaltedHash(dialog.password());
				} catch (Exception e) {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setMessage(R.string.hashing_error);
					builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							MainActivity.this.finish();
						}
					});
					builder.create().show();
					return;
				}
				mPreferences.edit().putString(PASSWORD_HASH_PREF, saltedHash).commit();
				mPasswordBytes = formatPassword(dialog.password());
				mImageAdapter = new AESImageAdapter(MainActivity.this, mPasswordBytes, new Handler(),
						mImagePreviewHeightPixels, mImagePreviewWidthPixels);
				mAdapterView.setAdapter(mImageAdapter);
			}
		});
		passwordDialog.show(getFragmentManager(), "PasswordDialogFragment");
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Open Android's image chooser activity, to allow
		// the user to choose an image
		if (item.getItemId() == R.id.add) {
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
			return true;
		} else if (item.getItemId() == R.id.quit) {
			finish();
			return true;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPasswordBytes = null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

		switch(requestCode) { 
		// the user has chosen an image to encrypt
		case ACTIVITY_SELECT_IMAGE:
		    if(resultCode == RESULT_OK){  
		        Uri selectedImage = imageReturnedIntent.getData();
		        String[] filePathColumn = {MediaStore.Images.Media.DATA};

		        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
		        cursor.moveToFirst();

		        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		        final String filePath = cursor.getString(columnIndex);
		        cursor.close();

		        final String newFilePath = AESManager.encryptedFolderString() + filePath;

                Log.i("Herman", "filePath: " + filePath);
                Log.i("Herman", "newFilePath: " + newFilePath);

		        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setMessage(String.format(getString(R.string.confirm_encrypt), filePath));
		        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						AESManager.encrypt(mPasswordBytes, filePath, newFilePath, MainActivity.this, null);
					}
				});
				builder.setNegativeButton(android.R.string.no, null);
				builder.create().show();
		    }
		}
	}
	
	class ImagePreviewClickListener implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			String filename = (String)mImageAdapter.getItem(position);
			Intent imageIntent = new Intent(MainActivity.this, ImageActivity.class);
			imageIntent.setAction(Intent.ACTION_VIEW);
			imageIntent.putExtra(ImageActivity.BUNDLE_FILENAME, filename);
			imageIntent.putExtra(ImageActivity.BUNDLE_PASSWORD, mPasswordBytes);
			startActivity(imageIntent);
		}
	}

	public byte[] formatPassword(String password) {
		byte[] passwordBytes = password.getBytes();
		byte[] newPassword = new byte[16];
		if (passwordBytes.length > PASSWORD_LENGTH) {
			for (int i = 0; i < PASSWORD_LENGTH; i++) {
				newPassword[i] = passwordBytes[i];
			}
		} else {
			for (int i = 0; i < passwordBytes.length; i++) {
				newPassword[i] = passwordBytes[i];
			}
			for (int i = passwordBytes.length; i < PASSWORD_LENGTH; i++) {
				newPassword[i] = 0;
			}
		}
		return newPassword;
	}

}

