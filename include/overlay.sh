bool overlayTopFace(vec3 chunkPos)
{
    vec3 normal = normalize(cross(dFdx(chunkPos), dFdy(chunkPos)));
    return normal.y > 0.99;
}

bool between(float value, float low, float high)
{
    return value >= low && value <= high;
}

bool rect(vec2 p, float x0, float x1, float y0, float y1)
{
    return between(p.x, x0, x1) && between(p.y, y0, y1);
}

vec3 redstoneColorForPower(int power)
{
    float normalized = float(power) * (1.0 / 15.0);
    float red = clamp(normalized * 0.6 + (power > 0 ? 0.4 : 0.3), 0.3, 1.0);
    float green = clamp(normalized * normalized * 0.7 - 0.5, 0.0, 1.0);
    return vec3(red, green, 0.0);
}

int redstonePowerFromColor(vec3 color)
{
    int power = 0;
    if (color.r > 0.38) {
        power = int(floor(((color.r - 0.4) / 0.6) * 15.0 + 0.5));
    }

    vec3 expected = redstoneColorForPower(power);
    vec3 delta = abs(color - expected);
    return delta.r < 0.005 && delta.g < 0.005 && delta.b < 0.005 ? power : -1;
}

int redstonePower(vec3 color)
{
    if (color.r <= color.g + color.b) {
        return -1;
    }

    return redstonePowerFromColor(color);
}

bool drawDigit(vec2 p, int value)
{
    if (value == 0) {
        return rect(p, 0.25, 0.35, 0.25, 0.75) ||
               rect(p, 0.45, 0.55, 0.25, 0.75) ||
               rect(p, 0.35, 0.45, 0.25, 0.35) ||
               rect(p, 0.35, 0.45, 0.65, 0.75);
    }
    if (value == 1) {
        return rect(p, 0.25, 0.55, 0.25, 0.35) ||
               rect(p, 0.35, 0.45, 0.35, 0.75) ||
               rect(p, 0.45, 0.55, 0.65, 0.75);
    }
    if (value == 2) {
        return rect(p, 0.35, 0.45, 0.45, 0.55) ||
               rect(p, 0.25, 0.35, 0.45, 0.75) ||
               rect(p, 0.45, 0.55, 0.25, 0.55) ||
               rect(p, 0.25, 0.45, 0.25, 0.35) ||
               rect(p, 0.35, 0.55, 0.65, 0.75);
    }
    if (value == 3) {
        return rect(p, 0.25, 0.35, 0.25, 0.75) ||
               rect(p, 0.35, 0.55, 0.45, 0.55) ||
               rect(p, 0.35, 0.55, 0.25, 0.35) ||
               rect(p, 0.35, 0.55, 0.65, 0.75);
    }
    if (value == 4) {
        return rect(p, 0.25, 0.35, 0.25, 0.75) ||
               rect(p, 0.45, 0.55, 0.45, 0.75) ||
               rect(p, 0.35, 0.45, 0.45, 0.55);
    }
    if (value == 5) {
        return rect(p, 0.35, 0.45, 0.45, 0.55) ||
               rect(p, 0.25, 0.35, 0.25, 0.55) ||
               rect(p, 0.45, 0.55, 0.45, 0.75) ||
               rect(p, 0.25, 0.45, 0.65, 0.75) ||
               rect(p, 0.35, 0.55, 0.25, 0.35);
    }
    if (value == 6) {
        return rect(p, 0.35, 0.45, 0.45, 0.55) ||
               rect(p, 0.25, 0.35, 0.25, 0.55) ||
               rect(p, 0.45, 0.55, 0.25, 0.75) ||
               rect(p, 0.25, 0.45, 0.65, 0.75) ||
               rect(p, 0.35, 0.55, 0.25, 0.35);
    }
    if (value == 7) {
        return rect(p, 0.25, 0.35, 0.25, 0.75) ||
               rect(p, 0.35, 0.55, 0.65, 0.75) ||
               rect(p, 0.45, 0.55, 0.55, 0.65);
    }
    if (value == 8) {
        return rect(p, 0.35, 0.45, 0.45, 0.55) ||
               rect(p, 0.25, 0.35, 0.25, 0.75) ||
               rect(p, 0.45, 0.55, 0.25, 0.75) ||
               rect(p, 0.35, 0.45, 0.25, 0.35) ||
               rect(p, 0.35, 0.45, 0.65, 0.75);
    }
    if (value == 9) {
        return rect(p, 0.35, 0.45, 0.45, 0.55) ||
               rect(p, 0.25, 0.35, 0.35, 0.75) ||
               rect(p, 0.45, 0.55, 0.45, 0.75) ||
               rect(p, 0.25, 0.55, 0.25, 0.35) ||
               rect(p, 0.35, 0.55, 0.65, 0.75);
    }

    return false;
}


bool drawDigitAt(vec2 p, int value, float xOffset)
{
    p.x -= xOffset;
    return drawDigit(p, value);
}

bool drawValue(vec2 p, int value)
{
    if (value < 10) {
        return drawDigitAt(p, value, 0.0);
    }

    //return drawDigitAt(p, 1, 0.0) || drawDigitAt(p, value - 10, 0.30);
    return drawDigitAt(p, 1, 0.15) || drawDigitAt(p, value - 10, -0.15);
}

bool drawRedstoneBitBar(vec2 p, int value)
{
    bool rails = rect(p, 0.10, 0.70, 0.05, 0.08) ||
                 rect(p, 0.10, 0.70, 0.17, 0.20);
    float bitColumn = floor((4.0 / 0.6) * (p.x - 0.1));
    bool bit = mod(floor(float(value) / exp2(bitColumn)), 2.0) >= 0.5;
    return rails || (bit && between(p.y, 0.10, 0.15));
}

int redstone_overlay(vec3 chunkPos, vec3 color)
{
    if (!overlayTopFace(chunkPos)) {
        return 0;
    }

    int power = redstonePower(color);
    if (power < 0) {
        return 0;
    }

    vec2 p = fract(chunkPos).xz * 3.0 - vec2(1.1, 1.1);
    if (drawValue(p, power) || (power > 0 && drawRedstoneBitBar(p, power))) {
        return 2;
    }

    return 1;
}

int lightOverlayDigit(vec2 p, float lightLevel, int highValueMax)
{
    //int value = int(floor((lightLevel + 0.006666667) * 15.0));
    // https://www.lomont.org/posts/2023/accuratecolorconversions/
    int value = int(floor(lightLevel * 16.0));
    value = clamp(value, 0, 15);

    return drawValue(p, value) ? (value <= highValueMax ? 2 : 1) : 0;
}

int light_overlay(vec3 chunkPos, vec2 light_uv)
{
    if (!overlayTopFace(chunkPos)) {
        return 0;
    }

    vec2 p = fract(chunkPos).xz * 3.0 - vec2(1.23, 1.1);

    int xOverlay = lightOverlayDigit(p, light_uv.x, 0);
    if (xOverlay > 0) {
        return xOverlay;
    }

    p = p * 2.0 + vec2(0.55, -0.25);
    return lightOverlayDigit(p, light_uv.y, 7);
}

int edgeCount16(vec3 p, float width)
{
    int count = 0;
    if (p.x < width || p.x > 16.0 - width) { count += 1; }
    if (p.y < width || p.y > 16.0 - width) { count += 1; }
    if (p.z < width || p.z > 16.0 - width) { count += 1; }
    return count;
}

int edgeCount1(vec3 p, float width)
{
    int count = 0;
    if (p.x < width || p.x > 1.0 - width) { count += 1; }
    if (p.y < width || p.y > 1.0 - width) { count += 1; }
    if (p.z < width || p.z > 1.0 - width) { count += 1; }
    return count;
}

bool chunkCorner(vec3 p, float width)
{
    return edgeCount16(p, width) >= 2;
}

bool blockGridCorner(vec3 p, float width)
{
    return edgeCount1(p, width) >= 2;
}

vec3 chunkAxisColor(vec3 chunkPos)
{
    return max(chunkPos.z, chunkPos.x) < 0.0625
        ? vec3(1.0, 0.2, 0.2)
        : vec3(0.0, 0.0, 1.0);
}

vec3 chunkInnerColor(vec3 color)
{
    return (color / 0.4) * (vec3(1.0, 1.0, 1.0) - color);
}

vec4 chunk_border(vec3 chunkPos, vec4 color)
{
    vec3 cp = fract(chunkPos);

    if (chunkCorner(chunkPos, 0.0625)) {
        color.rgb = mix(color.rgb, chunkAxisColor(chunkPos), 0.2);
    } else if ((chunkPos.x < 0.03125 || chunkPos.x > 15.96875 ||
                chunkPos.z < 0.03125 || chunkPos.z > 15.96875) &&
               blockGridCorner(cp, 0.03125)) {
        color.rgb = chunkInnerColor(color.rgb);
    }

    return color;
}
