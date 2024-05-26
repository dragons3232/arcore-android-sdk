precision mediump float;

// Passed in from the vertex shader.
varying vec2 texCoord;

// The texture.
uniform sampler2D uTexture;

void main() {
   gl_FragColor = texture2D(uTexture, texCoord);
}