attribute vec4 vPosition;
attribute vec2 vertexUV;

uniform mat4 uMVPMatrix;

varying vec2 texCoord;

void main() {
  gl_Position = uMVPMatrix * vPosition;
  texCoord = vertexUV;
}