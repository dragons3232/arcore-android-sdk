package com.google.ar.core.examples.java.common.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.examples.java.augmentedimage.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureFlag() {
    private val TAG = TextureFlag::class.java.simpleName

    private val VERTEX_SHADER_NAME = "shaders/tex_flag.vert"
    private val FRAGMENT_SHADER_NAME = "shaders/tex_flag.frag"

    private var program = 0;
    private var textureUniform = 0
    private var modelViewProjectionUniform = 0

    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    private var squareBuffer: FloatBuffer? = null
    private val squareVertices = floatArrayOf(
        -1.0f, -1.0f, 0.0f,  // 0. left-bottom
        1.0f, -1.0f, 0.0f,  // 1. right-bottom
        -1.0f, 1.0f, 0.0f,  // 2. left-top
        1.0f, 1.0f, 0.0f // 3. right-top
    )

    private var textureBuffer: FloatBuffer? = null
    private val textureVertices = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private val textureIDs = IntArray(1)
    private var bitmap: Bitmap? = null

    private fun loadTexture() {
        GLES20.glGenTextures(1, textureIDs, 0) // Generate texture-ID array for numFaces IDs

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIDs.get(0))
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        );
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        );
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        );
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        );
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap?.recycle()
    }

    fun createOnGlThread(context: Context?) {
        val linked = IntArray(1)

        // Compiles and loads the shader program based on the selected mode.
        val vertexShader = ShaderUtil.loadGLShader(
            TAG,
            context,
            GLES20.GL_VERTEX_SHADER,
            VERTEX_SHADER_NAME
        )
        val fragmentShader = ShaderUtil.loadGLShader(
            TAG,
            context,
            GLES20.GL_FRAGMENT_SHADER,
            FRAGMENT_SHADER_NAME,
            null
        )

        // Create the program object
        val programObject = GLES20.glCreateProgram()

        if (programObject == 0) return

        Log.e("created", "attaching shaders")
        GLES20.glAttachShader(programObject, vertexShader)
        GLES20.glAttachShader(programObject, fragmentShader)

        GLES20.glBindAttribLocation(programObject, 0, "vPosition")
        // Link the program
        GLES20.glLinkProgram(programObject)

        // Get access to projection matrix. Must call after linking program
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programObject, "uMVPMatrix");
        textureUniform = GLES20.glGetUniformLocation(programObject, "uTexture")

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0)

        if (linked[0] == 0) {
            GLES20.glDeleteProgram(programObject)
            return
        }

        // Store the program object
        program = programObject

        var buff = ByteBuffer.allocateDirect(textureVertices.size * 4)
        buff.order(ByteOrder.nativeOrder())
        textureBuffer = buff.asFloatBuffer()
        textureBuffer?.put(textureVertices)
        textureBuffer?.position(0)

        buff = ByteBuffer.allocateDirect(squareVertices.size * 4)
        buff.order(ByteOrder.nativeOrder())
        squareBuffer = buff.asFloatBuffer()
        squareBuffer?.put(squareVertices)
        squareBuffer?.position(0)

        bitmap = BitmapFactory.decodeResource(
            context?.getResources(),
            R.drawable.vn
        )
        loadTexture()
    }

    /**
     * Updates the object model matrix and applies scaling.
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @param scaleFactor A separate scaling factor to apply before the `modelMatrix`.
     * @see Matrix
     */
    fun updateModelMatrix(modelMatrix: FloatArray?, scaleFactor: Float) {
        val scaleMatrix = FloatArray(16)
        Matrix.setIdentityM(scaleMatrix, 0)
        scaleMatrix[0] = scaleFactor
        scaleMatrix[5] = scaleFactor
        scaleMatrix[10] = scaleFactor
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0)
    }

    fun draw(
        cameraView: FloatArray,
        cameraPerspective: FloatArray
    ) {
        // Build the ModelView and ModelViewProjection matrices
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)

        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0)

        // Use the program object
        GLES20.glUseProgram(program);

        GLES20.glUniformMatrix4fv(
            modelViewProjectionUniform,
            1,
            false,
            modelViewProjectionMatrix,
            0
        )

        GLES20.glEnableVertexAttribArray(textureUniform);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIDs[0]);

        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, squareBuffer)
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, squareVertices.size / 3);

        GLES20.glDisableVertexAttribArray(textureUniform);
    }
}