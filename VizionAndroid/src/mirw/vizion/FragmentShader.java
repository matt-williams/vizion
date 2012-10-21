package mirw.vizion;

import android.opengl.GLES20;

public class FragmentShader extends Shader {
    public FragmentShader(String source) {
        super(GLES20.GL_FRAGMENT_SHADER, source);
    }
}