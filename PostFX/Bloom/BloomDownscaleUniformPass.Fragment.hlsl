SamplerState s_RasterColorSampler : register(s1);
Texture2D<float4> s_RasterColorTexture : register(t1);

struct PSInput {
    float4 position : SV_Position;
    float2 texcoord0 : TEXCOORD0;
};

float4 main(PSInput input) : SV_Target0 {
    float3 rasterColor = s_RasterColorTexture.Sample(s_RasterColorSampler, input.texcoord0).rgb;

    rasterColor = (any(isnan(rasterColor) || isinf(rasterColor))) ? 0 : max(rasterColor, 0);

    return float4(rasterColor, 1);
}
