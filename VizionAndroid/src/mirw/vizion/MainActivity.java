package mirw.vizion;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity implements SurfaceHolder.Callback, GLSurfaceView.Renderer, Camera.PreviewCallback {

    private static final String TAG = MainActivity.class.getName();
    private SurfaceView mSurfaceView;
    private GLSurfaceView mGLSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private volatile byte[] mCallbackBuffer;
    private volatile byte[] mBufferData;
    private volatile int mBufferWidth;
    private volatile int mBufferHeight;
    private Texture mTexture;
    private Program mProgram;
  
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mGLSurfaceView = (GLSurfaceView)findViewById(R.id.glsurfaceview);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setZOrderOnTop(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
        mCamera = Camera.open();
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(parameters.getSupportedPreviewSizes().get(0).width, parameters.getSupportedPreviewSizes().get(0).height);
            parameters.setPreviewFormat(ImageFormat.NV21);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallbackWithBuffer(this);
            if (mHolder.getSurface() != null) {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
                addCallbackBuffer();
            }
        } catch (IOException e) {
            Log.e(TAG, "onResume caught IOException", e);
        }
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mGLSurfaceView.onPause();
        super.onPause();
    }
    
    // SurfaceHolder.Callback ------------------------------------------------- 
    
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                Log.e(TAG, "onResume caught IOException", e);
            }
            addCallbackBuffer();
        }        
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mCamera.startPreview();
            addCallbackBuffer();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    // GLSurfaceView.Renderer ------------------------------------------------- 

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        mProgram = new Program(new VertexShader(getResources().getString(R.string.vertexShader)),
                               new FragmentShader(getResources().getString(R.string.fragmentShader)));
        mProgram.setVertexAttrib("xy", new float[] {-1, -1, 1, -1, -1, 1, 1, 1}, 2);
        mProgram.setUniform("tex", 0);
        mTexture = new Texture(0, 0);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void onDrawFrame(GL10 gl) {
        if (mBufferData != null) {
            mTexture.setSize(mBufferWidth, mBufferHeight);
            mTexture.setData(mBufferData);
            mProgram.setUniform("duv", 1.0f/mBufferWidth, 1.0f/mBufferHeight);
            byte[] buffer = mBufferData;
            mBufferData = null;
            mCamera.addCallbackBuffer(buffer);
        }
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mTexture.use();
        mProgram.use();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    // Camera.PreviewCallback ------------------------------------------------- 
    
    public void onPreviewFrame(byte[] frameData, Camera camera) {
        if (mCamera != null) {
            Camera.Parameters parameters = camera.getParameters();
            mBufferWidth = parameters.getPreviewSize().width;
            mBufferHeight = parameters.getPreviewSize().height;
            mBufferData = frameData;
        }
    }
    
    private void addCallbackBuffer() {
        Camera.Parameters parameters = mCamera.getParameters();
        int bufferSize = parameters.getPreviewSize().width * parameters.getPreviewSize().height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat());
        if ((mCallbackBuffer == null) || (mCallbackBuffer.length != bufferSize)) {
            mCallbackBuffer = new byte[bufferSize];
            mCamera.addCallbackBuffer(mCallbackBuffer);        
        }
    }
}
