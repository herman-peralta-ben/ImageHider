package com.connell.imagehider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESInputStream extends InputStream {

	private byte[] IV = new byte[16];
	private int contentSize;
	private CipherInputStream cis;
	
	public AESInputStream(InputStream is, byte[] k) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		super();
		
		// read the first four bytes as an integer, which is the image size
		byte[] size = new byte[4];
		ByteBuffer sizeByteBuffer = ByteBuffer.allocate(4);
		is.read(size);
		sizeByteBuffer.put(size);
		contentSize = sizeByteBuffer.getInt(0);
		
		// read the next 16 bytes as the initialisation vector
		is.read(IV);
		
		Cipher c;
		c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec keySpec = new SecretKeySpec(k, "AES");
		c.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(IV));
		cis = new CipherInputStream(is, c);

	}
	
	public byte[] getIV() {
		return IV;
	}
	
	public int getContentSize() {
		return contentSize;
	}
	
	@Override
	public int read() throws IOException {
		return cis.read();
	}
	
	@Override
	public int read(byte[] a) throws IOException {
		return cis.read(a);
	}
	
	@Override
	public void close() throws IOException {
		cis.close();
	}

}
