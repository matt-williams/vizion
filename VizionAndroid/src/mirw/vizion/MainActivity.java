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
    private Texture mCameraTexture;
    private Texture mFrontBuffer;
    private Texture mBackBuffer;
    private Program mAccumulationProgram;
    private Program mDisplayProgram;
    private int mGLWidth;
    private int mGLHeight;
  
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
        Utils.checkErrors("glDisable");
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        Utils.checkErrors("glDepthTest");
        GLES20.glDepthMask(false);
        Utils.checkErrors("glDepthMask");

        mAccumulationProgram = new Program(new VertexShader(getResources().getString(R.string.vertexShader)),
                               new FragmentShader(getResources().getString(R.string.accumulationFragmentShader)));
        mAccumulationProgram.setVertexAttrib("xy", new float[] {-1, -1, 1, -1, -1, 1, 1, 1}, 2);
        mAccumulationProgram.setUniform("cam", 0);
        mAccumulationProgram.setUniform("old", 1);
        mDisplayProgram = new Program(new VertexShader(getResources().getString(R.string.vertexShader)),
                                new FragmentShader(getResources().getString(R.string.displayFragmentShader)));
        mDisplayProgram.setVertexAttrib("xy", new float[] {-1, -1, 1, -1, -1, 1, 1, 1}, 2);
        mDisplayProgram.setUniform("tex", 0);
        mCameraTexture = new Texture(0, 0);
        mFrontBuffer = new Texture(0, 0);
        mBackBuffer = new Texture(0, 0);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Utils.checkErrors("glViewport");
        mGLWidth = width;
        mGLHeight = height;
    }

    public void onDrawFrame(GL10 gl) {
        if (mBufferData != null) {
            mCameraTexture.setSize(mBufferWidth, mBufferHeight);
            mCameraTexture.setData(mBufferData);
            mFrontBuffer.setSize(mBufferWidth, mBufferHeight);
            mFrontBuffer.renderTo();
            mBackBuffer.setSize(mBufferWidth, mBufferHeight);
            byte[] buffer = mBufferData;
            mBufferData = null;
            mCamera.addCallbackBuffer(buffer);
        }
        
        mBackBuffer.renderTo();
        mCameraTexture.use(GLES20.GL_TEXTURE0);
        mFrontBuffer.use(GLES20.GL_TEXTURE1);
        mAccumulationProgram.use();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        Utils.checkErrors("glDrawArrays");
        
        Texture.renderToScreen(mGLWidth, mGLHeight);
        mBackBuffer.use(GLES20.GL_TEXTURE0);
        mDisplayProgram.use();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        Utils.checkErrors("glDrawArrays");
        
        Texture buffer = mBackBuffer;
        mBackBuffer = mFrontBuffer;
        mFrontBuffer = buffer;
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
