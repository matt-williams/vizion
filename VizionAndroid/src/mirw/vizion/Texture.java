package mirw.vizion;

import java.nio.ByteBuffer;

import android.opengl.GLES20;

public class Texture {
    private int mId;
    private int mFramebufferId;
    private int mWidth;
    private int mHeight;
    private boolean mResizePending;
    private boolean mFramebufferResizePending;

    public Texture(int width, int height) {
        mId = generateTextureId();
        mFramebufferId = generateFramebufferId();        
        int oldId = pushTexture();
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        popTexture(oldId);
        oldId = pushFramebuffer();
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mId, 0);
        popFramebuffer(oldId);
        setSize(width, height);
    }

    protected void finalize() {
        if (mId != 0) {
            GLES20.glDeleteTextures(1, new int[] {mId}, 0);
        }
        if (mFramebufferId != 0) {
            GLES20.glDeleteTextures(1, new int[] {mFramebufferId}, 0);
        }
    }
    
    public void setSize(int width, int height) {
        if ((mWidth != width) || (mHeight != height)) {
            mWidth = width;
            mHeight = height;
            mResizePending = true;            
            mFramebufferResizePending = true;
        }
    }
    
    public void use() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mId);
    }
    
    public void renderTo() {
        if (mFramebufferResizePending) {
            int oldId = pushTexture();
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.allocate(mWidth * mHeight * 4));
            Utils.checkErrors("glTexImage2D");
            popTexture(oldId);
            mFramebufferResizePending = false;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferId);
        Utils.checkErrors("glBindFramebuffer");
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) returned " + status);
        }
    }

    public static void renderToScreen() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    
    public void setData(byte[] data) {
        int oldId = pushTexture();
        if (mResizePending) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mWidth, mHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(data, 0, mWidth * mHeight));
            Utils.checkErrors("glTexImage2D");
            mResizePending = false;
        } else {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mWidth, mHeight, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(data, 0, mWidth * mHeight));            
            Utils.checkErrors("glTexSubImage2D");
        }
        popTexture(oldId);
    }

    public int getId() {
        return mId;
    }
    
    public int getFramebufferId() {
        return mFramebufferId;
    }
    
    private static int generateTextureId() {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        Utils.checkErrors("glGenTextures");
        return ids[0];
    }
    
    private static int generateFramebufferId() {
        int[] ids = new int[1];
        GLES20.glGenFramebuffers(1, ids, 0);
        Utils.checkErrors("glFramebuffers");
        return ids[0];
    }

    private int pushTexture() {
        int[] oldIds = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, oldIds, 0);
        Utils.checkErrors("glGetIntegerv");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mId);
        Utils.checkErrors("glBindTexture");
        return oldIds[0];
    }
    
    private void popTexture(int oldId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, oldId);        
        Utils.checkErrors("glBindTexture");
    }
    
    private int pushFramebuffer() {
        int[] oldIds = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, oldIds, 0);
        Utils.checkErrors("glGetIntegerv");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferId);
        Utils.checkErrors("glBindFramebuffer");
        return oldIds[0];
    }
    
    private void popFramebuffer(int oldId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, oldId);        
        Utils.checkErrors("glBindFramebuffer");
    }
}