bool overlayTopFace(vec3 chunkPos)
{
    vec3 normal = normalize(cross(dFdx(chunkPos), dFdy(chunkPos)));
    return normal.y > 0.99;
}

bool between(float value, float low, float high)
{
    return value >= low && value < high;
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

bool drawDigit(vec2 p, int digit)
{
    if (!rect(p, 0.25, 0.55, 0.25, 0.75)) {
        return false;
    }

    // Represent digits as a 15-segment display.
    ivec2 cell = ivec2(floor((p - vec2(0.25, 0.25)) * 10.0));
    int x = cell.x;
    int y = cell.y;

    if (digit == 0) {
        return x == 0 || x == 2 || y == 0 || y == 4;
    }
    if (digit == 1) {
        return y == 0 || x == 1 || (x == 2 && y == 4);
    }
    if (digit == 2) {
        return y == 0 || y == 2 || y == 4 || (x == 0 && y == 3) || (x == 2 && y == 1);
    }
    if (digit == 3) {
        return x == 0 || y == 0 || y == 2 || y == 4;
    }
    if (digit == 4) {
        return x == 0 || y == 2 || (x == 2 && y >= 2);
    }
    if (digit == 5) {
        return y == 0 || y == 2 || y == 4 || (x == 0 && y == 1) || (x == 2 && y == 3);
    }
    if (digit == 6) {
        return y == 0 || y == 2 || y == 4 || x == 2 || (x == 0 && y == 1);
    }
    if (digit == 7) {
        return x == 0 || y == 4 || (x == 2 && y == 3);
    }
    if (digit == 8) {
        return x == 0 || x == 2 || y == 0 || y == 2 || y == 4;
    }
    if (digit == 9) {
        return y == 0 || y == 2 || y == 4 || x == 0 || (x == 2 && y == 3);
    }

    return false;
}

bool drawDigitAt(vec2 p, int digit, float xOffset)
{
    p.x -= xOffset;
    return drawDigit(p, digit);
}

bool drawValue(vec2 p, int value)
{
    if (value < 10) {
        return drawDigitAt(p, value, 0.0);
    }

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
