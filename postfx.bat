@echo off
set DXC_OPTIONS=-enable-16bit-types -Wno-ambig-lit-shift -validator-version 1.6
set MATERIAL_BIN_TOOL_PATH="env\MaterialBinTool\MaterialBinTool-0.5.1-all.jar"
set TARGET_PATH="D:\MCLauncher\Minecraft-1.19.10.3\data\renderer\materials"

echo -compiling PostFX.Bloom.DownscaleUniformPass
env\dxc\dxc.exe -T vs_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Bloom\BloomDownscaleUniformPass\0.Direct3D_SM65.Vertex.dxbc"    "PostFX\PostFX.Vertex.hlsl"
env\dxc\dxc.exe -T ps_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Bloom\BloomDownscaleUniformPass\0.Direct3D_SM65.Fragment.dxbc"  "PostFX\Bloom\BloomDownscaleUniformPass.Fragment.hlsl"
echo -compiling PostFX.Bloom.BloomDownscaleGaussianPass
env\dxc\dxc.exe -T vs_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Bloom\BloomDownscaleGaussianPass\0.Direct3D_SM65.Vertex.dxbc"   "PostFX\PostFX.Vertex.hlsl"
env\dxc\dxc.exe -T ps_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Bloom\BloomDownscaleGaussianPass\0.Direct3D_SM65.Fragment.dxbc" "PostFX\Bloom\BloomDownscaleGaussianPass.Fragment.hlsl"
echo -compiling PostFX.Bloom.BloomUpscalePass
env\dxc\dxc.exe -T vs_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Bloom\BloomUpscalePass\0.Direct3D_SM65.Vertex.dxbc"             "PostFX\PostFX.Vertex.hlsl"
env\dxc\dxc.exe -T ps_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Bloom\BloomUpscalePass\0.Direct3D_SM65.Fragment.dxbc"           "PostFX\Bloom\BloomUpscalePass.Fragment.hlsl"
echo -compiling PostFX.Tonemapping
env\dxc\dxc.exe -T vs_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Tonemapping\ToneMapping\0.Direct3D_SM65.Vertex.dxbc"            "PostFX\PostFX.Vertex.hlsl"
env\dxc\dxc.exe -T ps_6_5 %DXC_OPTIONS% -Fo "build\PostFX.Tonemapping\ToneMapping\0.Direct3D_SM65.Fragment.dxbc"          "PostFX\Tonemapping\ToneMapping.Fragment.hlsl"
echo ------compile completed------
java -jar %MATERIAL_BIN_TOOL_PATH% "build\PostFX.Bloom\PostFX.Bloom.json" -r
java -jar %MATERIAL_BIN_TOOL_PATH% "build\PostFX.Tonemapping\PostFX.Tonemapping.json" -r
replace "build\PostFX.Bloom\PostFX.Bloom.material.bin" %TARGET_PATH%
replace "build\PostFX.Tonemapping\PostFX.Tonemapping.material.bin" %TARGET_PATH%
echo ------replace completed------