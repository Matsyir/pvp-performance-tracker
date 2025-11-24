/*
 * Copyright (c) 2025, Matsyir <https://github.com/Matsyir>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package matsyir.pvpperformancetracker.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import lombok.Setter;
import net.runelite.client.ui.overlay.RenderableEntity;

// initially copied from TextComponent.class
// could have just extended TextComponent & only overridden renderText, but can't override renderText ._.
public class TextComponentShadowless implements RenderableEntity {
    private static final Pattern COL_TAG_PATTERN = Pattern.compile("<col=([0-9a-fA-F]{2,6})>");
    @Setter
    private String text;
    @Setter
    private Point position = new Point();
    @Setter
    private Color color;
    @Setter
    private boolean outline;
    @Setter
    private boolean shadow;
    @Nullable
    private Font font;

    public TextComponentShadowless() {
        this.color = Color.WHITE;
        this.outline = false;
        this.shadow = false;
    }

    public TextComponentShadowless(boolean outline, boolean shadow) {
        this.color = Color.WHITE;
        this.outline = outline;
        this.shadow = shadow;
    }

    public Dimension render(Graphics2D graphics) {
        Font originalFont = null;
        if (this.font != null) {
            originalFont = graphics.getFont();
            graphics.setFont(this.font);
        }

        FontMetrics fontMetrics = graphics.getFontMetrics();
        Matcher matcher = COL_TAG_PATTERN.matcher(this.text);
        Color textColor = this.color;
        int idx = 0;

        int width;
        String color;
        for(width = 0; matcher.find(); textColor = Color.decode("#" + color)) {
            color = matcher.group(1);
            String s = this.text.substring(idx, matcher.start());
            idx = matcher.end();
            this.renderText(graphics, textColor, this.position.x + width, this.position.y, s);
            width += fontMetrics.stringWidth(s);
        }

        color = this.text.substring(idx);
        this.renderText(graphics, textColor, this.position.x + width, this.position.y, color);
        width += fontMetrics.stringWidth(color);
        int height = fontMetrics.getHeight();
        if (originalFont != null) {
            graphics.setFont(originalFont);
        }

        return new Dimension(width, height);
    }

    private void renderText(Graphics2D graphics, Color color, int x, int y, String text) {
        if (!text.isEmpty()) {
            graphics.setColor(Color.BLACK);
            if (this.outline) {
                graphics.drawString(text, x, y + 1);
                graphics.drawString(text, x, y - 1);
                graphics.drawString(text, x + 1, y);
                graphics.drawString(text, x - 1, y);
            } else if (this.shadow) {
                graphics.drawString(text, x + 1, y + 1);
            }

            graphics.setColor(color);
            graphics.drawString(text, x, y);
        }
    }

    public void setFont(@Nullable Font font) {
        this.font = font;
    }

    public void updatePosition(int x, int y)
    {
        position.x = x;
        position.y = y;
    }
}
