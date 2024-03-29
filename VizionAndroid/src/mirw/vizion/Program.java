package mirw.vizion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

public class Program {
    private static final int BYTES_PER_FLOAT = Float.SIZE / Byte.SIZE;
    private int mId;
    private VertexShader mVertexShader;
    private FragmentShader mFragmentShader;
    
    public Program(VertexShader vertexShader, FragmentShader fragmentShader) {
        mId = GLES20.glCreateProgram();
        Utils.checkErrors("glCreateProgram");
        setVertexShader(vertexShader);
        setFragmentShader(fragmentShader);
    }

    protected void finalize() {
        if (mId != 0) {
            GLES20.glDeleteProgram(mId);
        }
    }
    
    public void setVertexShader(VertexShader vertexShader) {
        mVertexShader = vertexShader;
        GLES20.glAttachShader(mId, vertexShader.getId());
        Utils.checkErrors("glAttachShader");
        link();
    }

    public void setFragmentShader(FragmentShader fragmentShader) {
        mFragmentShader = fragmentShader;
        GLES20.glAttachShader(mId, fragmentShader.getId());
        Utils.checkErrors("glAttachShader");
        link();
    }
    
    public void setUniform(String name, float x) {
        int oldId = pushProgram();
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mId, name), x);
        popProgram(oldId);
    }

    public void setUniform(String name, float x, float y) {
        int oldId = pushProgram();
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mId, name), x, y);
        popProgram(oldId);
    }

    public void setUniform(String name, float x, float y, float z) {
        int oldId = pushProgram();
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mId, name), x, y, z);
        popProgram(oldId);
    }

    public void setUniform(String name, float x, float y, float z, float w) {
        int oldId = pushProgram();
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mId, name), x, y, z, w);
        popProgram(oldId);
    }

    public void setUniform(String name, int value) {
        int oldId = pushProgram();
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mId, name), value);
        Utils.checkErrors("glUniform1i");
        popProgram(oldId);
    }

    public void setVertexAttrib(String name, float[] values, int valueSize) {
        int oldId = pushProgram();
        int handle = GLES20.glGetAttribLocation(mId, name);
        Utils.checkErrors("glGetAttribLocation");
        GLES20.glVertexAttribPointer(handle, valueSize, GLES20.GL_FLOAT, false, valueSize * BYTES_PER_FLOAT, wrap(values));
        Utils.checkErrors("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(handle);
        Utils.checkErrors("glEnableVertexAttribArray");
        popProgram(oldId);
    }
    
    public void use() {
        GLES20.glUseProgram(mId);
    }

    public int getId() {
        return mId;
    }
    
    private void link() {
        if ((mVertexShader != null) && (mFragmentShader != null)) {
            GLES20.glLinkProgram(mId);
            Utils.checkErrors("glLinkProgram");
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mId, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                String programInfoLog = GLES20.glGetProgramInfoLog(mId);
                throw new IllegalArgumentException(programInfoLog);
            }
        }
    }
 
    private int pushProgram() {
        int[] oldIds = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, oldIds, 0);
        Utils.checkErrors("glGetIntegerv");
        GLES20.glUseProgram(mId);
        Utils.checkErrors("glUseProgram");
        return oldIds[0];
    }
    
    private void popProgram(int oldId) {
        GLES20.glUseProgram(oldId);        
        Utils.checkErrors("glUseProgram");
    }
    
    private static FloatBuffer wrap(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer().put(data);
        buffer.position(0);
        return buffer;
    }
}