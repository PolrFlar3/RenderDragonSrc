struct PSInput {
    float4 position : SV_Position;
    float2 texcoord0 : TEXCOORD0;
};

PSInput main(float4 position : POSITION, float2 texcoord0 : TEXCOORD0) {
    float2 pos = (position.xy * 2.0f) + (-1.0f);
    PSInput result;
    result.texcoord0 = texcoord0;
    result.position = float4(pos, 0.0f, 1.0f);
    return result;
}
