@echo off
set MATERIAL_BIN_TOOL_PATH="env\MaterialBinTool\MaterialBinTool-0.5.1-all.jar"
set TARGET_PATH="D:\MCLauncher\Minecraft-1.19.10.3\data\renderer\materials"

java -jar %MATERIAL_BIN_TOOL_PATH% "build\PostFX.Bloom.material.bin" -u
java -jar %MATERIAL_BIN_TOOL_PATH% "build\PostFX.Tonemapping.material.bin" -u

java -jar %MATERIAL_BIN_TOOL_PATH% "build\PostFX.Bloom\PostFX.Bloom.json" -r
java -jar %MATERIAL_BIN_TOOL_PATH% "build\PostFX.Tonemapping\PostFX.Tonemapping.json" -r

replace "build\PostFX.Bloom\PostFX.Bloom.material.bin" %TARGET_PATH%
replace "build\PostFX.Tonemapping\PostFX.Tonemapping.material.bin" %TARGET_PATH%