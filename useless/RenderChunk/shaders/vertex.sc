// clang-format off
$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif

$output v_clipPosition, v_color0, v_ditheringAndMaskTinting, v_fog
$output v_lightmapUV, v_texcoord0, v_worldPos, v_worldPosition, v_origPosition
// clang-format on

#include "bgfx_shader.sh"

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 MeshContext;
uniform vec4 RenderChunkFogAlpha;
uniform vec4 SubPixelOffset;
uniform vec4 ViewPositionAndTime;

vec3 calculateWorldPos() {
#ifdef INSTANCING__ON
  mat4 model = mtxFromRows(i_data1, i_data2, i_data3, vec4(0.0, 0.0, 0.0, 1.0));

  vec4 pos4 = instMul(model, vec4(a_position, 1.0)); 
#else
  vec4 pos4 = mul(u_model[0], vec4(a_position, 1.0));
#endif
  return pos4.xyz;
}

vec3 transformAsBillboardVertex(vec3 worldPos) {
#ifdef RENDER_AS_BILLBOARDS__ON
  worldPos += vec3(0.5);
  vec3 forward = normalize(worldPos - ViewPositionAndTime.xyz);
  vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), forward));
  vec3 up = cross(forward, right);
  vec3 offsets = a_color0.xyz;
  worldPos -= up * (offsets.z - 0.5) + right * (offsets.x - 0.5);
#endif
  return worldPos;
}

vec4 jitterVertexPosition(vec3 worldPos) {
  mat4 offsetProj = u_proj;
#if BGFX_SHADER_LANGUAGE_GLSL
  offsetProj[2][0] += SubPixelOffset.x;
  offsetProj[2][1] -= SubPixelOffset.y;
#else
  offsetProj[0][2] += SubPixelOffset.x;
  offsetProj[1][2] -= SubPixelOffset.y;
#endif
  return mul(offsetProj, mul(u_view, vec4(worldPos, 1.0f)));
}

vec2 calculateLightmapUV(uvec2 u) {
  u = uvec2(u.y >> 4u, u.y) & 15u;
  return vec2(u) * (1.0 / 15.0);
}

vec2 calculateDither(uvec2 u) {
  return vec2(notEqual(u & 256u, uvec2_splat(0u)));
}

vec2 calculateCoord0(uvec2 u) {
  vec2 magnitude = vec2((u & 32767u) << 1u) * (1.0 / 65535.0);
  vec2 sign = 2.0 * vec2((u & 32768u) >> 15u) - 1.0;
  return magnitude + sign * (1.0 / 32768.0);
}

vec4 calculateFog(float cameraDepth, float maxDistance,
                  float fogStart, float fogEnd,
                  float fogAlpha) {
#ifdef ENABLE_NO_FOG
  return vec4(FogColor.rgb, 0.0);
#else
  float distance = cameraDepth / maxDistance;
  distance += fogAlpha;
  float intensity = clamp((distance - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
  return vec4(FogColor.rgb, intensity);
#endif
}

vec4 calculateColor(float cameraDepth, float alphaFadeDistance) {
#ifdef RENDER_AS_BILLBOARDS__ON
  vec4 color = vec4_splat(1.0);
#else
  vec4 color = a_color0;
#endif

#ifdef TRANSPARENT_PASS
  bool shouldBecomeOpaqueInTheDistance = a_color0.a < 0.95;
  if (shouldBecomeOpaqueInTheDistance) {
    float cameraDistance = cameraDepth / alphaFadeDistance;
    float alphaFadeOut = clamp(cameraDistance, 0.0, 1.0);
    color.a = mix(a_color0.a, 1.0, alphaFadeOut);
  }
#endif

  return color;
}

void main() {
  vec3 worldPos = calculateWorldPos();
  vec3 worldPos2 = transformAsBillboardVertex(worldPos);

  // Added by Useless Shaders
  v_origPosition = a_position + (worldPos2 - worldPos);

  float disableDistanceFog = (MeshContext.x > 0.5) ? 1.0 : 0.0;
  vec4 modifiedFogAndDistanceControl = mix(
    FogAndDistanceControl,
    vec4(0.99, 1.0, 100000.0, 100000.0),
    vec4_splat(disableDistanceFog)
  );

  vec4 clipPosition = jitterVertexPosition(worldPos2);

  uvec2 packed0 = uvec2(round(a_texcoord0 * 65535.0));
  v_texcoord0 = calculateCoord0(packed0);

  float cameraDepth = length(ViewPositionAndTime.xyz - worldPos2);
  v_fog = calculateFog(cameraDepth, modifiedFogAndDistanceControl.z,
    modifiedFogAndDistanceControl.x, modifiedFogAndDistanceControl.y,
    RenderChunkFogAlpha.x);

  v_color0 = calculateColor(cameraDepth, modifiedFogAndDistanceControl.w);
  
  uvec2 packed1 = uvec2(round(a_texcoord1 * 65535.0));
  v_ditheringAndMaskTinting = calculateDither(packed1);
  v_lightmapUV = calculateLightmapUV(packed1);

  v_worldPos = worldPos;

  v_clipPosition = clipPosition;
  v_worldPosition = vec4(worldPos, 0.0);

  gl_Position = clipPosition;
}
