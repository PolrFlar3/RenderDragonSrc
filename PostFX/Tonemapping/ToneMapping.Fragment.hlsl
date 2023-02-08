cbuffer FragmentUniforms : register(b1) {
    float4 gToneMappingDebugMode;
    float4 gToneMappingSaturation;
    float4 gToneMappingShadowContrastEnd;
    float4 gToneMappingShadowContrast;
    float4 ScreenSize;
    float4 gToneMappingGamma;
    float4 gToneMappingIntensity;
    float4 gColorGradingEnabled;
    float4 gPerformSRGBConversion;
    float4 gToneMappingContrast;
    float4 gToneMappingFilmicSaturationCorrection;
    float4 gBloomMultiplier;
    float4 gToneMappingColorBalance;
};
struct PSInput {
    float4 position : SV_Position;
    float2 texcoord0 : TEXCOORD0;
};

SamplerState s_RasterColorSampler : register(s0);
SamplerState s_gToneCurveSampler : register(s1);
SamplerState s_gRasterizedInputSampler : register(s2);
SamplerState s_gBloomBufferSampler : register(s3);
Texture2D<float4> s_RasterColorTexture : register(t0);
Texture2D<float4> s_gToneCurveTexture : register(t1);
Texture2D<float4> s_gRasterizedInputTexture : register(t2);
Texture2D<float4> s_gBloomBufferTexture : register(t3);

float luminance(float3 color) {
    return dot(color.rgb, float3(0.2126, 0.7152, 0.0722));
}
float LinearToSrgbBranchingChannel(float lin) {
    if (lin < 0.00313067)
        return lin * 12.92;
    return pow(lin, (1.0 / 2.4)) * 1.055 - 0.055;
}
float3 LinearToSrgb(float3 lin) {
    return float3(LinearToSrgbBranchingChannel(lin.r),
                  LinearToSrgbBranchingChannel(lin.g),
                  LinearToSrgbBranchingChannel(lin.b));
}
float3 linearToGamma(float3 c) {
    return pow(c, 1.0 / 2.2);
}

float3 gammaToLinear(float3 c) {
    return pow(c, 2.2);
}
float4 main(PSInput input) : SV_Target0 {
    float4 rasterColor = s_RasterColorTexture.Sample(s_RasterColorSampler, input.texcoord0);
    float4 bloomColor = s_gBloomBufferTexture.Sample(s_gBloomBufferSampler, input.texcoord0);
    float3 color = mad(bloomColor.rgb, gBloomMultiplier.rgb, rasterColor.rgb);
    float3 originalColor = color;
    if (gToneMappingIntensity.x != 0.0f) {
        if (gToneMappingDebugMode.x == 0.0f) {
            float colorLuminance = max(luminance(color), -24);
            float toneCurve =
                exp2(s_gToneCurveTexture.SampleLevel(s_gToneCurveSampler,
                float2((log2(colorLuminance) + 24) * (1.0 / 28.0), 0.5), 0).x);
            float3 adjustColor = toneCurve * color / colorLuminance;
            float averageColor = dot(adjustColor, 1.0 / 3.0);
            float var1 = ((((1486.4 - (averageColor * 1489.7)) * averageColor) - 3.3) /
                          (((averageColor * 0.15 + 944.2) * averageColor) + 1)) - 1;
            var1 = gToneMappingFilmicSaturationCorrection.x * max(0, var1) + 1;
            color = lerp(averageColor, adjustColor, var1);
        } else {
            if (input.texcoord0.x < 0.25f) {
                float colorLuminance = luminance(color);
                float deformedLuminance = colorLuminance / (colorLuminance + 1.0f);
                float var2 = min(exp(-gToneMappingShadowContrastEnd.x) * deformedLuminance, 1);
                float adjustVal = (pow(var2, gToneMappingShadowContrast.x) * deformedLuminance) / colorLuminance;
                color = adjustVal * color;
            } else {
                if (input.texcoord0.x < 0.5f) {
                    float3 var3 = max(0.0f, color - 0.004);
                    float3 var4 = var3 * 6.2;
                    color = ((var4 + 0.5) * var3) / (((var4 + 1.7) * var3) + 0.06);

                    if (gPerformSRGBConversion.x != 0.0f) {
                        color = gammaToLinear(color);  // ?
                    }
                } else {
                    float colorLuminance = max(luminance(color), -24);
                    float toneCurve = exp2(s_gToneCurveTexture
                                               .SampleLevel(s_gToneCurveSampler,
                                                            float2((log2(colorLuminance) + 24) * (1.0 / 28.0), 0.5), 0)
                                               .x);
                    float3 adjustColor = toneCurve * color / colorLuminance;
                    float averageColor = dot(adjustColor, 1.0 / 3.0);
                    float var1 = ((((1486.4 - (averageColor * 1489.7)) * averageColor) - 3.3) /
                                  (((averageColor * 0.15 + 944.2) * averageColor) + 1)) -
                                 1;
                    var1 = gToneMappingFilmicSaturationCorrection.x * max(0, var1) + 1;
                    color = lerp(averageColor, adjustColor, var1);
                }
            }
            if (input.texcoord0.y >
                (0.5f - (s_gToneCurveTexture.Sample(s_gToneCurveSampler, input.texcoord0).x * (1.0 / 31.1752)))) {
                color += (1 - color) * 0.2;
            }
        }
    }
    if (gColorGradingEnabled.x != 0.0f) {
        color = saturate(((gToneMappingColorBalance.x * color) - 0.18) * gToneMappingContrast.x + 0.18);
        color = pow(lerp(luminance(color), color, gToneMappingSaturation.x), gToneMappingGamma.x);
    }

    color = lerp(originalColor, color, gToneMappingIntensity.x);

    if (gPerformSRGBConversion.x != 0.0f) {
        color = LinearToSrgb(color);
    }
    uint var6 = (uint(abs(ScreenSize.x * input.texcoord0.x)) << 16u) + uint(abs(ScreenSize.y * input.texcoord0.y));
    uint var7 = ((var6 ^ 61u) ^ (var6 >> 16u)) * 9u;
    uint var8 = ((var7 >> 4u) ^ var7) * 668265261u;
    float var9 = (1.0 / 510.0) - (float((var8 >> 15u) ^ var8) * 1.826122803319507603703186759958e-12f);
    float4 rasterizedInput = s_gRasterizedInputTexture.Sample(s_gRasterizedInputSampler, input.texcoord0);

    return float4(rasterizedInput.rgb + ((var9 + color) * (1 - rasterizedInput.w)), 1);
}
