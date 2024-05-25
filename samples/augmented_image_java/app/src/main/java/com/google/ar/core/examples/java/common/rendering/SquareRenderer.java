package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Renders a square in OpenGL.
 */
public class SquareRenderer {
    private static final String TAG = SquareRenderer.class.getSimpleName();
    // Shader names.
    private static final String VERTEX_SHADER_NAME = "shaders/square.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/square.frag";
    private static final float[] DEFAULT_COLOR = new float[]{0, 255, 0, 255};

    private int program;
    // Shader location: model view projection matrix.
    private int modelViewUniform;
    private int modelViewProjectionUniform;

    // Shader location: object color property (to change the primary color of the object).
    private int colorUniform;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private FloatBuffer vertexBuffer; // Buffer for vertex-array
    private float[] vertices = new float[]{// Vertices for the square
            -1.0f, -1.0f, 0.0f,  // 0. left-bottom
            1.0f, -1.0f, 0.0f,  // 1. right-bottom
            -1.0f, 1.0f, 0.0f,  // 2. left-top
            1.0f, 1.0f, 0.0f // 3. right-top
    };

    /**
     * Creates and initializes OpenGL resources needed for rendering the model.
     *
     * @param context Context for loading the shader and below-named model and texture assets.
     */
    public void createOnGlThread(Context context)
            throws IOException {
        // Compiles and loads the shader based on the current configuration.
        compileAndLoadShaderProgram(context);

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use native byte order
        vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
        vertexBuffer.put(vertices);// Copy data into buffer
        vertexBuffer.position(0); // Rewind

        ShaderUtil.checkGLError(TAG, "Square vertex loading");
        Matrix.setIdentityM(modelMatrix, 0);
    }

    private void compileAndLoadShaderProgram(Context context) throws IOException {
        // Compiles and loads the shader program based on the selected mode.
        final int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        final int fragmentShader =
                ShaderUtil.loadGLShader(
                        TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME, null);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);

        ShaderUtil.checkGLError(TAG, "Program creation");

        modelViewUniform = GLES20.glGetUniformLocation(program, "u_ModelView");
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection");

        colorUniform = GLES20.glGetUniformLocation(program, "u_ObjColor");

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }

    /**
     * Updates the object model matrix and applies scaling.
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @param scaleFactor A separate scaling factor to apply before the {@code modelMatrix}.
     * @see Matrix
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    public void draw(
            float[] cameraView,
            float[] cameraPerspective) {

        ShaderUtil.checkGLError(TAG, "Before draw");

        // Build the ModelView and ModelViewProjection matrices
        Matrix.rotateM(modelMatrix, 0, 90, 1, 0, 0);

        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        GLES20.glUseProgram(program);

        // Set the object color property.
        GLES20.glUniform4fv(colorUniform, 1, DEFAULT_COLOR, 0);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(modelViewUniform, 1, false, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

        ShaderUtil.checkGLError(TAG, "After draw");
    }
}
