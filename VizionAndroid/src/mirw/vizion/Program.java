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
        link();
    }

    public void setFragmentShader(FragmentShader fragmentShader) {
        mFragmentShader = fragmentShader;
        GLES20.glAttachShader(mId, fragmentShader.getId());
        link();
    }
    
    public void setUniform(String name, float x) {
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mId, name), x);
    }

    public void setUniform(String name, float x, float y) {
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mId, name), x, y);
    }

    public void setUniform(String name, float x, float y, float z) {
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mId, name), x, y, z);
    }

    public void setUniform(String name, float x, float y, float z, float w) {
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mId, name), x, y, z, w);
    }

    public void setUniform(String name, int value) {
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mId, name), value);
    }

    public void setVertexAttrib(String name, float[] values, int valueSize) {
        int handle = GLES20.glGetAttribLocation(mId, name);
        GLES20.glVertexAttribPointer(handle, valueSize, GLES20.GL_FLOAT, false, valueSize * BYTES_PER_FLOAT, wrap(values));
        GLES20.glEnableVertexAttribArray(handle);
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
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mId, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                String programInfoLog = GLES20.glGetProgramInfoLog(mId);
                throw new IllegalArgumentException(programInfoLog);
            }
        }
    }
    
    private static FloatBuffer wrap(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer().put(data);
        buffer.position(0);
        return buffer;
    }
}