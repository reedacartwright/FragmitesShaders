/*
* Available Macros:
*
* Passes:
* - ALPHA_TEST_PASS
* - DEPTH_ONLY_PASS
* - DEPTH_ONLY_OPAQUE_PASS
* - OPAQUE_PASS
* - TRANSPARENT_PASS
*
* Dithering:
* - DITHERING__OFF
* - DITHERING__ON
*
* Instancing:
* - INSTANCING__OFF (not used)
* - INSTANCING__ON (not used)
*
* RenderAsBillboards:
* - RENDER_AS_BILLBOARDS__OFF (not used)
* - RENDER_AS_BILLBOARDS__ON (not used)
*
* Seasons:
* - SEASONS__OFF
* - SEASONS__ON
*/

$input v_clipPosition, v_color0, v_dithering, v_fog, v_lightmapUV, v_texcoord0, v_worldPos, v_worldPosition

#include "bgfx_shader.sh"

vec4 textureSample(mediump sampler2D _sampler, vec2 _coord) {
    return texture2D(_sampler, _coord);
}
vec4 textureSample(mediump sampler3D _sampler, vec3 _coord) {
    return texture3D(_sampler, _coord);
}
vec4 textureSample(mediump samplerCube _sampler, vec3 _coord) {
    return textureCube(_sampler, _coord);
}
vec4 textureSample(mediump sampler2D _sampler, vec2 _coord, float _lod) {
    return texture2DLod(_sampler, _coord, _lod);
}
vec4 textureSample(mediump sampler3D _sampler, vec3 _coord, float _lod) {
    return texture3DLod(_sampler, _coord, _lod);
}
vec4 textureSample(mediump sampler2DArray _sampler, vec3 _coord) {
    return texture2DArray(_sampler, _coord);
}
vec4 textureSample(mediump sampler2DArray _sampler, vec3 _coord, float _lod) {
    return texture2DArrayLod(_sampler, _coord, _lod);
}

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 GlobalRoughness;
uniform vec4 LightDiffuseColorAndIlluminance;
uniform vec4 LightWorldSpaceDirection;
uniform vec4 MaterialID;
uniform vec4 RenderChunkFogAlpha;
uniform vec4 SubPixelOffset;
uniform vec4 DitherParams;
uniform vec4 DitherParams2;
uniform vec4 ViewPositionAndTime;

vec4 ViewRect;
mat4 Proj;
mat4 View;
vec4 ViewTexel;
mat4 InvView;
mat4 InvProj;
mat4 ViewProj;
mat4 InvViewProj;
mat4 PrevViewProj;
mat4 WorldArray[4];
mat4 World;
mat4 WorldView;
mat4 WorldViewProj;
vec4 PrevWorldPosOffset;
vec4 AlphaRef4;
float AlphaRef;

struct FragmentInput {
    vec4 clipPosition;
    vec4 color0;
    float dithering;
    vec4 fog;
    vec2 lightmapUV;
    vec2 texcoord0;
    vec3 worldPos;
    vec4 worldPosition;
};

struct FragmentOutput {
    vec4 Color0;
};

SAMPLER2D_AUTOREG(s_LightMapTexture);
SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
struct StandardSurfaceInput {
    vec2 UV;
    vec3 Color;
    float Alpha;
    vec2 lightmapUV;
    vec4 fog;
    vec2 texcoord0;
    float dithering;
    vec4 clipPosition;
    vec4 worldPosition;
};

StandardSurfaceInput StandardTemplate_DefaultInput(FragmentInput fragInput) {
    StandardSurfaceInput result;
    result.UV = vec2(0, 0);
    result.Color = vec3(1, 1, 1);
    result.Alpha = 1.0;
    result.lightmapUV = fragInput.lightmapUV;
    result.fog = fragInput.fog;
    result.texcoord0 = fragInput.texcoord0;
    result.dithering = fragInput.dithering;
    result.clipPosition = fragInput.clipPosition;
    result.worldPosition = fragInput.worldPosition;
    return result;
}
struct StandardSurfaceOutput {
    vec3 Albedo;
    float Alpha;
    float Metallic;
    float Roughness;
    float Occlusion;
    float Emissive;
    float Subsurface;
    vec3 AmbientLight;
    vec3 ViewSpaceNormal;
};

StandardSurfaceOutput StandardTemplate_DefaultOutput() {
    StandardSurfaceOutput result;
    result.Albedo = vec3(1, 1, 1);
    result.Alpha = 1.0;
    result.Metallic = 0.0;
    result.Roughness = 1.0;
    result.Occlusion = 0.0;
    result.Emissive = 0.0;
    result.Subsurface = 0.0;
    result.AmbientLight = vec3(0.0, 0.0, 0.0);
    result.ViewSpaceNormal = vec3(0, 1, 0);
    return result;
}
vec3 applyFogVanilla(vec3 diffuse, vec3 fogColor, float fogIntensity) {
    return mix(diffuse, fogColor, fogIntensity);
}
vec4 applySeasons(vec3 vertexColor, float vertexAlpha, vec4 diffuse) {
    vec2 uv = vertexColor.xy;
    diffuse.rgb *= mix(vec3(1.0, 1.0, 1.0), textureSample(s_SeasonsTexture, uv).rgb * 2.0, vertexColor.b);
    diffuse.rgb *= vec3_splat(vertexAlpha);
    diffuse.a = 1.0;
    return diffuse;
}
void RenderChunkApplyFog(FragmentInput fragInput, StandardSurfaceInput surfaceInput, StandardSurfaceOutput surfaceOutput, inout FragmentOutput fragOutput) {
    fragOutput.Color0.rgb = applyFogVanilla(fragOutput.Color0.rgb, FogColor.rgb, surfaceInput.fog.a);
}
void RenderChunkSurfTransparent(in StandardSurfaceInput surfaceInput, inout StandardSurfaceOutput surfaceOutput) {
    vec4 diffuse = textureSample(s_MatTexture, surfaceInput.UV);
    diffuse.a *= surfaceInput.Alpha;
    diffuse.rgb *= surfaceInput.Color.rgb;
    surfaceOutput.Albedo = diffuse.rgb;
    surfaceOutput.Alpha = diffuse.a;
    surfaceOutput.Roughness = GlobalRoughness.x;
}
struct CompositingOutput {
    vec3 mLitColor;
};

vec4 standardComposite(StandardSurfaceOutput stdOutput, CompositingOutput compositingOutput) {
    return vec4(compositingOutput.mLitColor, stdOutput.Alpha);
}
void StandardTemplate_CustomSurfaceShaderEntryIdentity(vec2 uv, vec3 worldPosition, inout StandardSurfaceOutput surfaceOutput) {
}
struct DirectionalLight {
    vec3 ViewSpaceDirection;
    vec3 Intensity;
};

vec3 computeLighting_RenderChunk(FragmentInput fragInput, StandardSurfaceInput stdInput, StandardSurfaceOutput stdOutput, DirectionalLight primaryLight) {
    return textureSample(s_LightMapTexture, stdInput.lightmapUV).rgb * stdOutput.Albedo;
}

bool dissolvePosition(vec4 clipPosition, vec4 worldPosition) {
    vec3 ndc = clipPosition.xyz / clipPosition.w;
    vec2 screenUV = ndc.xy * 0.5 + 0.5;
    vec2 pixelCoords = screenUV * DitherParams.xy;
    vec2 ditherBlock = floor(pixelCoords / DitherParams2.x) * DitherParams2.x;

    vec2 block4 = floor(ditherBlock * 0.25);
    vec2 block2 = floor(ditherBlock * 0.5);
    vec2 block1 = floor(ditherBlock);

    vec3 forward = -normalize(vec3(u_view[0].z, u_view[1].z, u_view[2].z));
    vec3 fragment = worldPosition.xyz - ViewPositionAndTime.xyz;
    float viewDot = dot(forward, fragment);
    float fadeValue = smoothstep(DitherParams.z, DitherParams.w, viewDot);

    float hash1 = fract(block4.x * 0.5 + block4.y * block4.y * 0.75);
    float hash2 = fract(block2.x * 0.5 + block2.y * block2.y * 0.75);
    float hash3 = fract(block1.x * 0.5 + block1.y * block1.y * 0.75);

    float hash = ((hash1 * 0.25 + hash2) * 0.25 + hash3) * 64.0 + 0.5;
    float ditherValue = hash / 64.0;

    return fadeValue <= ditherValue;
}

void RenderChunkSurfAlpha(in StandardSurfaceInput surfaceInput, inout StandardSurfaceOutput surfaceOutput) {
    vec4 diffuse = textureSample(s_MatTexture, surfaceInput.UV);

    bool dissolve = false;
    if(surfaceInput.dithering > 0.5) {
#if defined(DITHERING__ON)
        dissolve = dissolvePosition(surfaceInput.clipPosition, surfaceInput.worldPosition);
#endif
#if defined(DITHERING__OFF)
        dissolve = true;
#endif
    }

    const float ALPHA_THRESHOLD = 0.5;
    if (dissolve || diffuse.a < ALPHA_THRESHOLD) {
        discard;
    }
    #ifdef ALPHA_TEST_PASS
    #ifdef SEASONS__ON
    diffuse = applySeasons(surfaceInput.Color, surfaceInput.Alpha, diffuse);
    #else
    diffuse.rgb *= surfaceInput.Color.rgb;
    #endif
    surfaceOutput.Albedo = diffuse.rgb;
    surfaceOutput.Alpha = diffuse.a;
    surfaceOutput.Roughness = GlobalRoughness.x;
    #endif
}
void RenderChunkSurfOpaque(in StandardSurfaceInput surfaceInput, inout StandardSurfaceOutput surfaceOutput) {
    #ifdef OPAQUE_PASS
    vec4 diffuse = textureSample(s_MatTexture, surfaceInput.UV);

    #ifdef SEASONS__ON
    diffuse = applySeasons(surfaceInput.Color, surfaceInput.Alpha, diffuse);
    #else
    diffuse.rgb *= surfaceInput.Color.rgb;
    diffuse.a = surfaceInput.Alpha;
    #endif

    surfaceOutput.Albedo = diffuse.rgb;
    surfaceOutput.Alpha = diffuse.a;
    surfaceOutput.Roughness = GlobalRoughness.x;
    #endif
}
void StandardTemplate_Opaque_Frag(FragmentInput fragInput, inout FragmentOutput fragOutput) {
    StandardSurfaceInput surfaceInput = StandardTemplate_DefaultInput(fragInput);
    StandardSurfaceOutput surfaceOutput = StandardTemplate_DefaultOutput();
    surfaceInput.UV = fragInput.texcoord0;
    surfaceInput.Color = fragInput.color0.xyz;
    surfaceInput.Alpha = fragInput.color0.a;
    #ifdef TRANSPARENT_PASS
    RenderChunkSurfTransparent(surfaceInput, surfaceOutput);
    #elif defined(ALPHA_TEST_PASS)|| defined(DEPTH_ONLY_PASS)
    RenderChunkSurfAlpha(surfaceInput, surfaceOutput);
    #elif defined(DEPTH_ONLY_OPAQUE_PASS)|| defined(OPAQUE_PASS)
    RenderChunkSurfOpaque(surfaceInput, surfaceOutput);
    #endif
    StandardTemplate_CustomSurfaceShaderEntryIdentity(surfaceInput.UV, fragInput.worldPos, surfaceOutput);
    DirectionalLight primaryLight;
    vec3 worldLightDirection = LightWorldSpaceDirection.xyz;
    primaryLight.ViewSpaceDirection = mul(View, vec4(worldLightDirection, 0)).xyz;
    primaryLight.Intensity = LightDiffuseColorAndIlluminance.rgb * LightDiffuseColorAndIlluminance.w;
    CompositingOutput compositingOutput;
    compositingOutput.mLitColor = computeLighting_RenderChunk(fragInput, surfaceInput, surfaceOutput, primaryLight);
    fragOutput.Color0 = standardComposite(surfaceOutput, compositingOutput);
    RenderChunkApplyFog(fragInput, surfaceInput, surfaceOutput, fragOutput);
}
void main() {
    FragmentInput fragmentInput;
    FragmentOutput fragmentOutput;
    fragmentInput.clipPosition = v_clipPosition;
    fragmentInput.color0 = v_color0;
    fragmentInput.dithering = v_dithering;
    fragmentInput.fog = v_fog;
    fragmentInput.lightmapUV = v_lightmapUV;
    fragmentInput.texcoord0 = v_texcoord0;
    fragmentInput.worldPos = v_worldPos;
    fragmentInput.worldPosition = v_worldPosition;
    fragmentOutput.Color0 = vec4(0, 0, 0, 0);
    ViewRect = u_viewRect;
    Proj = u_proj;
    View = u_view;
    ViewTexel = u_viewTexel;
    InvView = u_invView;
    InvProj = u_invProj;
    ViewProj = u_viewProj;
    InvViewProj = u_invViewProj;
    PrevViewProj = u_prevViewProj;
    {
        WorldArray[0] = u_model[0];
        WorldArray[1] = u_model[1];
        WorldArray[2] = u_model[2];
        WorldArray[3] = u_model[3];
    }
    World = u_model[0];
    WorldView = u_modelView;
    WorldViewProj = u_modelViewProj;
    PrevWorldPosOffset = u_prevWorldPosOffset;
    AlphaRef4 = u_alphaRef4;
    AlphaRef = u_alphaRef4.x;
    StandardTemplate_Opaque_Frag(fragmentInput, fragmentOutput);
    gl_FragColor = fragmentOutput.Color0;
}
