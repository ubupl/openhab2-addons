package org.openhab.binding.supla.internal.cloud;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;

import static java.lang.String.format;

public final class HsbTypeConverter {
    public static final HsbTypeConverter INSTANCE = new HsbTypeConverter();
    private static final String HEX_PREFIX = "0x";

    public String convert(HSBType hsbType) {
        final String red = toHex(hsbType.getRed());
        final String green = toHex(hsbType.getGreen());
        final String blue = toHex(hsbType.getBlue());

        return format("%s%s%s%s", HEX_PREFIX, red, green, blue);
    }

    private String toHex(PercentType x) {
        final double scale = x.intValue() / 100.0;
        final int value = (int) (scale * 255);
        final boolean addZeroPrefix = value < 16;
        final String hex = Integer.toHexString(value).toUpperCase();
        return addZeroPrefix ? "0" + hex : hex;
    }

    public HSBType toHsbType(String hex, int saturation, int brightness) {
        if (!hex.startsWith(HEX_PREFIX)) {
            throw new IllegalArgumentException("Hex should start with `" + HEX_PREFIX + "`. Was " + hex);
        }
        final String rgb = hex.substring(2);
        final String red = rgb.substring(0, 2);
        final String green = rgb.substring(2, 4);
        final String blue = rgb.substring(4, 6);

        final HSBType hsbType = HSBType.fromRGB(
                Integer.parseInt(red, 16),
                Integer.parseInt(green, 16),
                Integer.parseInt(blue, 16));
        return new HSBType(
                hsbType.getHue(), 
                new PercentType(saturation), 
                new PercentType(brightness));
    }

    public HSBType toHsbType(String hex, int saturation) {
        return toHsbType(hex, saturation, 100);
    }
}
