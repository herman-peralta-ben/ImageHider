package com.connell.imagehider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import com.connell.imagehider.R;


public class PasswordDialogFragment extends DialogFragment {
	
	public interface PasswordDialogListener {
		public void onDialogPositiveClick(PasswordDialogFragment dialog);
	}
	
	private EditText mPasswordEditor;
	private PasswordDialogListener mListener;
	private String mMessage;
	private boolean mCancelable;
	
	PasswordDialogFragment(String message, boolean cancelable, PasswordDialogListener listener) {
		super();
		mListener = listener;
		mMessage = message;
		mCancelable = cancelable;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		mPasswordEditor = new EditText(getActivity());
		mPasswordEditor.setHint(R.string.password_hint);
		mPasswordEditor.setInputType(InputType.TYPE_CLASS_TEXT |
				InputType.TYPE_TEXT_VARIATION_PASSWORD |
				InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		builder.setMessage(mMessage);
		builder.setView(mPasswordEditor);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mListener.onDialogPositiveClick(PasswordDialogFragment.this);
			}
		});
		if (mCancelable) {
			builder.setCancelable(false);
			builder.setNegativeButton(android.R.string.cancel, null);
		}
		
		return builder.create();
	}
	
	public String password() {
		return mPasswordEditor.getText().toString();
	}
}
