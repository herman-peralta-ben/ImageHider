package com.connell.imagehider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.connell.imagehider.R;


public class InitialPasswordDialogFragment extends DialogFragment {
	
	public interface PasswordDialogListener {
		public void onDialogPositiveClick(InitialPasswordDialogFragment dialog);
	}
	
	private EditText mPasswordEditor1;
	private EditText mPasswordEditor2;
	private PasswordDialogListener mListener;
	private String mMessage;
	private boolean mCancelable;
	private AlertDialog mDialog;
	
	private MyTextWatcher watcher = new MyTextWatcher();
	
	InitialPasswordDialogFragment(String message, boolean cancelable, PasswordDialogListener listener) {
		super();
		mListener = listener;
		mMessage = message;
		mCancelable = cancelable;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		mPasswordEditor1 = new EditText(getActivity());
		mPasswordEditor1.setHint(R.string.password_hint);
		mPasswordEditor1.setInputType(InputType.TYPE_CLASS_TEXT |
				InputType.TYPE_TEXT_VARIATION_PASSWORD |
				InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		mPasswordEditor2 = new EditText(getActivity());
		mPasswordEditor2.setHint(R.string.reenter_password_hint);
		mPasswordEditor2.setInputType(InputType.TYPE_CLASS_TEXT |
				InputType.TYPE_TEXT_VARIATION_PASSWORD |
				InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		LinearLayout layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(mPasswordEditor1);
		layout.addView(mPasswordEditor2);
		
		builder.setMessage(mMessage);
		builder.setView(layout);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mListener.onDialogPositiveClick(InitialPasswordDialogFragment.this);
			}
		});
		
		if (mCancelable) {
			builder.setCancelable(false);
			builder.setNegativeButton(android.R.string.cancel, null);
		}
		
		mPasswordEditor1.addTextChangedListener(watcher);
		mPasswordEditor2.addTextChangedListener(watcher);
		
		mDialog = builder.create();
		return mDialog;
	}
	
	public String password() {
		return mPasswordEditor1.getText().toString();
	}
	
	private class MyTextWatcher implements TextWatcher {

		public void afterTextChanged(Editable arg0) { }

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) { }

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (mPasswordEditor1.getText().toString().equals(
					mPasswordEditor2.getText().toString())) {
				mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
			} else {
				mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
			}
		}
		
	}
	
}
