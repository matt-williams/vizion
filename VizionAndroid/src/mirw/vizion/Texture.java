package mirw.vizion;

import java.nio.ByteBuffer;

import android.opengl.GLES20;

public class Texture {
    private int mId;
    private int mWidth;
    private int mHeight;
    private boolean mResizePending;

    public Texture(int width, int height) {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        mId = ids[0];
        use();
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        setSize(width, height);
    }

    protected void finalize() {
        if (mId != 0) {
            GLES20.glDeleteTextures(1, new int[] {mId}, 0);
        }
    }
    
    public void setSize(int width, int height) {
        if ((mWidth != width) || (mHeight != height)) {
            mWidth = width;
            mHeight = height;
            mResizePending = true;            
        }
    }
    
    public void use() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mId);
    }
    
    public void setData(byte[] data) {
        use();
        if (mResizePending) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mWidth, mHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(data, 0, mWidth * mHeight));
            mResizePending = false;
        } else {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mWidth, mHeight, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(data, 0, mWidth * mHeight));            
        }
    }

    public int getId() {
        return mId;
    }
}